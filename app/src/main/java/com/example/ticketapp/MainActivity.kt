package com.example.ticketapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), ProductsAdapter.ProductClickListener {

    private companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.ticketapp.USB_PERMISSION"
        private const val PRINTER_NAME_BLUETOOTH = "BlueTooth Printer"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // --- Vistas de la UI ---
    private lateinit var summaryContainer: View
    private lateinit var summaryTextView: TextView
    private lateinit var summaryTotalTextView: TextView
    private lateinit var btnCloseSummary: Button
    private lateinit var btnImprimir: Button
    private lateinit var btnEmparejar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnEditarPrecios: Button
    private lateinit var btnAdministrarMenu: Button
    private lateinit var editTextMesa: EditText
    private lateinit var imgQR: ImageView
    private lateinit var noCuenta: CheckBox
    private lateinit var categoryFilterSpinner: android.widget.Spinner // <--- AÑADE ESTO
    private lateinit var categoriesAdapter: ArrayAdapter<String> // Adaptador para el Spinner
    private val availableCategories = mutableListOf<String>() // Lista para las categorías
    private var currentSelectedCategoryFilter: String = "todas" // Lista para las categorías del Spinner

    // --- Arquitectura Dinámica: Base de datos y RecyclerView ---
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private val database by lazy { AppDatabase.getDatabase(this, lifecycleScope) }
    private val productDao by lazy { database.productDao() }

    // El mapa de cantidades ahora usa el ID (Int) del producto para mayor robustez
    private val quantities = mutableMapOf<Int, Int>()

    // Estado para el modo de edición de precios
    private var isEditMode = false

    // --- Variables de Conectividad ---
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private val bluetoothManager: BluetoothManager by lazy { getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private var bluetoothSocket: BluetoothSocket? = null

    // --- ActivityResultLaunchers para permisos ---
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (!granted) {
                Toast.makeText(this, "Se requieren permisos de Bluetooth para imprimir", Toast.LENGTH_LONG).show()
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(this, "No se pudo activar Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { setupUsbDevice(it) }
                    } else {
                        Toast.makeText(context, "Permiso USB denegado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(USB_SERVICE) as UsbManager

        // Configuración centralizada de la UI
        setupViews()
        setupRecyclerView()
        observeProducts()

        // Registro de receivers y permisos
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbReceiver, filter)
        }
        checkAndRequestBluetoothPermissions()
        detectAndRequestUsbPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        releaseUsbDevice()
        closeBluetoothSocket()
    }

    // --- Configuración de la Interfaz de Usuario ---
    private fun setupViews() {
        btnImprimir = findViewById(R.id.btnImprimir)
        btnEmparejar = findViewById(R.id.btnEmparejar)
        btnLimpiar = findViewById(R.id.btnLimpiar)
        imgQR = findViewById(R.id.imgQR)
        summaryContainer = findViewById(R.id.summaryContainer)
        summaryTextView = findViewById(R.id.summaryTextView)
        summaryTotalTextView = findViewById(R.id.summaryTotalTextView)
        btnCloseSummary = findViewById(R.id.btnCloseSummary)
        editTextMesa = findViewById(R.id.editMesa)
        btnEditarPrecios = findViewById(R.id.btnEditarPrecios)
        btnAdministrarMenu = findViewById(R.id.btnAdministrarMenu)
        noCuenta = findViewById(R.id.noCuenta)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)


        setupCategoryFilterSpinner()

        btnImprimir.setOnClickListener { imprimirTicket() }
        btnEmparejar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }
        btnLimpiar.setOnClickListener { limpiarCantidades() }
        btnCloseSummary.setOnClickListener { ocultarResumen() }
        btnEditarPrecios.setOnClickListener { toggleEditMode() }
        btnAdministrarMenu.setOnClickListener {
            val intent = Intent(this, MenuAdminActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCategoryFilterSpinner() {
        // Asegúrate de que "Todas" (o tu string para "todas las categorías") esté presente.
        // Sé consistente con el uso de mayúsculas/minúsculas. Usaré "Todas" aquí.
        if (!availableCategories.contains("Todas")) {
            availableCategories.add(0, "Todas") // Añade al principio si no está
        }

        // Inicializa o actualiza el ArrayAdapter
        if (!::categoriesAdapter.isInitialized) { // Si no está inicializado
            categoriesAdapter = ArrayAdapter(
                this,

                R.layout.spinner_dropdown_item, // Layout para el ítem seleccionado
                availableCategories
            )
            categoriesAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item) // Layout para el dropdown
            categoryFilterSpinner.adapter = categoriesAdapter
        } else {
            // Si ya está inicializado (ej. después de una actualización de categorías),
            // solo notifica al adaptador que los datos han cambiado.
            // Esto asume que `availableCategories` ya ha sido actualizada por `updateSpinnerCategories`.
            categoriesAdapter.notifyDataSetChanged()
        }

        // Establecer la selección actual en el Spinner
        // Intenta encontrar el índice de la categoría actualmente seleccionada
        val currentSelectionIndex = availableCategories.indexOf(currentSelectedCategoryFilter)

        if (currentSelectionIndex != -1) {
            // Si se encuentra la categoría, selecciónala en el Spinner.
            // El 'false' evita que onItemSelected se dispare en este momento de configuración inicial.
            categoryFilterSpinner.setSelection(currentSelectionIndex, false)
        } else {
            // Si la categoría actual no se encuentra (ej. es la primera vez o la categoría fue eliminada),
            // selecciona la primera opción ("Todas") por defecto.
            if (availableCategories.isNotEmpty()) {
                categoryFilterSpinner.setSelection(0, false)
                currentSelectedCategoryFilter = availableCategories[0] // Actualiza la variable de filtro
            } else {
                // Caso raro: no hay categorías. Podrías querer manejar esto,
                // por ejemplo, deshabilitando el spinner o mostrando un mensaje.
                Log.w(TAG, "setupCategoryFilterSpinner: No hay categorías disponibles para el Spinner.")
            }
        }



        // Listener para cuando un ítem es seleccionado en el Spinner
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()

                // Solo actuar si la selección realmente ha cambiado para evitar recargas innecesarias
                if (currentSelectedCategoryFilter != selectedCategory) {
                    currentSelectedCategoryFilter = selectedCategory
                    Log.d(TAG, "Filtro de categoría cambiado a: $currentSelectedCategoryFilter")
                    observeProducts() // Llama a observeProducts para recargar y filtrar la lista
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (currentSelectedCategoryFilter != "Todas" && availableCategories.contains("Todas")) {
                    val todasPosition = availableCategories.indexOf("Todas")
                    if (todasPosition != -1) { // Asegurar que "Todas" exista
                        currentSelectedCategoryFilter = "Todas"
                        categoryFilterSpinner.setSelection(todasPosition, false) // false para no disparar de nuevo
                        Log.d(TAG, "Spinner - Nada seleccionado, volviendo a: $currentSelectedCategoryFilter")
                        observeProducts() // Recargar productos con el filtro "Todas"
                    }
                }
            }
        }
    }

    private fun onNothingSelected(parent: AdapterView<*>?) {

        // Solo actuar si el filtro actual no es ya "Todas" para evitar trabajo innecesario.
        if (currentSelectedCategoryFilter != "Todas") {
            // Verificar si "Todas" existe en la lista de categorías disponibles.
            // Esto es importante si la lista de categorías pudiera estar vacía o no contener "Todas".
            if (availableCategories.contains("Todas")) {
                val todasPosition = availableCategories.indexOf("Todas")
                // Aunque `contains` ya lo verificó, `indexOf` podría devolver -1 si hay inconsistencias (raro).
                // Es una buena práctica verificar de nuevo.
                if (todasPosition != -1) {
                    currentSelectedCategoryFilter = "Todas"
                    // Establece la selección en el Spinner.
                    // El 'false' como segundo parámetro evita que onItemSelected se dispare
                    // recursivamente si esta función fue llamada desde el listener.
                    // Si esta es una implementación directa de la interfaz en la Activity,
                    // y no dentro de un `object : ...`, entonces `parent` se refiere al Spinner.
                    // Si `categoryFilterSpinner` es el único Spinner que usa este listener, puedes usarlo directamente.
                    categoryFilterSpinner.setSelection(todasPosition, false)
                    Log.d(TAG, "onNothingSelected: No había selección, volviendo al filtro: $currentSelectedCategoryFilter")
                    // Actualiza la lista de productos para reflejar el cambio de filtro.
                    observeProducts()
                } else {
                    // Caso muy improbable si availableCategories.contains("Todas") fue true.
                    Log.w(TAG, "onNothingSelected: 'Todas' estaba en availableCategories pero indexOf devolvió -1.")
                    // Como fallback, si "Todas" no se encuentra por alguna razón pero hay otras categorías,
                    // podríamos seleccionar la primera disponible.
                    if (availableCategories.isNotEmpty()) {
                        currentSelectedCategoryFilter = availableCategories[0]
                        categoryFilterSpinner.setSelection(0, false)
                        Log.d(TAG, "onNothingSelected: 'Todas' no encontrada, volviendo a: $currentSelectedCategoryFilter")
                        observeProducts()
                    } else {
                        // Si no hay categorías, no hay nada que seleccionar o filtrar.
                        Log.w(TAG, "onNothingSelected: No hay categorías disponibles en el Spinner.")
                        // Podrías querer limpiar la lista de productos si este es el caso.
                        productsAdapter.submitList(emptyList())
                    }
                }
            } else if (availableCategories.isNotEmpty()) {
                // Si "Todas" no está, pero hay otras categorías, selecciona la primera.
                currentSelectedCategoryFilter = availableCategories[0]
                categoryFilterSpinner.setSelection(0, false)
                Log.d(TAG, "onNothingSelected: 'Todas' no disponible, volviendo a la primera categoría: $currentSelectedCategoryFilter")
                observeProducts()
            } else {
                // Si no hay ninguna categoría en el spinner.
                Log.w(TAG, "onNothingSelected: El Spinner no tiene categorías.")
                // Aquí podrías querer limpiar la UI o mostrar un mensaje.
                // Si currentSelectedCategoryFilter no se actualiza, y observeProducts() se llama,
                // usará el último filtro válido. Si queremos limpiar:
                currentSelectedCategoryFilter = "" // O un valor que indique "sin filtro / vacío"
                productsAdapter.submitList(emptyList()) // Limpia el RecyclerView
            }
        } else {
            // Si el filtro ya era "Todas", no es necesario hacer nada.
            // O, si el spinner está vacío y `currentSelectedCategoryFilter` es "Todas" por defecto,
            // pero `availableCategories` no contiene "Todas", podría entrar aquí.
            // Si el objetivo es solo re-filtrar si el spinner se vacía y luego se repuebla:
            // observeProducts() // Descomentar si se quiere forzar un re-filtrado.
        }
    }

    private fun setupRecyclerView() {
        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsAdapter = ProductsAdapter(this)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            Log.d(TAG, "observeProducts: Iniciando. Filtro actual: $currentSelectedCategoryFilter")
            try {
                productDao.getAllProducts().collectLatest { productList ->
                    Log.d(TAG, "observeProducts: Recibida lista completa del DAO. Tamaño: ${productList.size}")

                    // Paso 1: Actualizar las categorías del spinner con la lista completa de productos
                    updateSpinnerCategories(productList)

                    // Paso 2: Manejar lista vacía del DAO
                    if (productList.isEmpty()) {
                        Log.w(TAG, "observeProducts: La lista de productos del DAO está VACÍA.")
                        productsAdapter.submitList(emptyList())
                        return@collectLatest
                    }

                    // Paso 3: Filtrar la lista de productos según el filtro del spinner
                    val itemsToProcess: List<Product>
                    if (currentSelectedCategoryFilter == "Todas" || currentSelectedCategoryFilter.isBlank()) {
                        itemsToProcess = productList // Mostrar todos si el filtro es "Todas" o vacío
                        Log.d(TAG, "observeProducts: Filtro es 'Todas' o vacío. Procesando todos los ${productList.size} productos.")
                    } else {
                        itemsToProcess = productList.filter {
                            (it.category.takeIf { cat -> cat.isNotBlank() } ?: "Sin Categoría") == currentSelectedCategoryFilter
                        }
                        Log.d(TAG, "observeProducts: Aplicando filtro para categoría '$currentSelectedCategoryFilter'. Productos después del filtro: ${itemsToProcess.size}")
                    }

                    // Paso 4: Si después de filtrar no hay productos, actualizar el adaptador y salir
                    if (itemsToProcess.isEmpty()) {
                        Log.d(TAG, "observeProducts: No hay productos para mostrar después de aplicar el filtro '$currentSelectedCategoryFilter'.")
                        productsAdapter.submitList(emptyList())
                        return@collectLatest
                    }

                    // Paso 5: Agrupar los productos filtrados (itemsToProcess) por su categoría real
                    val finalGroupedItems = mutableListOf<GroupedItem>()
                    val groupedByCategory = itemsToProcess
                        .groupBy { it.category.takeIf { cat -> cat.isNotBlank() } ?: "Sin Categoría" }
                        .toSortedMap() // Ordenar las categorías alfabéticamente

                    Log.d(TAG, "observeProducts: Productos (después de filtro) agrupados por categoría. Número de grupos: ${groupedByCategory.size}")

                    for ((category, productsInCategory) in groupedByCategory) {
                        // Solo añadir el header si el filtro es "Todas" o si la categoría actual coincide con el filtro.
                        // Si el filtro es específico, solo habrá una categoría en groupedByCategory.
                        finalGroupedItems.add(GroupedItem.Header(category))
                        Log.d(TAG, "observeProducts: Agregando Header para categoría: '$category'")

                        productsInCategory.sortedBy { it.name }.forEach { product -> // Ordenar productos dentro de cada categoría
                            val quantity = quantities.getOrDefault(product.id, 0)
                            finalGroupedItems.add(GroupedItem.ProductItem(product, quantity))
                            Log.d(TAG, "observeProducts: Agregando Producto: '${product.name}', Cantidad: $quantity, Categoría (original): '${product.category}' a grupo '$category'")
                        }
                    }

                    // Paso 6: Enviar la lista final agrupada y filtrada al adaptador
                    productsAdapter.submitList(finalGroupedItems)
                    Log.d(TAG, "observeProducts: Lista final de GroupedItems enviada al adaptador (Filtro: '$currentSelectedCategoryFilter'). Tamaño: ${finalGroupedItems.size}")

                    if (finalGroupedItems.isEmpty() && productList.isNotEmpty() && (currentSelectedCategoryFilter == "Todas" || currentSelectedCategoryFilter.isBlank())) {
                        // Esta condición solo debería ser preocupante si se esperaban todos los productos
                        // y aún así la lista agrupada está vacía.
                        Log.e(TAG, "observeProducts: ALERTA - productList original no estaba vacía y el filtro era 'Todas', pero finalGroupedItems SÍ lo está. Revisa la lógica de agrupación.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "observeProducts: Error catastrófico durante la observación o agrupación.", e)
                productsAdapter.submitList(emptyList()) // Limpiar UI en caso de error grave
                // Considerar resetear el spinner a un estado seguro también
                availableCategories.clear()
                availableCategories.add("Todas")
                categoriesAdapter.notifyDataSetChanged()
                currentSelectedCategoryFilter = "Todas"
                if (availableCategories.isNotEmpty()) categoryFilterSpinner.setSelection(0, false)
            }
        }
    }


    private fun updateSpinnerCategories(productList: List<Product>) {
        val newCategories = mutableListOf("Todas") // Siempre tener "Todas"
        val distinctProductCategories = productList
            .mapNotNull { it.category.takeIf { cat -> cat.isNotBlank() } } // Ignorar categorías vacías/nulas
            .distinct() // Obtener categorías únicas
            .sorted()   // Ordenarlas alfabéticamente
        newCategories.addAll(distinctProductCategories)

        // Solo actualizar si la lista de categorías ha cambiado realmente
        if (availableCategories != newCategories) {
            val previousSelection = currentSelectedCategoryFilter // Guardar la selección actual
            availableCategories.clear()
            availableCategories.addAll(newCategories)
            categoriesAdapter.notifyDataSetChanged() // Notificar al adaptador del cambio

            // Intentar restaurar la selección previa si aún existe, sino, seleccionar "Todas"
            val newPosition = availableCategories.indexOf(previousSelection)
            if (newPosition != -1) {
                categoryFilterSpinner.setSelection(newPosition, false) // false para no disparar onItemSelected
            } else {
                currentSelectedCategoryFilter = "Todas" // Resetear filtro si la categoría anterior desapareció
                val todasPosition = availableCategories.indexOf("Todas")
                if (todasPosition != -1) categoryFilterSpinner.setSelection(todasPosition, false)
                else if (availableCategories.isNotEmpty()) categoryFilterSpinner.setSelection(0, false) // Fallback al primer item
            }
            Log.d(TAG, "Categorías del Spinner actualizadas: $availableCategories. Selección actual: $currentSelectedCategoryFilter")
        }
    }

    // --- Lógica de la Aplicación ---
    override fun onQuantityChanged(product: Product, change: Int) {
        val currentQuantity = quantities.getOrDefault(product.id, 0)
        val newQuantity = (currentQuantity + change).coerceAtLeast(0)
        quantities[product.id] = newQuantity
        observeProducts() // Refresca la lista para mostrar la nueva cantidad
    }

    override fun onEditPriceClicked(product: Product) {
        if (isEditMode) {
            showEditPriceDialog(product)
        } else {
            Toast.makeText(this, "Activa 'Editar Precios' para cambiar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        if (isEditMode) {
            btnEditarPrecios.text = "Guardar Cambios"
            Toast.makeText(this, "MODO EDICIÓN: Haz un clic largo para cambiar el precio.", Toast.LENGTH_LONG).show()
        } else {
            btnEditarPrecios.text = "Editar Precios"
            Toast.makeText(this, "Cambios guardados.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditPriceDialog(product: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nuevo Precio para: ${product.name}")

        val container = FrameLayout(this).apply {
            setPadding(50, 20, 50, 20)
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(product.price.toString())
            selectAll()
        }
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newPrice = input.text.toString().toDoubleOrNull()
            if (newPrice != null && newPrice >= 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    productDao.update(product.copy(price = newPrice))
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Por favor, introduce un precio válido.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun limpiarCantidades() {
        quantities.clear()
        editTextMesa.text.clear()
        currentSelectedCategoryFilter = "Todas"

        val todasPosition = availableCategories.indexOf("Todas")
        if (todasPosition != -1) {
            categoryFilterSpinner.setSelection(todasPosition) // Esto disparará onItemSelected y observeProducts
        } else if (availableCategories.isNotEmpty()) {
            categoryFilterSpinner.setSelection(0)
        } else {
            observeProducts() // Si no hay "Todas" y el spinner está vacío, solo re-observar.
        }
        ocultarResumen()
        Toast.makeText(this, "Cantidades restablecidas a 0", Toast.LENGTH_SHORT).show()
    }


    private fun obtenerProductosDesdeInputs(): List<Producto> { // Asumo que Producto aquí es tu data class para el ticket
        val listaDeProductos = mutableListOf<Producto>()
        productsAdapter.currentList.forEach { groupedItem ->
            if (groupedItem is GroupedItem.ProductItem) {
                val productEntity = groupedItem.product // Este es tu @Entity Product
                val quantity = quantities.getOrDefault(productEntity.id, 0)

                if (quantity > 0) {
                    listaDeProductos.add(
                        Producto( // Esta es la data class para el ticket
                            nombre = productEntity.name,
                            precio = productEntity.price,
                            cantidad = quantity
                        )
                    )
                }
            }
        }
        return listaDeProductos
    }


    private fun generarTextoTicket(): String {
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        val anchoTotalLinea = 32
        sb.appendLine("   ANTOJITOS MEXICANOS MARGARITA")
        sb.appendLine("********************************")
        sb.appendLine("     *** TICKET DE COMPRA ***")
        sb.appendLine("********************************")
        sb.appendLine("Fecha y hora: $fechaHora")

        val mesaInfo = editTextMesa.text.toString().trim()
        if (mesaInfo.isNotEmpty()) {
            sb.appendLine(String.format("%-32s", "Mesa: ${mesaInfo.uppercase()}"))
            sb.appendLine("********************************")
        }
        if (noCuenta.isChecked) {
            sb.appendLine(String.format("%-32s", "4027 6657 8599 1515"))
            sb.appendLine(String.format("%-32s", "Nombre: Omar Aldair"))
            sb.appendLine(String.format("%-32s", "Banco: Azteca"))
            sb.appendLine("********************************")
        }
        val lineaSeparadoraCorta = "-".repeat(anchoTotalLinea)
        sb.appendLine(lineaSeparadoraCorta)
        sb.appendLine(String.format("%-15s %6s %3s %7s", "Producto", "Precio", "Cant", "Total"))
        sb.appendLine(lineaSeparadoraCorta)
        var totalGeneral = 0.0
        val productos = obtenerProductosDesdeInputs()
        if (productos.isEmpty()) {
            sb.appendLine("      NINGUN PRODUCTO")
            sb.appendLine("       SELECCIONADO")
        } else {
            for (p in productos) {
                totalGeneral += p.total
                val nombreProducto = if (p.nombre.length > 15) p.nombre.substring(0, 12) + "..." else p.nombre
                val precioConSimbolo = String.format("$%.2f", p.precio)
                val totalConSimbolo = String.format("$%.2f", p.total)
                sb.appendLine(String.format("%-15s %6s %3d %7s", nombreProducto, precioConSimbolo, p.cantidad, totalConSimbolo))
            }
        }
        sb.appendLine(lineaSeparadoraCorta)
        val totalGeneralConSimbolo = String.format("$%.2f", totalGeneral)
        sb.appendLine(String.format("%-22s%10s", "TOTAL:", totalGeneralConSimbolo))
        sb.appendLine(lineaSeparadoraCorta)
        sb.appendLine("")
        sb.appendLine("    Gracias por su compra")
        sb.appendLine("    Vuelva pronto")
        sb.appendLine("\n\n\n")
        sb.append(byteArrayOf(0x1D, 0x56, 0x42, 0x01).toString(StandardCharsets.ISO_8859_1))
        return sb.toString()
    }

    private fun imprimirTicket() {
        val productosSeleccionados = obtenerProductosDesdeInputs()
        if (productosSeleccionados.isEmpty()) {
            Toast.makeText(this, "No hay productos seleccionados", Toast.LENGTH_SHORT).show()
            return
        }
        mostrarResumen(productosSeleccionados)
        val textoTicket = generarTextoTicket()
        lifecycleScope.launch {
            val usbSuccess = printViaUsb(textoTicket)
            if (!usbSuccess) {
                if (checkBluetoothPermissions()) {
                    val btSuccess = printViaBluetooth(textoTicket)
                    if (btSuccess) {
                        Toast.makeText(this@MainActivity, "Ticket enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Fallo al imprimir", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Permisos de Bluetooth no concedidos.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Ticket enviado por USB", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarResumen(productos: List<Producto>) {
        val sb = StringBuilder()
        var totalGeneral = 0.0
        productos.forEach { producto ->
            val totalProducto = producto.cantidad * producto.precio
            totalGeneral += totalProducto
            sb.appendLine("${producto.cantidad} x ${producto.nombre} ... $${"%.2f".format(totalProducto)}")
        }
        summaryTextView.text = sb.toString()
        summaryTotalTextView.text = "TOTAL: $${"%.2f".format(totalGeneral)}"
        imgQR.visibility = View.GONE
        summaryContainer.visibility = View.VISIBLE
    }

    private fun ocultarResumen() {
        if (::summaryContainer.isInitialized) {
            summaryContainer.visibility = View.GONE
        }
    }

    // --- Data class para el ticket ---
    data class Producto(val nombre: String, val precio: Double, val cantidad: Int) {
        val total: Double get() = precio * cantidad
    }

    // --- Toda la lógica de bajo nivel (Permisos, Bluetooth, USB) se queda igual ---
    private fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            requestBluetoothPermissionLauncher.launch(permissions)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                val pairedDevices = bluetoothAdapter?.bondedDevices
                val printer = pairedDevices?.find { it.name.contains(PRINTER_NAME_BLUETOOTH, ignoreCase = true) }

                if (printer != null) {
                    val socket = printer.createRfcommSocketToServiceRecord(PRINTER_UUID)
                    socket.connect()
                    socket
                } else {
                    Log.e(TAG, "No se encontró impresora Bluetooth emparejada")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando a impresora Bluetooth", e)
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(textoTicket: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                closeBluetoothSocket()
                bluetoothSocket = connectToBluetoothPrinter()

                bluetoothSocket?.let { socket ->
                    val outputStream = socket.outputStream
                    outputStream.write(textoTicket.toByteArray(StandardCharsets.ISO_8859_1))
                    outputStream.flush()
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error imprimiendo por Bluetooth", e)
                false
            }
        }
    }

    private fun closeBluetoothSocket() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando socket Bluetooth", e)
        }
    }

    private fun detectAndRequestUsbPermission() {
        val deviceList = usbManager.deviceList
        if (deviceList.isNotEmpty()) {
            val device = deviceList.values.first()
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun setupUsbDevice(device: UsbDevice) {
        try {
            usbDevice = device
            usbDeviceConnection = usbManager.openDevice(device)

            if (device.interfaceCount > 0) {
                usbInterface = device.getInterface(0)
                usbDeviceConnection?.claimInterface(usbInterface, true)

                for (i in 0 until usbInterface!!.endpointCount) {
                    val endpoint = usbInterface!!.getEndpoint(i)
                    if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                        usbEndpointOut = endpoint
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando dispositivo USB", e)
        }
    }

    private suspend fun printViaUsb(data: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                usbDeviceConnection?.let { connection ->
                    usbEndpointOut?.let { endpoint ->
                        val bytes = data.toByteArray(StandardCharsets.ISO_8859_1)
                        val result = connection.bulkTransfer(endpoint, bytes, bytes.size, 5000)
                        result > 0
                    } ?: false
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error imprimiendo por USB", e)
                false
            }
        }
    }

    private fun releaseUsbDevice() {
        try {
            usbDeviceConnection?.releaseInterface(usbInterface)
            usbDeviceConnection?.close()
            usbDeviceConnection = null
            usbInterface = null
            usbEndpointOut = null
            usbDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando dispositivo USB", e)
        }
    }

    private fun generarQR(texto: String): Bitmap {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generando código QR", e)
            return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        }
    }

    private fun bitmapToEscPosData(bitmap: Bitmap, printerDotsPerLine: Int): ByteArray {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, printerDotsPerLine, bitmap.height * printerDotsPerLine / bitmap.width, false)
        val width = scaledBitmap.width
        val height = scaledBitmap.height
        val data = mutableListOf<Byte>()

        // ESC/POS bitmap command
        data.addAll(byteArrayOf(0x1B, 0x2A, 0x00, (width / 8).toByte(), (height and 0xFF).toByte()).toList())

        for (y in 0 until height) {
            for (x in 0 until width step 8) {
                var byte = 0
                for (bit in 0 until 8) {
                    if (x + bit < width) {
                        val pixel = scaledBitmap.getPixel(x + bit, y)
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                        }
                    }
                }
                data.add(byte.toByte())
            }
        }

        return data.toByteArray()
    }

}