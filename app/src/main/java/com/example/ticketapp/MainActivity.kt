package com.example.ticketapp
import java.util.Locale
import android.widget.EditText
import android.app.DatePickerDialog

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import android.widget.CheckBox
import kotlinx.coroutines.CoroutineScope
import java.util.Calendar
import java.util.Date
import androidx.core.view.GravityCompat



class MainActivity : AppCompatActivity() {

    // --- Constantes ---
    private companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.ticketapp.USB_PERMISSION"

        // Nombres comunes de impresoras POS-58 / 5890A-L vía Bluetooth.
        private val PRINTER_BT_NAMES = listOf("POS-58", "5890", "BlueTooth Printer")

        // SPP UUID clásico (Serial Port Profile)
        private val PRINTER_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Vendor/Product IDs comunes de la POS-5890 y clones.
        // 1155 / 22339 corresponde a controladores tipo USB-Serial genéricos (0x0483/0x5743).
        // 1659 / 8963 es otro ID de muchas térmicas chinas.
        private val PRINTER_USB_IDS = setOf(
            Pair(1155, 22339),
            Pair(1659, 8963)
        )

        private const val EXTRA_COMBO = 30.0
    }

    private lateinit var txtTotal: TextView
    private lateinit var txtNormales: Map<String, TextView>
    private lateinit var txtCombos: Map<String, TextView>

    private val preciosHamburguesas = mapOf(
        "Hamburguesa Clasica" to 65.0,
        "Hamburguesa Hawaiana" to 80.0,
        "Hamburguesa Pollo" to 70.0,
        "Hamburguesa Champinones" to 90.0,
        "Hamburguesa Arrachera" to 105.0,
        "Hamburguesa Maggy" to 100.0,
        "Hamburguesa Doble" to 110.0
    )

    private val extraCombo = 30.0
    private val cantidadesNormales = mutableMapOf<String, Int>()
    private val cantidadesCombo = mutableMapOf<String, Int>()

    // 🔹 MAPA DE VARIANTES DE PRODUCTOS
    private val productVariations = mapOf(
        "Quesadillas" to listOf("Chorizo","Huevo","Huitlacoche", "Mole Verde", "Bisteck", "Pollo", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Quesadilla/Queso" to listOf("Chorizo", "Huevo","Mole Verde","Huitlacoche","Bisteck", "Pollo", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Volcanes" to listOf("Chorizo", "Mole Verde","Huevo", "Bisteck", "Pollo","Huitlacoche", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Volcan Queso" to listOf("Chorizo", "Mole Verde","Huevo", "Bisteck", "Pollo","Huitlacoche", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Guisado Extra" to listOf("Chorizo", "Mole Verde","Huevo", "Bisteck", "Pollo","Huitlacoche", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Tostadas" to listOf("Chorizo", "Mole Verde","pata", "Bisteck","Huevo", "Pollo", "Champiñones","Huitlacoche", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pozole Grande" to listOf("pollo", "puerco", "combinado"),
        "Pozole Chico" to listOf("pollo", "puerco", "combinado"),
        "Guajoloyets Naturales Extra" to listOf("Chorizo", "Mole Verde", "Huevo","Bisteck","Huitlacoche", "Pollo", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Guajoloyets Adobados Extra" to listOf("Chorizo", "Mole Verde","Huevo", "Bisteck","Huitlacoche", "Pollo", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Naturales" to listOf("Chorizo", "Mole Verde", "Bisteck","Huevo", "Pollo", "Champiñones","Huitlacoche", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Naturales Combinados" to listOf("Chorizo", "Mole Verde","Huevo", "Bisteck", "Pollo", "Champiñones","Huitlacoche", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Naturales Extra" to listOf("Chorizo", "Mole Verde", "Bisteck","Huevo", "Pollo", "Champiñones", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Naturales Combinados con Queso" to listOf("Chorizo", "Mole Verde", "Huevo","Bisteck", "Pollo", "Champiñones","Huitlacoche", "Tinga", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Adobados" to listOf("Chorizo", "Mole Verde", "Bisteck", "Pollo", "Huevo","Champiñones", "Tinga", "Picadillo","Huitlacoche", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Adobados Combinados" to listOf("Chorizo", "Mole Verde", "Bisteck","Huevo", "Pollo", "Champiñones", "Tinga","Huitlacoche", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Adobados Combinados con Queso" to listOf("Chorizo", "Mole Verde", "Huevo","Bisteck", "Pollo", "Champiñones", "Tinga","Huitlacoche", "Picadillo", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Pambazos Adobados Extra" to listOf("Chorizo", "Mole Verde", "Bisteck", "Pollo", "Huevo","Champiñones", "Tinga", "Picadillo","Huitlacoche", "Papa con chorizo", "Chicharrón Prensado", "Queso"),
        "Taco (c/u)" to listOf("Costilla", "Arrachera", "Cecina", "Chorizo Argentino"),
        "Taco con Queso (c/u)" to listOf("Costilla", "Arrachera", "Cecina", "Chorizo Argentino"),
        "Alitas 6 pzas" to listOf("BBQ", "BBQ Hot", "Búfalo","Mango-Habanero", "Macha"),
        "Alitas 10 pzas" to listOf("BBQ", "BBQ Hot", "Búfalo", "Mango-Habanero","Macha"),
        "Alitas 15 pzas" to listOf("BBQ", "BBQ Hot", "Búfalo","Mango-Habanero", "Macha")
    )


    // 🔹 PRODUCTOS QUE USAN INPUT DE TEXTO PARA VARIANTES
    private val textInputProducts = setOf("Refrescos", "Aguas de Sabor")

    // 🔹 LISTA PARA ALMACENAR PRODUCTOS CON VARIANTES SELECCIONADAS
    private val selectedVariations = mutableListOf<Producto>()



    // Base de datos de la aplicación
    // Declaración única del adaptador del panel de administración
    // (evita duplicados y confusiones con nombres)
    private lateinit var adminOrderAdapter: AdminOrderAdapter

    private lateinit var summaryContainer: View
    private lateinit var summaryTextView: TextView
    private lateinit var summaryTotalTextView: TextView
    private lateinit var btnCloseSummary: Button
    private lateinit var btnImprimir: Button
    private lateinit var btnEmparejar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var editTextMesa: EditText
    private lateinit var noCuenta: CheckBox

    private val products = mutableMapOf<String, ProductData>()
    private val quantities = mutableMapOf<String, Int>()

    private lateinit var appDatabase: AppDatabase
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var adminSummaryTextView: TextView
    private lateinit var recyclerViewOrders: RecyclerView



    // El adaptador se declara una sola vez arriba

    private var isEditMode = false

    // Flag para evitar múltiples impresiones por toques rápidos
    private var isPrinting = false

    // --- Filtro de ventas por producto ---
    private lateinit var spinnerProductFilter: Spinner
    private lateinit var spinnerPeriodFilter: Spinner
    private lateinit var spinnerTypeFilter: Spinner
    private lateinit var editStartDate: EditText
    private lateinit var editEndDate: EditText
    private lateinit var btnCalcularVentas: MaterialButton
    private lateinit var layoutCustomRange: View
    private lateinit var tvProductSalesResult: TextView




    // --- Hardware & Permissions ---
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private var bluetoothSocket: BluetoothSocket? = null

    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                Toast.makeText(this, "Permisos Bluetooth concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Se requieren permisos de Bluetooth para imprimir",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo activar Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            Log.d(TAG, "Permiso USB concedido para: ${it.deviceName}")
                            setupUsbDevice(it)
                        }
                    } else {
                        Log.d(
                            TAG,
                            "Permiso USB denegado para: ${device?.deviceName}"
                        )
                        Toast.makeText(
                            context,
                            "Permiso USB denegado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    // animación de flechitas para colapsar categorías
    fun View.rotateArrow(expanded: Boolean, duration: Long = 200L) {
        animate()
            .rotation(if (expanded) 180f else 0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    var isExpanded = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // Inicializa la base de datos una vez al inicio. La observación se configurará
        // después de que el adaptador de órdenes haya sido configurado para evitar
        // condiciones de carrera.
        appDatabase = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        txtTotal = findViewById(R.id.textViewTotal)


        txtNormales = mapOf(
            "Hamburguesa Clasica" to findViewById(R.id.cantidadHamburguesaClasicaNormal),
            "Hamburguesa Hawaiana" to findViewById(R.id.cantidadHamburguesaHawaianaNormal),
            "Hamburguesa Pollo" to findViewById(R.id.cantidadHamburguesaPolloNormal),
            "Hamburguesa Champinones" to findViewById(R.id.cantidadHamburguesaChampinonesNormal),
            "Hamburguesa Arrachera" to findViewById(R.id.cantidadHamburguesaArracheraNormal),
            "Hamburguesa Maggy" to findViewById(R.id.cantidadHamburguesaMaggyNormal),
            "Hamburguesa Doble" to findViewById(R.id.cantidadHamburguesaDobleNormal)
        )

        txtCombos = mapOf(
            "Hamburguesa Clasica" to findViewById(R.id.cantidadHamburguesaClasicaCombo),
            "Hamburguesa Hawaiana" to findViewById(R.id.cantidadHamburguesaHawaianaCombo),
            "Hamburguesa Pollo" to findViewById(R.id.cantidadHamburguesaPolloCombo),
            "Hamburguesa Champinones" to findViewById(R.id.cantidadHamburguesaChampinonesCombo),
            "Hamburguesa Arrachera" to findViewById(R.id.cantidadHamburguesaArracheraCombo),
            "Hamburguesa Maggy" to findViewById(R.id.cantidadHamburguesaMaggyCombo),
            "Hamburguesa Doble" to findViewById(R.id.cantidadHamburguesaDobleCombo)
        )

// Inicializa cantidades
        for (nombre in preciosHamburguesas.keys) {
            cantidadesNormales[nombre] = 0
            cantidadesCombo[nombre] = 0
        }


        fun recalcularTotal() {
            var total = 0.0
            for ((nombre, precioBase) in preciosHamburguesas) {
                val normales = cantidadesNormales[nombre] ?: 0
                val combos = cantidadesCombo[nombre] ?: 0
                total += (precioBase * normales) + ((precioBase + extraCombo) * combos)
            }
            txtTotal.text = "Total: $%.2f".format(total)
        }

        val botonesNormal = mapOf(
            "Hamburguesa Clasica" to findViewById<Button>(R.id.btnMasHamburguesaClasicaNormal),
            "Hamburguesa Hawaiana" to findViewById<Button>(R.id.btnMasHamburguesaHawaianaNormal),
            "Hamburguesa Pollo" to findViewById<Button>(R.id.btnMasHamburguesaPolloNormal),
            "Hamburguesa Champinones" to findViewById<Button>(R.id.btnMasHamburguesaChampinonesNormal),
            "Hamburguesa Arrachera" to findViewById<Button>(R.id.btnMasHamburguesaArracheraNormal),
            "Hamburguesa Maggy" to findViewById<Button>(R.id.btnMasHamburguesaMaggyNormal),
            "Hamburguesa Doble" to findViewById<Button>(R.id.btnMasHamburguesaDobleNormal)
        )

        val botonesCombo = mapOf(
            "Hamburguesa Clasica" to findViewById<Button>(R.id.btnMasHamburguesaClasicaCombo),
            "Hamburguesa Hawaiana" to findViewById<Button>(R.id.btnMasHamburguesaHawaianaCombo),
            "Hamburguesa Pollo" to findViewById<Button>(R.id.btnMasHamburguesaPolloCombo),
            "Hamburguesa Champinones" to findViewById<Button>(R.id.btnMasHamburguesaChampinonesCombo),
            "Hamburguesa Arrachera" to findViewById<Button>(R.id.btnMasHamburguesaArracheraCombo),
            "Hamburguesa Maggy" to findViewById<Button>(R.id.btnMasHamburguesaMaggyCombo),
            "Hamburguesa Doble" to findViewById<Button>(R.id.btnMasHamburguesaDobleCombo)
        )

        for ((nombre, boton) in botonesNormal) {
            boton.setOnClickListener {
                val actual = (cantidadesNormales[nombre] ?: 0) + 1
                cantidadesNormales[nombre] = actual
                txtNormales[nombre]?.text = "$actual"
                // Recalcula el total (solo hamburguesas)
                recalcularTotal()
                // Actualiza el resumen en tiempo real
                val productosSeleccionados = obtenerProductosDesdeInputs()
                mostrarResumen(productosSeleccionados)
            }
        }

        for ((nombre, boton) in botonesCombo) {
            boton.setOnClickListener {
                val actual = (cantidadesCombo[nombre] ?: 0) + 1
                cantidadesCombo[nombre] = actual
                txtCombos[nombre]?.text = "$actual"
                // Recalcula el total (solo hamburguesas)
                recalcularTotal()
                // Actualiza el resumen en tiempo real
                val productosSeleccionados = obtenerProductosDesdeInputs()
                mostrarResumen(productosSeleccionados)
            }
        }

        // Botones de RESTAR
        val botonesMenosNormal = mapOf(
            "Hamburguesa Clasica" to findViewById<Button>(R.id.btnMenosHamburguesaClasicaNormal),
            "Hamburguesa Hawaiana" to findViewById<Button>(R.id.btnMenosHamburguesaHawaianaNormal),
            "Hamburguesa Pollo" to findViewById<Button>(R.id.btnMenosHamburguesaPolloNormal),
            "Hamburguesa Champinones" to findViewById<Button>(R.id.btnMenosHamburguesaChampinonesNormal),
            "Hamburguesa Arrachera" to findViewById<Button>(R.id.btnMenosHamburguesaArracheraNormal),
            "Hamburguesa Maggy" to findViewById<Button>(R.id.btnMenosHamburguesaMaggyNormal),
            "Hamburguesa Doble" to findViewById<Button>(R.id.btnMenosHamburguesaDobleNormal)
        )

        val botonesMenosCombo = mapOf(
            "Hamburguesa Clasica" to findViewById<Button>(R.id.btnMenosHamburguesaClasicaCombo),
            "Hamburguesa Hawaiana" to findViewById<Button>(R.id.btnMenosHamburguesaHawaianaCombo),
            "Hamburguesa Pollo" to findViewById<Button>(R.id.btnMenosHamburguesaPolloCombo),
            "Hamburguesa Champinones" to findViewById<Button>(R.id.btnMenosHamburguesaChampinonesCombo),
            "Hamburguesa Arrachera" to findViewById<Button>(R.id.btnMenosHamburguesaArracheraCombo),
            "Hamburguesa Maggy" to findViewById<Button>(R.id.btnMenosHamburguesaMaggyCombo),
            "Hamburguesa Doble" to findViewById<Button>(R.id.btnMenosHamburguesaDobleCombo)
        )

        for ((nombre, boton) in botonesMenosNormal) {
            boton.setOnClickListener {
                val actual = maxOf((cantidadesNormales[nombre] ?: 0) - 1, 0)
                cantidadesNormales[nombre] = actual
                txtNormales[nombre]?.text = "$actual"
                recalcularTotal()
                val productosSeleccionados = obtenerProductosDesdeInputs()
                mostrarResumen(productosSeleccionados)
            }
        }

        for ((nombre, boton) in botonesMenosCombo) {
            boton.setOnClickListener {
                val actual = maxOf((cantidadesCombo[nombre] ?: 0) - 1, 0)
                cantidadesCombo[nombre] = actual
                txtCombos[nombre]?.text = "$actual"
                recalcularTotal()
                val productosSeleccionados = obtenerProductosDesdeInputs()
                mostrarResumen(productosSeleccionados)
            }
        }




        // DB y panel admin
        // La base de datos ya fue inicializada al inicio de onCreate, no es necesario volver a asignarla aquí
        // appDatabase = AppDatabase.getDatabase(this, lifecycleScope)
        drawerLayout = findViewById(R.id.drawerLayout)
        adminSummaryTextView = findViewById(R.id.adminSummaryTextView)

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders)
        recyclerViewOrders.layoutManager = LinearLayoutManager(this)



        // Elementos para el filtro de ventas por producto
        spinnerProductFilter = findViewById(R.id.spinnerProductFilter)
        spinnerPeriodFilter = findViewById(R.id.spinnerPeriodFilter)
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter)
        editStartDate = findViewById(R.id.editStartDate)
        editEndDate = findViewById(R.id.editEndDate)
        btnCalcularVentas = findViewById(R.id.btnCalcularVentas)
        layoutCustomRange = findViewById(R.id.layoutCustomRange)
        tvProductSalesResult = findViewById(R.id.tvProductSalesResult)


        appDatabase = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        adminOrderAdapter = AdminOrderAdapter(
            onDelete = { orderId -> eliminarPedido(orderId) },

            // CORREGIDO: Llama a la nueva función que maneja la lógica
            onOrderClick = { order -> mostrarResumenDeOrdenGuardada(order) },

            onPrint = { order -> reimprimirTicket(order) }
        )



        recyclerViewOrders.adapter = adminOrderAdapter
        // Comienza a observar la lista de órdenes una vez que el adaptador está listo
        observarOrdenes()




        // No llamamos a cargarPedidos(). El flujo de datos de Room se encarga
        // de actualizar la lista automáticamente.



        usbManager = getSystemService(USB_SERVICE) as UsbManager

        setupProductViews()
        setupButtons()
        setupCollapsibleCategories()

        // Configura el filtro de ventas por producto (spinner, fechas, botón)
        setupProductSalesFilter()

        // registrar receiver USB
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbReceiver, filter)
        }



        // permisos y detección inicial
        checkAndRequestBluetoothPermissions()
        detectAndRequestUsbPermission()
    }

    // --- HAMBURGUESAS (a nivel de clase) ---






    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        releaseUsbDevice()
        closeBluetoothSocket()
    }

    // --- Configuración de la UI (productos, botones, secciones colapsables) ---

    private fun showVariationSelectionDialog(productName: String) {
        val variations = productVariations[productName] ?: return
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("Elige variante para $productName")
        builder.setItems(variations.toTypedArray()) { _, which ->
            val selectedVariant = variations[which]
            val productData = products[productName] ?: return@setItems

            val newItem = Producto(
                nombre = "$productName ($selectedVariant)",
                precio = productData.precio,
                cantidad = 1,
                esCombo = false
            )
            selectedVariations.add(newItem)

            // Actualizar contador UI
            updateQuantity(productName, 1)
        }
        builder.show()
    }

    private fun showVariationRemovalDialog(productName: String) {
        val prefix = "$productName ("
        // Buscar items que empiecen con "NombreProducto ("
        val items = selectedVariations.filter { it.nombre.startsWith(prefix) }

        if (items.isEmpty()) {
            updateQuantity(productName, -1)
            return
        }

        // Agrupar para el diálogo: "Chorizo (x2)"
        val grouped = items.groupBy { it.nombre }
        val displayList = grouped.map { "${it.key.substringAfter("(").substringBefore(")")} (x${it.value.size})" }.toTypedArray()
        val keyList = grouped.keys.toList()

        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("Eliminar de $productName")
        builder.setItems(displayList) { _, which ->
            val keyToRemove = keyList[which]
            // Remover SOLO UNO de la lista
            val itemToRemove = selectedVariations.firstOrNull { it.nombre == keyToRemove }
            if (itemToRemove != null) {
                selectedVariations.remove(itemToRemove)
                updateQuantity(productName, -1)
            }
        }
        builder.show()
    }

    private fun showTextInputVariationDialog(productName: String) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("Especifica $productName")
        
        val input = EditText(this)
        input.hint = "Ej: Coca Cola, Jamaica, etc."
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        input.setPadding(50, 30, 50, 30)
        builder.setView(input)
        
        builder.setPositiveButton("Agregar") { _, _ ->
            val variant = input.text.toString().trim()
            if (variant.isNotEmpty()) {
                val productData = products[productName] ?: return@setPositiveButton
                
                val newItem = Producto(
                    nombre = "$productName ($variant)",
                    precio = productData.precio,
                    cantidad = 1,
                    esCombo = false
                )
                selectedVariations.add(newItem)
                updateQuantity(productName, 1)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showSequentialGuisadoDialog(productName: String, productData: ProductData, guisadosSeleccionados: MutableList<String> = mutableListOf()) {
        val variations = productVariations[productName] ?: emptyList()
        
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("Selecciona guisado ${guisadosSeleccionados.size + 1}")
        
        val items = variations.toTypedArray()
        builder.setItems(items) { _, which ->
            val selectedVariant = variations[which]
            guisadosSeleccionados.add(selectedVariant)
            
            // Mostrar siguiente diálogo
            showSequentialGuisadoDialog(productName, productData, guisadosSeleccionados)
        }
        
        // Agregar botón "Listo" para terminar
        builder.setPositiveButton("Listo") { _, _ ->
            if (guisadosSeleccionados.isNotEmpty()) {
                // Crear nombre con todos los guisados seleccionados
                val nombreCompleto = "$productName (${guisadosSeleccionados.joinToString(" + ")})"
                
                val newItem = Producto(
                    nombre = nombreCompleto,
                    precio = productData.precio,
                    cantidad = 1,
                    esCombo = false
                )
                selectedVariations.add(newItem)
                
                // Actualizar contador UI
                updateQuantity(productName, 1)
            }
        }
        
        builder.show()
    }

    private fun setupProductViews() {
        // Mapeo de productos (nombre lógico -> views + precio)
        products["Quesadillas"] = ProductData(
            findViewById(R.id.cantidadQuesadillas),
            findViewById(R.id.btnMenosQuesadillas),
            findViewById(R.id.btnMasQuesadillas),
            30.0
        )
        products["Quesadilla/Queso"] = ProductData(
            findViewById(R.id.cantidadQuesadillaQueso),
            findViewById(R.id.btnMenosQuesadillaQueso),
            findViewById(R.id.btnMasQuesadillaQueso),
            30.0
        )
        products["Pozole Grande"] = ProductData(
            findViewById(R.id.cantidadPozoleGrande),
            findViewById(R.id.btnMenosPozoleGrande),
            findViewById(R.id.btnMasPozoleGrande),
            110.0
        )
        products["Pozole Chico"] = ProductData(
            findViewById(R.id.cantidadPozoleChico),
            findViewById(R.id.btnMenosPozoleChico),
            findViewById(R.id.btnMasPozoleChico),
            90.0
        )
        products["Tostadas"] = ProductData(
            findViewById(R.id.cantidadTostadas),
            findViewById(R.id.btnMenosTostadas),
            findViewById(R.id.btnMasTostadas),
            35.0
        )
        products["Volcanes"] = ProductData(
            findViewById(R.id.cantidadVolcanes),
            findViewById(R.id.btnMenosVolcanes),
            findViewById(R.id.btnMasVolcanes),
            60.0
        )
        products["Volcan Queso"] = ProductData(
            findViewById(R.id.cantidadVolcanQueso),
            findViewById(R.id.btnMenosVolcanQueso),
            findViewById(R.id.btnMasVolcanQueso),
            72.0
        )
        products["Guisado Extra"] = ProductData(
            findViewById(R.id.cantidadGuisadoExtra),
            findViewById(R.id.btnMenosGuisadoExtra),
            findViewById(R.id.btnMasGuisadoExtra),
            72.0
        )
        products["Guajoloyets Naturales"] = ProductData(
            findViewById(R.id.cantidadGuajoloyetNatural),
            findViewById(R.id.btnMenosGuajoloyetNatural),
            findViewById(R.id.btnMasGuajoloyetNatural),
            60.0
        )
        products["Guajoloyets Naturales Extra"] = ProductData(
            findViewById(R.id.cantidadGujoloyetNaturalExtra),
            findViewById(R.id.btnMenosGujoloyetNaturalExtra),
            findViewById(R.id.btnMasGujoloyetNaturalExtra),
            72.0
        )
        products["Guajoloyets Adobados"] = ProductData(
            findViewById(R.id.cantidadGuajoloyetAdobado),
            findViewById(R.id.btnMenosGuajoloyetAdobado),
            findViewById(R.id.btnMasGuajoloyetAdobado),
            65.0
        )
        products["Guajoloyets Adobados Extra"] = ProductData(
            findViewById(R.id.cantidadGujoloyetAdobadoExtra),
            findViewById(R.id.btnMenosGujoloyetAdobadoExtra),
            findViewById(R.id.btnMasGujoloyetAdobadoExtra),
            77.0
        )
        products["Pambazos Naturales"] = ProductData(
            findViewById(R.id.cantidadPambazosNaturales),
            findViewById(R.id.btnMenosPambazosNaturales),
            findViewById(R.id.btnMasPambazosNaturales),
            35.0
        )
        products["Pambazos Naturales Combinados"] = ProductData(
            findViewById(R.id.cantidadPambazosNaturalesCombinados),
            findViewById(R.id.btnMenosPambazosNaturalesCombinados),
            findViewById(R.id.btnMasPambazosNaturalesCombinados),
            42.0
        )
        products["Pambazos Naturales Combinados con Queso"] = ProductData(
            findViewById(R.id.cantidadPambazosNaturalesCombinadosQueso),
            findViewById(R.id.btnMenosPambazosNaturalesCombinadosQueso),
            findViewById(R.id.btnMasPambazosNaturalesCombinadosQueso),
            54.0
        )
        products["Pambazos Naturales Extra"] = ProductData(
            findViewById(R.id.cantidadPambazosNaturalesQueso),
            findViewById(R.id.btnMenosPambazosNaturalesQueso),
            findViewById(R.id.btnMasPambazosNaturalesQueso),
            47.0
        )
        products["Pambazos Adobados"] = ProductData(
            findViewById(R.id.cantidadPambazosAdobados),
            findViewById(R.id.btnMenosPambazosAdobados),
            findViewById(R.id.btnMasPambazosAdobados),
            40.0
        )
        products["Pambazos Adobados Combinados"] = ProductData(
            findViewById(R.id.cantidadPambazosAdobadosCombinados),
            findViewById(R.id.btnMenosPambazosAdobadosCombinados),
            findViewById(R.id.btnMasPambazosAdobadosCombinados),
            47.0
        )
        products["Pambazos Adobados Combinados con Queso"] = ProductData(
            findViewById(R.id.cantidadPambazosAdobadosCombinadosQueso),
            findViewById(R.id.btnMenosPambazosAdobadosCombinadosQueso),
            findViewById(R.id.btnMasPambazosAdobadosCombinadosQueso),
            59.0
        )
        products["Pambazos Adobados Extra"] = ProductData(
            findViewById(R.id.cantidadPambazosAdobadosExtra),
            findViewById(R.id.btnMenosPambazosAdobadosExtra),
            findViewById(R.id.btnMasPambazosAdobadosExtra),
            52.0
        )
        products["Chalupas"] = ProductData(
            findViewById(R.id.cantidadChalupas),
            findViewById(R.id.btnMenosChalupas),
            findViewById(R.id.btnMasChalupas),
            5.0
        )
        products["Alones"] = ProductData(
            findViewById(R.id.cantidadAlones),
            findViewById(R.id.btnMenosAlones),
            findViewById(R.id.btnMasAlones),
            25.0
        )
        products["Mollejas"] = ProductData(
            findViewById(R.id.cantidadMollejas),
            findViewById(R.id.btnMenosMollejas),
            findViewById(R.id.btnMasMollejas),
            25.0
        )
        products["Higados"] = ProductData(
            findViewById(R.id.cantidadHigados),
            findViewById(R.id.btnMenosHigados),
            findViewById(R.id.btnMasHigados),
            22.0
        )
        products["Patitas"] = ProductData(
            findViewById(R.id.cantidadPatitas),
            findViewById(R.id.btnMenosPatitas),
            findViewById(R.id.btnMasPatitas),
            22.0
        )
        products["Huevos"] = ProductData(
            findViewById(R.id.cantidadHuevos),
            findViewById(R.id.btnMenosHuevos),
            findViewById(R.id.btnMasHuevos),
            20.0
        )
        products["Refrescos"] = ProductData(
            findViewById(R.id.cantidadRefrescos),
            findViewById(R.id.btnMenosRefrescos),
            findViewById(R.id.btnMasRefrescos),
            26.0
        )
        products["Cafe"] = ProductData(
            findViewById(R.id.cantidadCafe),
            findViewById(R.id.btnMenosCafe),
            findViewById(R.id.btnMasCafe),
            22.0
        )
        products["Aguas de Sabor"] = ProductData(
            findViewById(R.id.cantidadAguasSabor),
            findViewById(R.id.btnMenosAguasSabor),
            findViewById(R.id.btnMasAguasSabor),
            25.0
        )
        products["Agua Natural"] = ProductData(
            findViewById(R.id.cantidadAguasNat),
            findViewById(R.id.btnMenosAguasNat),
            findViewById(R.id.btnMasAguasNat),
            20.0
        )
        products["Agua para Te"] = ProductData(
            findViewById(R.id.cantidadAguaTe),
            findViewById(R.id.btnMenosAguaTe),
            findViewById(R.id.btnMasAguaTe),
            20.0
        )


// ===== PAPAS =====
        products["Orden de Papas Sencillas"] = ProductData(
            findViewById(R.id.cantidadPapasSencillas),
            findViewById(R.id.btnMenosPapasSencillas),
            findViewById(R.id.btnMasPapasSencillas),
            50.0
        )

        products["Orden de Papas Queso y Tocino"] = ProductData(
            findViewById(R.id.cantidadPapasQuesoTocino),
            findViewById(R.id.btnMenosPapasQuesoTocino),
            findViewById(R.id.btnMasPapasQuesoTocino),
            65.0
        )

// ===== TACOS (precio por pieza) =====
        products["Taco (c/u)"] = ProductData(
            findViewById(R.id.cantidadTacoUnitario),
            findViewById(R.id.btnMenosTacoUnitario),
            findViewById(R.id.btnMasTacoUnitario),
            25.0
        )

        products["Taco con Queso (c/u)"] = ProductData(
            findViewById(R.id.cantidadTacoConQueso),
            findViewById(R.id.btnMenosTacoConQueso),
            findViewById(R.id.btnMasTacoConQueso),
            30.0
        )

// ===== ALITAS =====
        products["Alitas 6 pzas"] = ProductData(
            findViewById(R.id.cantidadAlitas6),
            findViewById(R.id.btnMenosAlitas6),
            findViewById(R.id.btnMasAlitas6),
            65.0
        )

        products["Alitas 10 pzas"] = ProductData(
            findViewById(R.id.cantidadAlitas10),
            findViewById(R.id.btnMenosAlitas10),
            findViewById(R.id.btnMasAlitas10),
            100.0
        )

        products["Alitas 15 pzas"] = ProductData(
            findViewById(R.id.cantidadAlitas15),
            findViewById(R.id.btnMenosAlitas15),
            findViewById(R.id.btnMasAlitas15),
            140.0
        )
        products["Combo"] = ProductData(
            findViewById(R.id.cantidadCombo),
            findViewById(R.id.btnMenosCombo),
            findViewById(R.id.btnMasCombo),
            30.0
        )



        products.forEach { (productName, productData) ->
            quantities[productName] = 0

            productData.btnMas.setOnClickListener {
                when {
                    // Productos combinados usan diálogos secuenciales
                    productName.contains("Combinados", ignoreCase = true) -> {
                        showSequentialGuisadoDialog(productName, productData)
                    }
                    textInputProducts.contains(productName) -> showTextInputVariationDialog(productName)
                    productVariations.containsKey(productName) -> showVariationSelectionDialog(productName)
                    else -> updateQuantity(productName, 1)
                }
            }
            productData.btnMenos.setOnClickListener {
                when {
                    textInputProducts.contains(productName) && (quantities[productName] ?: 0) > 0 -> showVariationRemovalDialog(productName)
                    productVariations.containsKey(productName) && (quantities[productName] ?: 0) > 0 -> showVariationRemovalDialog(productName)
                    else -> updateQuantity(productName, -1)
                }
            }

            // chalupas es EditText libre
            if (productName == "Chalupas" && productData.cantidadTV is EditText) {
                productData.cantidadTV.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        val currentQuantity = quantities[productName] ?: 0
                        val newValue = s?.toString()?.toIntOrNull() ?: 0
                        if (currentQuantity != newValue) {
                            quantities[productName] = newValue
                        }
                        // Actualizar resumen en tiempo real al cambiar la cantidad de chalupas
                        val productosSeleccionados = obtenerProductosDesdeInputs()
                        mostrarResumen(productosSeleccionados)
                    }
                })
            }
        }
    }

    // plegables por categoría
    private fun setupCollapsibleCategories() {
        setupCollapsibleView(
            findViewById(R.id.headerPlatillos),
            findViewById(R.id.gridPlatillos),
            findViewById(R.id.arrowPlatillos)
        )
        setupCollapsibleView(
            findViewById(R.id.headerPambazos),
            findViewById(R.id.gridPambazos),
            findViewById(R.id.arrowPambazos)
        )
        setupCollapsibleView(
            findViewById(R.id.headerGuajoloyets),
            findViewById(R.id.gridGuajoloyets),
            findViewById(R.id.arrowGuajoloyets)
        )
        setupCollapsibleView(
            findViewById(R.id.headerEntradas),
            findViewById(R.id.gridEntradas),
            findViewById(R.id.arrowEntradas)
        )
        setupCollapsibleView(
            findViewById(R.id.headerBebidas),
            findViewById(R.id.gridBebidas),
            findViewById(R.id.arrowBebidas)
        )
        setupCollapsibleView(
            findViewById(R.id.headerHamburguesas),
            findViewById(R.id.gridHamburguesas),
            findViewById(R.id.arrowHamburguesas)
        )
        setupCollapsibleView(
            findViewById(R.id.headerTacos),
            findViewById(R.id.gridTacos),
            findViewById(R.id.arrowTacos)
        )
        setupCollapsibleView(
            findViewById(R.id.headerAlitas),
            findViewById(R.id.gridAlitas),
            findViewById(R.id.arrowAlitas)
        )
        setupCollapsibleView(
            findViewById(R.id.headerPostres),
            findViewById(R.id.containerNotasExtras),
            findViewById(R.id.arrowPostres)
        )
    }

    private fun setupCollapsibleView(header: View, content: View, arrow: ImageView) {
        header.setOnClickListener {
            val expand = content.visibility == View.GONE
            content.visibility = if (expand) View.VISIBLE else View.GONE
            arrow.rotateArrow(expand)
        }
    }

    private fun setupButtons() {
        btnImprimir = findViewById(R.id.btnImprimir)
        btnEmparejar = findViewById(R.id.btnEmparejar)
        btnLimpiar = findViewById(R.id.btnLimpiar)

        editTextMesa = findViewById(R.id.editMesa)
        noCuenta = findViewById(R.id.noCuenta)

        summaryContainer = findViewById(R.id.summaryContainer)
        summaryTextView = findViewById(R.id.summaryTextView)
        summaryTotalTextView = findViewById(R.id.summaryTotalTextView)
        btnCloseSummary = findViewById(R.id.btnCloseSummary)

        btnImprimir.setOnClickListener {
            // Evita múltiples toques rápidos que generan órdenes duplicadas
            if (isPrinting) return@setOnClickListener
            isPrinting = true

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1️⃣ Obtener los productos seleccionados desde la UI
                    val productosSeleccionados = obtenerProductosDesdeInputs()
                    if (productosSeleccionados.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "No hay productos seleccionados",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isPrinting = false
                        return@launch
                    }

                    // 2️⃣ Generar el texto del ticket UNA vez
                    val textoTicket = generarTextoTicket(productosSeleccionados)

                    // 3️⃣ Guardar la orden en la base de datos
                    val mesaInfo = editTextMesa.text.toString().trim()
                    guardarOrden(productosSeleccionados, mesaInfo)

                    // 4️⃣ Imprimir automáticamente sin mostrar vista previa
                    val exito = printViaUsb(textoTicket) || printViaBluetooth(textoTicket)

                    // 5️⃣ Mostrar mensaje en la UI (no actualizamos el resumen aquí)
                    withContext(Dispatchers.Main) {
                        if (exito) {
                            Toast.makeText(
                                this@MainActivity,
                                "Ticket impreso correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "No se pudo imprimir el ticket",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        // No actualizamos el resumen al imprimir; éste se actualiza en tiempo real al agregar productos
                        isPrinting = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al generar ticket: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isPrinting = false
                }
            }
        }





        btnEmparejar.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No se pudo abrir la configuración de Bluetooth",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnLimpiar.setOnClickListener {
            limpiarCantidades()
        }


        btnCloseSummary.setOnClickListener { ocultarResumen() }
    }

    // -------------------------------------------------------------------------
    // Filtro de ventas por producto
    // -------------------------------------------------------------------------

    /**
     * Configura el filtro de ventas por producto: pobla los spinners de producto y periodo,
     * muestra/oculta campos de fecha personalizados según la selección, y define la lógica
     * para calcular las ventas del producto seleccionado en el rango seleccionado.
     */
    private fun setupProductSalesFilter() {
        // Poblar spinner de productos: une nombres de hamburguesas y otros productos
        val productNames: MutableList<String> = mutableSetOf<String>().apply {
            // nombres de hamburguesas (normales y combos comparten nombre base)
            addAll(preciosHamburguesas.keys)
            // nombres de productos generales (otros platillos y extras)
            addAll(products.keys)
        }.toMutableList().sorted().toMutableList()

        // Adaptador para el spinner de productos
        val productoAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            productNames
        )
        productoAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerProductFilter.adapter = productoAdapter

        // Adaptador para el spinner de tipo (Normal, Combo, Todos)
        val typeOptions = listOf("Todos", "Normal", "Combo")
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            typeOptions
        )
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        // Por defecto, ocultar el spinner de tipo hasta que se seleccione un producto que lo requiera
        spinnerTypeFilter.visibility = View.GONE

        // Mostrar u ocultar el spinner de tipo según si el producto tiene variante combo
        spinnerProductFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedName = productNames[position]
                // Si es una hamburguesa, mostrar el spinner de tipo; de lo contrario ocultarlo
                if (preciosHamburguesas.containsKey(selectedName)) {
                    spinnerTypeFilter.visibility = View.VISIBLE
                    // por defecto, seleccionar "Todos" para combos y normales
                    spinnerTypeFilter.setSelection(0)
                } else {
                    spinnerTypeFilter.visibility = View.GONE
                    // si se oculta, seleccionar "Todos" para evitar null
                    spinnerTypeFilter.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        // Opciones de periodo
        val periodOptions = listOf(
            "Hoy",
            "Esta semana",
            "Este mes",
            "Rango específico"
        )
        val periodAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            periodOptions
        )
        periodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerPeriodFilter.adapter = periodAdapter

        // Mostrar u ocultar el rango personalizado según la selección del periodo
        spinnerPeriodFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = periodOptions[position]
                if (selected == "Rango específico") {
                    layoutCustomRange.visibility = View.VISIBLE
                } else {
                    layoutCustomRange.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }

        // Configurar date pickers para seleccionar fechas de inicio y fin
        editStartDate.setOnClickListener { showDatePicker(editStartDate) }
        editEndDate.setOnClickListener { showDatePicker(editEndDate) }

        // Listener para el botón de calcular ventas
        btnCalcularVentas.setOnClickListener {
            val selectedProduct = spinnerProductFilter.selectedItem as? String ?: return@setOnClickListener
            val selectedPeriod = spinnerPeriodFilter.selectedItem as? String ?: return@setOnClickListener
            val selectedType = spinnerTypeFilter.selectedItem as? String ?: "Todos"

            // Determinar las fechas de inicio y fin según el periodo seleccionado
            val range: Pair<Long, Long>? = when (selectedPeriod) {
                "Hoy" -> {
                    val now = Date()
                    Pair(getStartOfDay(now), getEndOfDay(now))
                }
                "Esta semana" -> {
                    getStartAndEndOfWeek(Date())
                }
                "Este mes" -> {
                    getStartAndEndOfMonth(Date())
                }
                "Rango específico" -> {
                    // Obtener fechas de los EditText. Si están vacías, mostrar mensaje y regresar
                    val startStr = editStartDate.text.toString().trim()
                    val endStr = editEndDate.text.toString().trim()
                    if (startStr.isEmpty() || endStr.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Seleccione las fechas de inicio y fin",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val startDate = sdf.parse(startStr) ?: Date()
                        val endDate = sdf.parse(endStr) ?: Date()
                        // Validar que la fecha inicio no sea mayor a fin
                        if (startDate.after(endDate)) {
                            Toast.makeText(
                                this,
                                "La fecha de inicio no puede ser posterior a la fecha de fin",
                                Toast.LENGTH_LONG
                            ).show()
                            return@setOnClickListener
                        }
                        Pair(getStartOfDay(startDate), getEndOfDay(endDate))
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Formato de fecha inválido",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
                else -> null
            }

            if (range != null) {
                calcularVentasProducto(selectedProduct, range.first, range.second, selectedType)
            }
        }
    }

    /**
     * Muestra un DatePickerDialog para seleccionar una fecha y la asigna al EditText objetivo.
     */
    private fun showDatePicker(targetEdit: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                targetEdit.setText(sdf.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /**
     * Calcula las ventas de un producto entre dos timestamps (inclusive) y actualiza la UI con el resultado.
     */
    /**
     * Calcula las ventas de un producto (y su variante) entre dos timestamps (inclusive) y actualiza la UI con el resultado.
     *
     * @param nombreProducto El nombre base del producto (por ejemplo, "Hamburguesa Hawaiana").
     * @param inicio         Timestamp de inicio del rango (inclusive).
     * @param fin            Timestamp de fin del rango (inclusive).
     * @param tipo           Variante a filtrar: "Normal", "Combo" o "Todos". Para productos sin variante, se ignora.
     */
    private fun calcularVentasProducto(nombreProducto: String, inicio: Long, fin: Long, tipo: String = "Todos") {
        lifecycleScope.launch(Dispatchers.IO) {
            // Obtener todas las órdenes
            val orders = appDatabase.orderDao().getAllOrders()
            var totalNormales = 0.0
            var unidadesNormales = 0
            var totalCombos = 0.0
            var unidadesCombos = 0
            // Filtrar por rango de fechas
            val filteredOrders = orders.filter { order ->
                order.createdAt >= inicio && order.createdAt <= fin
            }
            // Para cada orden en el rango, sumar las ventas del producto
            for (order in filteredOrders) {
                val items = appDatabase.orderDao().getItemsForOrder(order.orderId)
                for (item in items) {
                    // Extraer el nombre base sin sufijo de combo si existe
                    val baseName = if (item.esCombo) {
                        // Soporta tanto los nombres persistidos antiguos " (Combo)" como el nuevo formato " + Combo"
                        item.name.substringBefore(" + Combo").substringBefore(" (Combo)")
                    } else {
                        item.name
                    }
                    if (baseName == nombreProducto) {
                        if (item.esCombo) {
                            totalCombos += item.unitPrice * item.quantity
                            unidadesCombos += item.quantity
                        } else {
                            totalNormales += item.unitPrice * item.quantity
                            unidadesNormales += item.quantity
                        }
                    }
                }
            }
            // Determinar resultados según el tipo
            val (unidadesVendidas, totalVentas) = when (tipo) {
                "Normal" -> unidadesNormales to totalNormales
                "Combo" -> unidadesCombos to totalCombos
                else -> (unidadesNormales + unidadesCombos) to (totalNormales + totalCombos)
            }
            withContext(Dispatchers.Main) {
                // Actualizar resultado en la UI
                val resultadoTexto: String = if (unidadesVendidas > 0) {
                    when (tipo) {
                        "Normal" -> "Ventas de $nombreProducto (normal): $unidadesVendidas unidad(es), Total: ${totalVentas.formatMoney()}"
                        "Combo" -> "Ventas de $nombreProducto combo: $unidadesVendidas unidad(es), Total: ${totalVentas.formatMoney()}"
                        else -> {
                            // Mostrar resumen separado si hay ventas en ambas variantes
                            val partes = mutableListOf<String>()
                            if (unidadesNormales > 0) {
                                partes.add("Normales: $unidadesNormales unidad(es), Total: ${totalNormales.formatMoney()}")
                            }
                            if (unidadesCombos > 0) {
                                partes.add("Combo: $unidadesCombos unidad(es), Total: ${totalCombos.formatMoney()}")
                            }
                            if (partes.isNotEmpty()) {
                                "Ventas de $nombreProducto:\n" + partes.joinToString("\n")
                            } else {
                                // Fallback por si acaso (no debería ocurrir)
                                "Ventas de $nombreProducto: $unidadesVendidas unidad(es), Total: ${totalVentas.formatMoney()}"
                            }
                        }
                    }
                } else {
                    "No se encontraron ventas para $nombreProducto en el periodo seleccionado"
                }
                tvProductSalesResult.text = resultadoTexto
            }
        }
    }



    /**
     * Calcula el timestamp de inicio y fin del día para la fecha dada.
     */
    private fun getStartOfDay(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Devuelve el inicio (lunes) y fin (domingo) de la semana de la fecha dada.
     */
    private fun getStartAndEndOfWeek(date: Date): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.firstDayOfWeek = Calendar.MONDAY
        // Ajustar al inicio de la semana
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        // Fin de la semana (domingo)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    /**
     * Devuelve el inicio y fin del mes de la fecha dada.
     */
    private fun getStartAndEndOfMonth(date: Date): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        // Fin del mes
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    // -------------------------------------------------------------------------
    // Lógica de cantidades / impresión / resumen en pantalla
    // -------------------------------------------------------------------------

    private fun updateQuantity(productName: String, change: Int) {
        val currentQuantity = quantities[productName] ?: 0
        val newQuantity = (currentQuantity + change).coerceAtLeast(0)
        quantities[productName] = newQuantity
        val view = products[productName]?.cantidadTV
        view?.text = newQuantity.toString()
        if (view is EditText) {
            view.setSelection(view.text.length)
        }

        // 🔸 Actualizar resumen en tiempo real con los productos seleccionados
        // Se obtiene la lista actual de productos (incluye hamburguesas y combos)
        val productosSeleccionados = obtenerProductosDesdeInputs()
        mostrarResumen(productosSeleccionados)
    }

    private fun limpiarCantidades() {
        // 🔹 1) Reiniciar todos los productos del mapa general
    selectedVariations.clear()
    
    // Limpiar notas/extras
    val editNotas = findViewById<EditText>(R.id.editNotasExtras)
    editNotas.text.clear()
    
    for ((nombre, data) in products) {
            quantities[nombre] = 0
            data.cantidadTV.text = "0"
        }

        // 🔹 2) Reiniciar hamburguesas (normales y combos)
        for (nombre in preciosHamburguesas.keys) {
            cantidadesNormales[nombre] = 0
            cantidadesCombo[nombre] = 0
        }

        // 🔹 3) TextViews de hamburguesas normales
        val idsNormales = listOf(
            R.id.cantidadHamburguesaClasicaNormal,
            R.id.cantidadHamburguesaHawaianaNormal,
            R.id.cantidadHamburguesaPolloNormal,
            R.id.cantidadHamburguesaChampinonesNormal,
            R.id.cantidadHamburguesaArracheraNormal,
            R.id.cantidadHamburguesaMageyNormal,
            R.id.cantidadHamburguesaDobleNormal
        )

        // 🔹 4) TextViews de hamburguesas combo
        val idsCombos = listOf(
            R.id.cantidadHamburguesaClasicaCombo,
            R.id.cantidadHamburguesaHawaianaCombo,
            R.id.cantidadHamburguesaPolloCombo,
            R.id.cantidadHamburguesaChampinonesCombo,
            R.id.cantidadHamburguesaArracheraCombo,
            R.id.cantidadHamburguesaMageyCombo,
            R.id.cantidadHamburguesaDobleCombo
        )

        // 🔹 5) Establecer textos en “Normales: 0” y “Combos: 0”
        idsNormales.forEach { id ->
            findViewById<TextView>(id)?.text = "Normales: 0"
        }

        idsCombos.forEach { id ->
            findViewById<TextView>(id)?.text = "Combos: 0"
        }

        // 🔹 6) Limpiar campo de mesa y resumen. Mantener visible el resumen actual
        editTextMesa.setText("")
        summaryTextView.text = ""
        summaryTotalTextView.text = "TOTAL: $0.00"
        // No ocultamos el summaryContainer para que el resumen actual no desaparezca
        summaryContainer.visibility = View.VISIBLE

        // 🔹 7) Confirmación visual
        Toast.makeText(this, "Todas las cantidades se han restablecido a 0", Toast.LENGTH_SHORT).show()
    }





    fun imprimirTicket(textoTicket: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val productosSeleccionados = obtenerProductosDesdeInputs()

            withContext(Dispatchers.Main) {
                if (productosSeleccionados.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay productos seleccionados",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }
            }

            // 🔹 Generar texto del ticket
            val ticketTexto = generarTextoTicket(productosSeleccionados)

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Vista previa del ticket")
                    .setMessage(ticketTexto)
                    .setPositiveButton("Imprimir") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val printed = printViaUsb(ticketTexto) || printViaBluetooth(ticketTexto)
                            withContext(Dispatchers.Main) {
                                if (printed) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Ticket impreso correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo imprimir el ticket",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            requestBluetoothPermissionLauncher.launch(permissions)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // -------------------------------------------------------------------------
    // *** BLUETOOTH PRINT ***
    // -------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? =
        withContext(Dispatchers.IO) {
            val adapter = bluetoothAdapter
            if (adapter == null) {
                Log.e(TAG, "Adaptador Bluetooth no disponible.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Bluetooth no disponible en este dispositivo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            if (!adapter.isEnabled) {
                Log.i(TAG, "Bluetooth no activado. Solicitando activación.")
                withContext(Dispatchers.Main) {
                    val enableBtIntent =
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                    Toast.makeText(
                        this@MainActivity,
                        "Activando Bluetooth...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Log.e(TAG, "No hay dispositivos emparejados")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay dispositivos Bluetooth emparejados",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext null
            }

            // Buscar impresora cuyo nombre contenga "POS-58", "5890", etc.
            val printerDevice = pairedDevices.firstOrNull { device ->
                val name = device.name ?: ""
                PRINTER_BT_NAMES.any { sig ->
                    name.contains(sig, ignoreCase = true)
                }
            }

            if (printerDevice == null) {
                Log.e(TAG, "No encontré impresora tipo POS-58 entre los emparejados")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No encontré impresora POS-58 emparejada",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext null
            }

            Log.d(
                TAG,
                "Intentando conectar con la impresora Bluetooth: ${printerDevice.name} [${printerDevice.address}]"
            )

            return@withContext try {
                val socket =
                    printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
                adapter.cancelDiscovery()
                socket.connect()
                Log.d(
                    TAG,
                    "Conexión Bluetooth establecida con ${printerDevice.name}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Conectado a ${printerDevice.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                bluetoothSocket = socket
                socket
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error al conectar por Bluetooth con ${printerDevice.name}: ${e.message}",
                    e
                )
                try {
                    bluetoothSocket?.close()
                } catch (_: IOException) {
                }
                bluetoothSocket = null
                null
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(textoTicket: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val socket = bluetoothSocket ?: connectToBluetoothPrinter()
                if (socket == null || !socket.isConnected) {
                    Log.e(TAG, "No se pudo conectar a la impresora Bluetooth")
                    return@withContext false
                }

                val outputStream: OutputStream = socket.outputStream

                // ESC @ -> reset impresora
                val initPrinter = byteArrayOf(0x1B, 0x40)

                // Texto del ticket con codificación occidental
                // (windows-1252 imprime bien ñ, acentos en muchas POS 58mm)
                val ticketBytes =
                    textoTicket.toByteArray(Charset.forName("windows-1252"))

                // Alimentar papel y comando de corte (ignorado si no tiene cortador)
                val feedAndCut = byteArrayOf(
                    0x0A, 0x0A, 0x0A, 0x0A,
                    0x1D, 0x56, 0x42, 0x00
                )

                outputStream.write(initPrinter)
                outputStream.write(ticketBytes)
                outputStream.write(feedAndCut)
                outputStream.flush()

                true
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error de E/S al imprimir por Bluetooth: ${e.message}",
                    e
                )
                false
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "Error de permisos BT al imprimir: ${e.message}",
                    e
                )
                false
            } finally {
                closeBluetoothSocket()
            }
        }

    private fun closeBluetoothSocket() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.d(TAG, "Socket Bluetooth cerrado.")
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar socket Bluetooth: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // *** USB PRINT ***
    // -------------------------------------------------------------------------

    private fun detectAndRequestUsbPermission() {
        val deviceList = usbManager.deviceList.values
        if (deviceList.isEmpty()) {
            Log.d(TAG, "No hay dispositivos USB conectados")
            return
        }

        // 1. Buscar vendor/product conocidos
        var printerDevice = deviceList.firstOrNull { dev ->
            PRINTER_USB_IDS.contains(Pair(dev.vendorId, dev.productId))
        }

        // 2. Si no, buscar interfaz con clase PRINTER
        if (printerDevice == null) {
            printerDevice = deviceList.firstOrNull { dev ->
                (0 until dev.interfaceCount).any { idx ->
                    dev.getInterface(idx).interfaceClass ==
                            UsbConstants.USB_CLASS_PRINTER
                }
            }
        }

        // 3. Fallback: primero disponible
        if (printerDevice == null) {
            printerDevice = deviceList.firstOrNull()
        }

        if (printerDevice == null) {
            Log.d(TAG, "No se detectó impresora USB compatible")
            return
        }

        if (usbManager.hasPermission(printerDevice)) {
            Log.d(TAG, "Permiso USB ya concedido para: ${printerDevice.deviceName}")
            setupUsbDevice(printerDevice)
        } else {
            Log.d(TAG, "Solicitando permiso USB para: ${printerDevice.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(printerDevice, permissionIntent)
        }
    }

    private fun setupUsbDevice(device: UsbDevice) {
        releaseUsbDevice()

        usbDeviceConnection = usbManager.openDevice(device)
        if (usbDeviceConnection == null) {
            Toast.makeText(
                this,
                "No se pudo abrir la conexión USB",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // buscar interfaz tipo impresora + endpoint bulk OUT
        for (i in 0 until device.interfaceCount) {
            val usbIface = device.getInterface(i)
            if (usbIface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                usbInterface = usbIface
                for (j in 0 until usbIface.endpointCount) {
                    val endpoint = usbIface.getEndpoint(j)
                    if (
                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        endpoint.direction == UsbConstants.USB_DIR_OUT
                    ) {
                        usbEndpointOut = endpoint
                    }
                }
            }
        }

        if (usbInterface == null || usbEndpointOut == null) {
            Toast.makeText(
                this,
                "No se encontró interfaz de impresora USB",
                Toast.LENGTH_SHORT
            ).show()
            releaseUsbDevice()
            return
        }

        if (!usbDeviceConnection!!.claimInterface(usbInterface, true)) {
            Toast.makeText(
                this,
                "No se pudo reclamar la interfaz USB",
                Toast.LENGTH_SHORT
            ).show()
            releaseUsbDevice()
            return
        }

        this.usbDevice = device
        Toast.makeText(
            this,
            "Impresora USB lista: ${device.deviceName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private suspend fun printViaUsb(data: String): Boolean =
        withContext(Dispatchers.IO) {
            if (usbDeviceConnection == null || usbEndpointOut == null) {
                Log.w(TAG, "Dispositivo USB no configurado. Reintentando detección.")
                withContext(Dispatchers.Main) { detectAndRequestUsbPermission() }
                return@withContext false
            }

            try {
                // ESC @ -> reset impresora
                val initPrinter = byteArrayOf(0x1B, 0x40)

                // Ticket con acentos correcto
                val ticketBytes =
                    data.toByteArray(Charset.forName("windows-1252"))

                // Alimentación y corte opcional
                val feedAndCut = byteArrayOf(
                    0x0A, 0x0A, 0x0A, 0x0A,
                    0x1D, 0x56, 0x42, 0x00
                )

                val fullJob = ByteArray(
                    initPrinter.size + ticketBytes.size + feedAndCut.size
                )
                System.arraycopy(initPrinter, 0, fullJob, 0, initPrinter.size)
                System.arraycopy(
                    ticketBytes,
                    0,
                    fullJob,
                    initPrinter.size,
                    ticketBytes.size
                )
                System.arraycopy(
                    feedAndCut,
                    0,
                    fullJob,
                    initPrinter.size + ticketBytes.size,
                    feedAndCut.size
                )

                val sentBytes = usbDeviceConnection!!.bulkTransfer(
                    usbEndpointOut!!,
                    fullJob,
                    fullJob.size,
                    5000
                )

                if (sentBytes >= 0) {
                    Log.d(TAG, "Enviados $sentBytes bytes vía USB.")
                    true
                } else {
                    Log.e(
                        TAG,
                        "Error al enviar datos USB, sentBytes=$sentBytes"
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error de transmisión USB",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    releaseUsbDevice()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción USB al imprimir: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Excepción de USB: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                releaseUsbDevice()
                false
            }
        }

    private fun releaseUsbDevice() {
        usbDeviceConnection?.let { conn ->
            usbInterface?.let { intf ->
                conn.releaseInterface(intf)
                Log.d(TAG, "Interfaz USB liberada.")
            }
            conn.close()
            Log.d(TAG, "Conexión USB cerrada.")
        }
        usbDeviceConnection = null
        usbInterface = null
        usbEndpointOut = null
        usbDevice = null
    }

    private fun mostrarResumen(productos: List<Producto>) {
        // Referencias al layout
        val summaryContainer = findViewById<View>(R.id.summaryContainer)
        val summaryTextView = findViewById<TextView>(R.id.summaryTextView)
        val summaryTotalTextView = findViewById<TextView>(R.id.summaryTotalTextView)
        val btnCloseSummary = findViewById<Button>(R.id.btnCloseSummary)

        val sb = StringBuilder()
        var totalGeneral = 0.0

        // Separar normales vs combos (requiere Producto.esCombo)
        val combos = mutableListOf<Producto>()
        val normales = mutableListOf<Producto>()
        for (p in productos) if (p.esCombo) combos.add(p) else normales.add(p)

        if (normales.isNotEmpty()) {
            sb.appendLine(" PRODUCTOS")
            for (p in normales) {
                val total = p.cantidad * p.precio
                totalGeneral += total
                sb.appendLine("${p.cantidad} x ${p.nombre} ... $${"%.2f".format(total)}")
            }
            sb.appendLine("-----------------------------------")
        }

        if (combos.isNotEmpty()) {
            sb.appendLine(" COMBOS")
            for (c in combos) {
                val total = c.cantidad * c.precio
                totalGeneral += total
                sb.appendLine("${c.cantidad} x ${c.nombre} ... $${"%.2f".format(total)}")
            }
            sb.appendLine("-----------------------------------")
        }

        // Mostrar número de cuenta si aplica (ANTES de pintar el TextView)
        if (noCuenta.isChecked) {
            sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)}")
        }

        // Pintar
        summaryTextView.text = sb.toString()
        summaryTotalTextView.text = "TOTAL: $${"%.2f".format(totalGeneral)}"
        summaryContainer.visibility = View.VISIBLE

        // Botón cerrar
        btnCloseSummary.setOnClickListener { summaryContainer.visibility = View.GONE }
    }

    private fun mostrarResumenDeOrdenGuardada(order: OrderEntity) {
        // 1. Usar una corrutina para acceder a la base de datos en un hilo secundario
        lifecycleScope.launch(Dispatchers.IO) {
            // 2. Obtener la lista de artículos para la orden seleccionada
            val itemsDeLaOrden = appDatabase.orderDao().getItemsForOrder(order.orderId)


            val productosParaResumen = itemsDeLaOrden.map { itemEntity ->
                Producto(
                    nombre = itemEntity.name,
                    precio = itemEntity.unitPrice,
                    cantidad = itemEntity.quantity,
                    esCombo = itemEntity.esCombo
                )
            }

            // 3. Volver al hilo principal para mostrar el diálogo
            withContext(Dispatchers.Main) {
                // 4. Llamar a la función que ya sabe cómo mostrar un resumen, pero con la lista convertida
                mostrarResumen(productosParaResumen) // <--- Se usa la nueva lista
            }
        }
    }






    private fun ocultarResumen() {
        if (::summaryContainer.isInitialized) {
            summaryContainer.visibility = View.GONE
        }
    }

    // -------------------------------------------------------------------------
    // Modelos internos
    // -------------------------------------------------------------------------

    data class ProductData(
        val cantidadTV: TextView,
        val btnMenos: Button,
        val btnMas: Button,
        val precio: Double
    )

    data class Producto(
        val nombre: String,
        val precio: Double,
        val cantidad: Int,
        val esCombo: Boolean
    ) {
        val total: Double
            get() = precio * cantidad
    }

    // -------------------------------------------------------------------------
    // Construcción del ticket ESC/POS (solo texto)
    // -------------------------------------------------------------------------

    private fun obtenerProductosDesdeInputs(): List<Producto> {
        val lista = mutableListOf<Producto>()

        // 🔹 1) Productos normales (que no son hamburguesas)
        products.forEach { (nombre, data) ->
            if (!productVariations.containsKey(nombre)) {
                val cantidad = quantities[nombre] ?: 0
                if (cantidad > 0) {
                    lista.add(
                        Producto(
                            nombre = nombre,
                            precio = data.precio,
                            cantidad = cantidad,
                            esCombo = false
                        )
                    )
                }
            }
        }
        
        // Agregar variantes agrupadas
        val grouped = selectedVariations.groupBy { it.nombre }
        for ((nombreVar, items) in grouped) {
            val first = items.first()
            lista.add(
                Producto(
                     nombre = nombreVar,
                     precio = first.precio,
                     cantidad = items.size,
                     esCombo = false
                )
            )
        }

        // 🔹 2) Hamburguesas normales
        for ((nombre, cantidad) in cantidadesNormales) {
            if (cantidad > 0) {
                val precio = preciosHamburguesas[nombre] ?: 0.0
                lista.add(
                    Producto(
                        nombre = nombre,
                        precio = precio,
                        cantidad = cantidad,
                        esCombo = false
                    )
                )
            }
        }

        // 🔹 3) Hamburguesas en combo (+$30)
        val extraCombo = 30.0
        for ((nombre, cantidad) in cantidadesCombo) {
            if (cantidad > 0) {
                val precioBase = preciosHamburguesas[nombre] ?: 0.0
                val precioCombo = precioBase + extraCombo
                lista.add(
                    Producto(
                        // Utilizar "+ Combo" en lugar de paréntesis para unificar el nombre
                        nombre = "$nombre + Combo",
                        precio = precioCombo,
                        cantidad = cantidad,
                        esCombo = true
                    )
                )
            }
        }

        return lista
    }




    private fun guardarSoloCombos() {
        CoroutineScope(Dispatchers.IO).launch {
            // 🔹 Obtener lista de productos seleccionados desde inputs (solo combos)
            val productos = obtenerProductosDesdeInputs()
            val combos = productos.filter { it.nombre.contains("Combo", ignoreCase = true) }

            if (combos.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay combos para guardar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // 🔹 Convertir productos a entidades para guardar en BD
            val comboItems = combos.map {
                OrderItemEntity(
                    orderId = 0L, // se asignará automáticamente al insertar
                    name = it.nombre,
                    unitPrice = it.precio,
                    quantity = quantities[it.nombre.substringBefore(" (x")] ?: 1, // cantidad desde UI
                    esCombo = true
                )
            }

            // 🔹 Crear la orden (solo combos)
            val order = OrderEntity(
                mesa = "Combos Especiales",
                createdAt = System.currentTimeMillis(),
                businessDate = System.currentTimeMillis(),
                grandTotal = comboItems.sumOf { it.unitPrice * it.quantity },
                esCombo = true
            )

            // 🔹 Insertar en BD usando tu DAO transaccional
            val db = AppDatabase.getDatabase(applicationContext, this)
            db.orderDao().insertOrderWithItems(order, comboItems)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Combos guardados correctamente (${combos.size})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private suspend fun generarTextoTicket(productosSeleccionados: List<Producto>): String = withContext(Dispatchers.IO){
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        val anchoTotalLinea = 32
        val lineaSeparadora = "-".repeat(anchoTotalLinea)

        // Encabezado Simple
    sb.appendLine("ANTOJITOS MEXICANOS MARGARITA")
    sb.appendLine("TICKET DE COMPRA")
    sb.appendLine("Fecha: $fechaHora")

    // Datos de cuenta si aplica
    if (noCuenta.isChecked) {
        sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)} ")
        sb.appendLine("Nombre: Margarita Daniel Pérez")
        sb.appendLine("Banco: BBVA")
    }

    // Mesa/cliente
    val mesaInfo = editTextMesa.text.toString().trim()
    if (mesaInfo.isNotEmpty()) {
        sb.appendLine("Mesa: ${mesaInfo.uppercase()}")
    }
    sb.appendLine(lineaSeparadora)

    // Obtener productos seleccionados desde tus inputs (debe devolver List<Producto>)

    // Guardar orden en BD y mostrar resumen en el layou

    // Separación normales vs combos
    val combos = mutableListOf<Producto>()
    val normales = mutableListOf<Producto>()
    for (p in productosSeleccionados) if (p.esCombo) combos.add(p) else normales.add(p)

    // SIN CABECERAS DE COLUMNA

    var totalGeneral = 0.0

    // Listado de productos normales
    if (normales.isNotEmpty()) {
        for (p in normales) {
            val totalProducto = p.precio * p.cantidad
            totalGeneral += totalProducto

            // Formato Minimalista: "Cant x Producto"
            // Ej: "2 x Quesadilla (Chorizo)"
            sb.appendLine("${p.cantidad} x ${p.nombre}")
        }
    }

    // Listado de combos
    if (combos.isNotEmpty()) {
        if (normales.isNotEmpty()) sb.appendLine(lineaSeparadora) // Separador si hay ambos
        for (c in combos) {
            val totalCombo = c.precio * c.cantidad
            totalGeneral += totalCombo

            sb.appendLine("${c.cantidad} x ${c.nombre}")
        }
    }

    // Notas/Extras/Postres
    val editNotas = findViewById<EditText>(R.id.editNotasExtras)
    val notasText = editNotas.text.toString().trim()
    if (notasText.isNotEmpty()) {
        sb.appendLine(lineaSeparadora)
        sb.appendLine("NOTAS:")
        sb.appendLine(notasText)
    }

    // Solo Total Final
    sb.appendLine(lineaSeparadora)
    sb.appendLine("TOTAL: $${String.format("%.2f", totalGeneral)}")
    sb.appendLine(lineaSeparadora)
    sb.appendLine("")
    sb.appendLine("Gracias por su compra")
    sb.appendLine("\n\n\n") // feed para corte manual

    return@withContext sb.toString()
}

    // -------------------------------------------------------------------------
    // Persistencia en base de datos y panel admin
    // -------------------------------------------------------------------------

    /** Helper: formato money */
    private fun Double.formatMoney(): String =
        "$" + String.format("%.2f", this)

    /** Actualiza lista en panel admin */
    private fun refreshOrders(order: OrderEntity, items: List<OrderItemEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 🔹 Inserta la orden con sus ítems
            appDatabase.orderDao().insertOrderWithItems(order, items)

            // 🔹 Luego obtiene todas las órdenes actualizadas
            val updatedOrders = appDatabase.orderDao().getAllOrders()

            withContext(Dispatchers.Main) {
                adminOrderAdapter.updateOrders(updatedOrders)
            }
        }
    }


    /** Guarda la orden en Room y refresca el panel */
    /** Guarda la orden en Room y refresca el panel */
    /**
     * Guarda la orden en Room. No se encarga de actualizar la UI directamente.
     */
    private fun guardarOrden(productos: List<Producto>, mesaInfo: String) {
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay productos para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val createdAt = System.currentTimeMillis()
            // ... (tu lógica para businessDate, etc.) ...

            val orderEntity = OrderEntity(
                mesa = mesaInfo.ifBlank { null },
                createdAt = createdAt,
                businessDate = createdAt, // o tu variable businessDate
                grandTotal = productos.sumOf { it.total }
            )

            val items = productos.map {
                OrderItemEntity(
                    // itemId se genera automáticamente
                    orderId = 0, // se rellena en la transacción
                    name = it.nombre,
                    unitPrice = it.precio,
                    quantity = it.cantidad,
                    esCombo = it.esCombo // Asegúrate de que tu OrderItemEntity tenga este campo
                )
            }

            // Inserta en la BD. El observador se encargará del resto.
            appDatabase.orderDao().insertOrderWithItems(orderEntity, items)
        }
    }

    /**
     * Observa la tabla de órdenes y actualiza el adaptador cada vez que hay un cambio.
     * Esta es la ÚNICA forma en que el adaptador debe recibir datos.
     */
    private fun observarOrdenes() {
        lifecycleScope.launch {
            appDatabase.orderDao().getAllOrdersFlow().collect { listaDeOrdenes ->
                // Cuando hay un cambio en la BD, la lista llega aquí
                withContext(Dispatchers.Main) {
                    // Y se la pasamos al adaptador, que se actualiza eficientemente
                    adminOrderAdapter.submitList(listaDeOrdenes)
                }
            }
        }
    }



    /** utilidad para armar texto resumen de ventas */
    private inline fun <T> buildResumen(
        resultados: List<T>,
        crossinline line: (T) -> String
    ): String {
        val sb = StringBuilder()
        var totalGeneral = 0.0
        resultados.forEach { r ->
            sb.appendLine(line(r))
            val total = when (r) {
                is DailySummary -> r.totalSales
                is WeeklySummary -> r.totalSales
                is MonthlySummary -> r.totalSales
                else -> 0.0
            }
            totalGeneral += total
        }
        sb.appendLine("------------------------------")
        sb.appendLine("Total acumulado: ${totalGeneral.formatMoney()}")
        return sb.toString()
    }

    /** Ganancia diaria */
    private fun generarGananciaDiaria() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados =
                appDatabase.orderDao().getDailySales()
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )
            val texto = buildResumen(resultados) { r ->
                "${sdf.format(Date(r.businessDate))}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) {
                adminSummaryTextView.text = texto
            }
        }
    }

    /** Ganancia semanal */
    private fun generarGananciaSemanal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados =
                appDatabase.orderDao().getWeeklySales()
            val texto = buildResumen(resultados) { r ->
                "Semana ${r.week}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) {
                adminSummaryTextView.text = texto
            }
        }
    }

    /** Ganancia mensual */
    private fun generarGananciaMensual() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados =
                appDatabase.orderDao().getMonthlySales()
            val texto = buildResumen(resultados) { r ->
                "Mes ${r.month}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) {
                adminSummaryTextView.text = texto
            }
        }
    }

    /** Carga pedidos a Recycler */
    /** Carga los pedidos en el RecyclerView */
    private fun cargarPedidos() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 🔹 Obtener pedidos desde la base de datos
                val pedidos = appDatabase.orderDao().getAllOrders()

                withContext(Dispatchers.Main) {
                    // 🔹 Actualizar adaptador
                    adminOrderAdapter.updateOrders(pedidos)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error al cargar pedidos", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("MainActivity", "Error cargando pedidos", e)
                }
            }
        }
    }


    /** Elimina un pedido y deja que el observador de la base de datos actualice la UI automáticamente. */
    private fun eliminarPedido(orderId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Elimina la orden de la base de datos en un hilo secundario.
            //    La llave foránea con onDelete = CASCADE se encargará de borrar sus items.
            appDatabase.orderDao().deleteOrderById(orderId)

            // 2. Muestra un mensaje de confirmación en el hilo principal.
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Pedido eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // ❌ NO se necesita llamar a refreshOrders() o updateOrders() aquí.
            //    El observador (getAllOrdersFlow().collect) se activará solo
            //    y le pasará la nueva lista (sin el pedido borrado) al adaptador.
        }
    }

    /**
     * Reimprime el ticket de una orden existente. Utiliza los ítems almacenados en la base de
     * datos para reconstruir el ticket, en lugar de leer la UI actual. Muestra un diálogo de
     * confirmación previo a la impresión.
     */
    private fun reimprimirTicket(order: OrderEntity) {
        // Utiliza una rutina de corrutina para recuperar los ítems y construir el ticket
        lifecycleScope.launch(Dispatchers.IO) {
            // Obtén los items de la orden desde la base de datos
            val items = appDatabase.orderDao().getItemsForOrder(order.orderId)

            // Construye una lista de Producto a partir de los items guardados
            val productos = items.map {
                Producto(
                    nombre = it.name,
                    precio = it.unitPrice,
                    cantidad = it.quantity,
                    esCombo = it.esCombo
                )
            }

            // Genera el texto del ticket con los productos recuperados
            val ticket = generarTextoTicket(productos)

            // Cambia al hilo principal para mostrar el diálogo personalizado
            withContext(Dispatchers.Main) {
                // Infla la vista personalizada para el diálogo de reimpresión de ticket
                val inflater = LayoutInflater.from(this@MainActivity)
                val dialogView = inflater.inflate(R.layout.dialog_reimprimir_ticket, null)

                // Establece el contenido del ticket en el TextView
                val ticketTextView = dialogView.findViewById<TextView>(R.id.ticketTextView)
                ticketTextView.text = ticket

                // Configura los botones dentro del diálogo
                val btnPrint = dialogView.findViewById<MaterialButton>(R.id.btnPrintTicket)
                val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelTicket)
                val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)

                // Construye el AlertDialog con la vista personalizada
                val alertDialog = AlertDialog.Builder(this@MainActivity)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                // Al imprimir, ejecuta la impresión en un hilo de fondo y cierra el diálogo
                btnPrint.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        printViaUsb(ticket) || printViaBluetooth(ticket)
                    }
                    alertDialog.dismiss()
                }

                // Cierre del diálogo al cancelar
                val dismissListener = View.OnClickListener { alertDialog.dismiss() }
                btnCancel.setOnClickListener(dismissListener)
                btnClose.setOnClickListener(dismissListener)

                alertDialog.show()
            }
        }
    }




}

private fun OrderEntity.filter(function: Any) {
    TODO("Not yet implemented")
}


