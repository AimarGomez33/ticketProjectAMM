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
    private lateinit var editTextMesa: EditText
    private lateinit var imgQR: ImageView

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

        btnImprimir.setOnClickListener { imprimirTicket() }
        btnEmparejar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }
        btnLimpiar.setOnClickListener { limpiarCantidades() }
        btnCloseSummary.setOnClickListener { ocultarResumen() }
        btnEditarPrecios.setOnClickListener { toggleEditMode() }
    }

    private fun setupRecyclerView() {
        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsAdapter = ProductsAdapter(this)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            productDao.getAllProducts().collectLatest { productList ->
                val productsWithQuantities = productList.map { product ->
                    Pair(product, quantities.getOrDefault(product.id, 0))
                }
                productsAdapter.submitList(productsWithQuantities)
            }
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
        observeProducts()
        ocultarResumen()
        Toast.makeText(this, "Cantidades restablecidas a 0", Toast.LENGTH_SHORT).show()
    }

    private fun obtenerProductosDesdeInputs(): List<Producto> {
        val listaDeProductos = mutableListOf<Producto>()
        productsAdapter.currentList.forEach { pair ->
            val product = pair.first
            val quantity = pair.second
            if (quantity > 0) {
                listaDeProductos.add(Producto(product.name, product.price, quantity))
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
            sb.appendLine(String.format("%-32s", "PARA: ${mesaInfo.uppercase()}"))
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
    private fun checkAndRequestBluetoothPermissions() { /* ... */ }
    private fun checkBluetoothPermissions(): Boolean { /* ... */ }
    @SuppressLint("MissingPermission") private suspend fun connectToBluetoothPrinter(): BluetoothSocket? { /* ... */ }
    @SuppressLint("MissingPermission") private suspend fun printViaBluetooth(textoTicket: String): Boolean { /* ... */ }
    private fun closeBluetoothSocket() { /* ... */ }
    private fun detectAndRequestUsbPermission() { /* ... */ }
    private fun setupUsbDevice(device: UsbDevice) { /* ... */ }
    private suspend fun printViaUsb(data: String): Boolean { /* ... */ }
    private fun releaseUsbDevice() { /* ... */ }
    private fun generarQR(texto: String): Bitmap { /* ... */ }
    private fun bitmapToEscPosData(bitmap: Bitmap, printerDotsPerLine: Int): ByteArray { /* ... */ }

}