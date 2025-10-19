package com.example.ticketapp

import android.Manifest
import android.annotation.SuppressLint
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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout // <<-- AÑADIDO: Importación necesaria
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.nio.charset.StandardCharsets

// Importación para mostrar diálogos
import androidx.appcompat.app.AlertDialog


import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.GravityCompat
import com.google.android.material.button.MaterialButton
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CheckBox
import com.example.ticketapp.AppDatabase
import com.example.ticketapp.AdminOrderAdapter




import com.example.ticketapp.OrderEntity
import com.example.ticketapp.OrderItemEntity
import com.example.ticketapp.DailySummary
import com.example.ticketapp.WeeklySummary
import com.example.ticketapp.MonthlySummary
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity() {

    // --- Constantes y variables de la clase (sin cambios) ---
    private companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.ticketapp.USB_PERMISSION"
        private const val PRINTER_NAME_BLUETOOTH = "BlueTooth Printer"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var summaryContainer: View
    private lateinit var summaryTextView: TextView
    private lateinit var summaryTotalTextView: TextView
    private lateinit var btnCloseSummary: Button

    private lateinit var appDatabase: AppDatabase

    private val products = mutableMapOf<String, ProductData>()
    private val quantities = mutableMapOf<String, Int>()

    private lateinit var btnImprimir: Button
    private lateinit var btnEmparejar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var editTextMesa: EditText
    private lateinit var noCuenta: CheckBox


    private var isEditMode = false

    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null


    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private var bluetoothSocket: BluetoothSocket? = null

    // --- Campos relacionados con la base de datos y la pantalla de administración ---

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnGananciaDiaria: MaterialButton
    private lateinit var btnGananciaSemanal: MaterialButton
    private lateinit var btnGananciaMensual: MaterialButton
    private lateinit var adminSummaryTextView: TextView
    private lateinit var recyclerViewOrders: RecyclerView
    private lateinit var adminOrderAdapter: AdminOrderAdapter

    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Log.d(TAG, "Permisos Bluetooth concedidos.")
                Toast.makeText(this, "Permisos Bluetooth concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Permisos Bluetooth denegados.")
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
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d(TAG, "Permiso USB concedido para: ${device.deviceName}")
                            setupUsbDevice(device)
                        }
                    } else {
                        Log.d(TAG, "Permiso USB denegado para: ${device?.deviceName}")
                        Toast.makeText(context, "Permiso USB denegado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

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

        // Inicialización de la base de datos y la interfaz de administración
        appDatabase = AppDatabase.getDatabase(this, lifecycleScope)
        drawerLayout = findViewById(R.id.drawerLayout)
        btnGananciaDiaria = findViewById(R.id.btnGananciaDiaria)
        btnGananciaSemanal = findViewById(R.id.btnGananciaSemanal)
        btnGananciaMensual = findViewById(R.id.btnGananciaMensual)
        adminSummaryTextView = findViewById(R.id.adminSummaryTextView)
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders)

        // Configurar RecyclerView para listar pedidos con un layout lineal vertical
        recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        adminOrderAdapter = AdminOrderAdapter(
            mutableListOf<OrderEntity>(),
            { orderId -> eliminarPedido(orderId) },
            { order -> mostrarResumenPedido(order) }
        )
        recyclerViewOrders.adapter = adminOrderAdapter

        // Asignar listeners para calcular ganancias
        btnGananciaDiaria.setOnClickListener { generarGananciaDiaria() }
        btnGananciaSemanal.setOnClickListener { generarGananciaSemanal() }
        btnGananciaMensual.setOnClickListener { generarGananciaMensual() }

        // Cargar pedidos existentes en la lista al arrancar
        cargarPedidos()

        usbManager = getSystemService(USB_SERVICE) as UsbManager

        setupProductViews()
        setupButtons()

        // --- INICIO DE CAMBIOS: Lógica para categorías plegables ---
        setupCollapsibleCategories()
        // --- FIN DE CAMBIOS ---

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

    // --- Configuración de la UI (Vistas y Botones) ---
    private fun setupProductViews() {
        // ... (tu lista de products[...] se queda igual)
        products["Quesadillas"] = ProductData(
            findViewById(R.id.cantidadQuesadillas),
            findViewById(R.id.btnMenosQuesadillas),
            findViewById(R.id.btnMasQuesadillas),
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
        products["Volcan Queso/Guisado Extra"] = ProductData(
            findViewById(R.id.cantidadGuajolotaExtra),
            findViewById(R.id.btnMenosGuajolotaExtra),
            findViewById(R.id.btnMasGuajolotaExtra),
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
        products["Extra +5"] = ProductData(
            findViewById(R.id.cantidadExtra5),
            findViewById(R.id.btnMenosExtra5),
            findViewById(R.id.btnMasExtra5),
            5.0
        )
        products["Extra +10"] = ProductData(
            findViewById(R.id.cantidadExtra10),
            findViewById(R.id.btnMenosExtra10),
            findViewById(R.id.btnMasExtra10),
            10.0
        )
        products["Extra +12"] = ProductData(
            findViewById(R.id.cantidadExtra12),
            findViewById(R.id.btnMenosExtra12),
            findViewById(R.id.btnMasExtra12),
            12.0
        )
        products["Postres 20"] = ProductData(
            findViewById(R.id.cantidadPostres20),
            findViewById(R.id.btnMenosPostres20),
            findViewById(R.id.btnMasPostres20),
            20.0
        )
        products["Postres 25"] = ProductData(
            findViewById(R.id.cantidadPostres25),
            findViewById(R.id.btnMenosPostres25),
            findViewById(R.id.btnMasPostres25),
            25.0
        )
        products["Postres 30"] = ProductData(
            findViewById(R.id.cantidadPostres30),
            findViewById(R.id.btnMenosPostres30),
            findViewById(R.id.btnMasPostres30),
            30.0
        )
        products["Postres 35"] = ProductData(
            findViewById(R.id.cantidadPostres35),
            findViewById(R.id.btnMenosPostres35),
            findViewById(R.id.btnMasPostres35),
            35.0
        )


        products.forEach { (productName, productData) ->
            quantities[productName] = 0
            productData.btnMas.setOnClickListener {
                updateQuantity(productName, 1)
            }
            productData.btnMenos.setOnClickListener {
                updateQuantity(productName, -1)
            }

            if (productName == "Chalupas" && productData.cantidadTV is EditText) {
                productData.cantidadTV.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        val currentQuantity = quantities[productName] ?: 0
                        val newValue = s?.toString()?.toIntOrNull() ?: 0
                        if (currentQuantity != newValue) {
                            quantities[productName] = newValue
                        }
                    }
                })
            }
        }
    }

    // --- INICIO DE CAMBIOS ---
    private fun setupCollapsibleCategories() {
        // Llama a la función de ayuda para cada categoría definida en el XML
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
            findViewById(R.id.headerPostres),
            findViewById(R.id.gridPostres),
            findViewById(R.id.arrowPostres)
        )
    }

    private fun setupCollapsibleView(header: View, content: View, arrow: ImageView) {
        // Establece el listener en la cabecera
        header.setOnClickListener {
            // Determina si debe expandirse u ocultarse el contenido
            val expand = content.visibility == View.GONE
            // Cambia la visibilidad del contenido según corresponda
            content.visibility = if (expand) View.VISIBLE else View.GONE
            // Rota la flecha usando la función de extensión
            arrow.rotateArrow(expand)
        }
    }
    // --- FIN DE CAMBIOS ---

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
            imprimirTicket()
        }
        btnEmparejar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
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

        btnCloseSummary.setOnClickListener {
            ocultarResumen()
        }
    }

    // El resto de tu código (updateQuantity, limpiarCantidades, imprimirTicket, etc.) permanece igual.
    // ...
    // --- Tu código original sin cambios desde aquí hacia abajo ---

    private fun updateQuantity(productName: String, change: Int) {
        val currentQuantity = quantities[productName] ?: 0
        val newQuantity = (currentQuantity + change).coerceAtLeast(0)
        quantities[productName] = newQuantity
        val view = products[productName]?.cantidadTV
        view?.text = newQuantity.toString()
        if (view is EditText) {
            view.setSelection(view.text.length)
        }
    }

    private fun limpiarCantidades() {
        products.keys.forEach { productName ->
            quantities[productName] = 0
            products[productName]?.cantidadTV?.text = "0"
        }
        ocultarResumen()
        Toast.makeText(this, "Cantidades restablecidas a 0", Toast.LENGTH_SHORT).show()
    }

    private fun imprimirTicket() {
        val productosSeleccionados = obtenerProductosDesdeInputs()
        if (productosSeleccionados.isEmpty()) {
            Toast.makeText(this, "No hay productos seleccionados", Toast.LENGTH_SHORT).show()
            return
        }
        // Guardar la venta en la base de datos antes de mostrar el resumen
        val mesaInfoForDb = editTextMesa.text.toString().trim()
        guardarOrden(productosSeleccionados, mesaInfoForDb)
        mostrarResumen(productosSeleccionados)
        val textoTicket = generarTextoTicket()
        lifecycleScope.launch {
            val usbSuccess = printViaUsb(textoTicket)
            if (!usbSuccess) {
                Log.w(TAG, "Fallo al imprimir por USB. Intentando Bluetooth...")
                if (checkBluetoothPermissions()) {
                    val btSuccess = printViaBluetooth(textoTicket)
                    if (btSuccess) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ticket enviado por Bluetooth",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Fallo al imprimir por Bluetooth",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permisos de Bluetooth no concedidos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Ticket enviado por USB", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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

    @SuppressLint("MissingPermission")
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? =
        withContext(Dispatchers.IO) {
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Adaptador Bluetooth no disponible.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Adaptador Bluetooth no disponible",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }
            if (!bluetoothAdapter!!.isEnabled) {
                Log.i(TAG, "Bluetooth no activado. Solicitando activación.")
                withContext(Dispatchers.Main) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                    Toast.makeText(this@MainActivity, "Activando Bluetooth...", Toast.LENGTH_SHORT)
                        .show()
                }
                return@withContext null
            }
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
            val printerDevice =
                pairedDevices?.find { it.name.equals(PRINTER_NAME_BLUETOOTH, ignoreCase = true) }
            if (printerDevice == null) {
                Log.e(
                    TAG,
                    "Impresora Bluetooth '$PRINTER_NAME_BLUETOOTH' no encontrada entre los dispositivos emparejados."
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Impresora no emparejada: $PRINTER_NAME_BLUETOOTH",
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
                val socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                Log.d(TAG, "Conexión Bluetooth establecida con ${printerDevice.name}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Conectado a ${printerDevice.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                socket
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error al conectar por Bluetooth con ${printerDevice.name}: ${e.message}",
                    e
                )
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                }
                null
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(textoTicket: String): Boolean =
        withContext(Dispatchers.IO) {
            var success = false
            try {
                bluetoothSocket = connectToBluetoothPrinter()
                val socket = bluetoothSocket
                if (socket == null || !socket.isConnected) {
                    Log.e(TAG, "No se pudo conectar a la impresora Bluetooth")
                    return@withContext false
                }
                val outputStream: OutputStream = socket.outputStream
                val initPrinter = byteArrayOf(0x1B, 0x40)
                val cutPaper = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
                val ticketBytes = textoTicket.toByteArray(Charset.forName("GB18030"))
                outputStream.write(initPrinter)
                outputStream.write(ticketBytes)
                outputStream.write(cutPaper)
                outputStream.flush()
                success = true
            } catch (e: IOException) {
                Log.e(TAG, "Error de E/S al imprimir por Bluetooth: ${e.message}", e)
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "Error de seguridad (permisos) al imprimir por Bluetooth: ${e.message}",
                    e
                )
            } finally {
                closeBluetoothSocket()
            }
            return@withContext success
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

    private fun detectAndRequestUsbPermission() {
        val deviceList = usbManager.deviceList
        val printerDevice = deviceList.values.firstOrNull {
            it.vendorId == 1155 && it.productId == 22339
        } ?: deviceList.values.firstOrNull()
        if (printerDevice == null) {
            return
        }
        if (usbManager.hasPermission(printerDevice)) {
            Log.d(TAG, "Permiso USB ya concedido para: ${printerDevice.deviceName}")
            setupUsbDevice(printerDevice)
        } else {
            Log.d(TAG, "Solicitando permiso USB para: ${printerDevice.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(printerDevice, permissionIntent)
        }
    }

    private fun setupUsbDevice(device: UsbDevice) {
        releaseUsbDevice()
        usbDeviceConnection = usbManager.openDevice(device)
        if (usbDeviceConnection == null) {
            Toast.makeText(this, "No se pudo abrir la conexión USB", Toast.LENGTH_SHORT).show()
            return
        }
        for (i in 0 until device.interfaceCount) {
            val usbIface = device.getInterface(i)
            if (usbIface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                usbInterface = usbIface
                for (j in 0 until usbIface.endpointCount) {
                    val endpoint = usbIface.getEndpoint(j)
                    if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK && endpoint.direction == UsbConstants.USB_DIR_OUT) {
                        usbEndpointOut = endpoint
                    }
                }
            }
        }
        if (usbInterface == null || usbEndpointOut == null) {
            Toast.makeText(this, "No se encontró interfaz de impresora USB", Toast.LENGTH_SHORT)
                .show()
            releaseUsbDevice()
            return
        }
        if (!usbDeviceConnection!!.claimInterface(usbInterface, true)) {
            Toast.makeText(this, "No se pudo reclamar la interfaz USB", Toast.LENGTH_SHORT).show()
            releaseUsbDevice()
            return
        }
        this.usbDevice = device
        Toast.makeText(this, "Impresora USB lista: ${device.deviceName}", Toast.LENGTH_SHORT).show()
    }

    private suspend fun printViaUsb(data: String): Boolean = withContext(Dispatchers.IO) {
        if (usbDeviceConnection == null || usbEndpointOut == null) {
            Log.w(TAG, "Dispositivo USB no configurado. Intentando re-detectar.")
            withContext(Dispatchers.Main) { detectAndRequestUsbPermission() }
            return@withContext false
        }
        return@withContext try {
            val bytes = data.toByteArray(Charset.forName("GB18030"))
            val sentBytes =
                usbDeviceConnection!!.bulkTransfer(usbEndpointOut!!, bytes, bytes.size, 5000)
            if (sentBytes >= 0) {
                Log.d(TAG, "Datos ($sentBytes bytes) enviados por USB.")
                true
            } else {
                Log.e(TAG, "Error al enviar datos por USB, sentBytes: $sentBytes")
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
            Log.e(TAG, "Excepción al imprimir por USB: ${e.message}", e)
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
        val sb = StringBuilder()
        var totalGeneral = 0.0
        productos.forEach { producto ->
            val totalProducto = producto.cantidad * producto.precio
            totalGeneral += totalProducto
            sb.appendLine(
                "${producto.cantidad} x ${producto.nombre} ... $${
                    "%.2f".format(
                        totalProducto
                    )
                }"
            )

        }

        summaryTextView.text = sb.toString()
        summaryTotalTextView.text = "TOTAL: $${"%.2f".format(totalGeneral)}"
        summaryContainer.visibility = View.VISIBLE
        if (noCuenta.isChecked) {
            sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)}")
        }
    }

    private fun ocultarResumen() {
        if (::summaryContainer.isInitialized) {
            summaryContainer.visibility = View.GONE
        }
    }

    data class ProductData(
        val cantidadTV: TextView,
        val btnMenos: Button,
        val btnMas: Button,
        val precio: Double
    )

    data class Producto(
        val nombre: String,
        val precio: Double,
        val cantidad: Int
    ) {
        val total: Double get() = precio * cantidad
    }

    private fun obtenerProductosDesdeInputs(): List<Producto> {
        val listaDeProductos = mutableListOf<Producto>()
        products.forEach { (nombre, data) ->
            val cantidad = quantities[nombre] ?: 0
            if (cantidad > 0) {
                listaDeProductos.add(Producto(nombre, data.precio, cantidad))
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

        if (noCuenta.isChecked) {
            sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)} ")
            sb.appendLine("Nombre: Margarita Daniel Perez")
            sb.appendLine("Banco: BBVA")

        }

        val mesaInfo = editTextMesa.text.toString().trim()
        if (mesaInfo.isNotEmpty()) {
            sb.appendLine(String.format("%-32s", "Mesa: ${mesaInfo.uppercase()}"))
            sb.appendLine("*****************************")
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
                val nombreProducto =
                    if (p.nombre.length > 15) p.nombre.substring(0, 12) + "..." else p.nombre
                val precioConSimbolo = String.format("$%.2f", p.precio)
                val totalConSimbolo = String.format("$%.2f", p.total)
                sb.appendLine(
                    String.format(
                        "%-15s %6s %3d %7s",
                        nombreProducto,
                        precioConSimbolo,
                        p.cantidad,
                        totalConSimbolo
                    )
                )
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


    // -----------------------------------------------------------------------------------------
    // Funciones de persistencia y administración
    // -----------------------------------------------------------------------------------------

    /**
     * Persiste una orden y sus productos en la base de datos. Calcula el campo
     * businessDate aplicando un corte a las 5:00 AM para que las ventas después
     * de medianoche se asignen al día anterior. También determina el tipo de
     * orden (DINE_IN o TAKEOUT) según si hay valor en el campo mesa.
     *
     * @param productos Lista de productos seleccionados.
     * @param mesaInfo Texto introducido en el campo mesa; si está vacío se
     *                 asumirá que es para llevar.
     */



// === BLOQUE CORREGIDO Y OPTIMIZADO — PÉGALO AL FINAL DE TU MainActivity.kt ===

    /** Formatea un Double como dinero con 2 decimales. */
    private fun Double.formatMoney(): String = "$" + String.format("%.2f", this)

    /** Refresca el listado de órdenes en el panel administrador. */
    private fun refreshOrders() {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = appDatabase.OrderDao().getAllOrders()
            withContext(Dispatchers.Main) { adminOrderAdapter.updateOrders(updated) }
        }
    }

    /** Persiste una orden y refresca la lista del panel de administración. */
    private fun guardarOrden(productos: List<Producto>, mesaInfo: String) {
        val createdAt = System.currentTimeMillis()

        // businessDate con corte 5:00 AM (misma lógica, solo más clara)
        val businessDate = Calendar.getInstance().apply {
            timeInMillis = createdAt
            if (get(Calendar.HOUR_OF_DAY) < 5) add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val subtotal = productos.sumOf { it.cantidad * it.precio }
        val grandTotal = subtotal // (misma lógica: sin propinas/iva/etc.)

        val orderEntity = OrderEntity(
            orderId = 0,
            mesa = mesaInfo.ifBlank { null },
            createdAt = createdAt,
            businessDate = businessDate,
            grandTotal = grandTotal
        )

        val items = productos.map {
            OrderItemEntity(
                itemId = 0,
                orderId = 0, // se asigna en la transacción
                name = it.nombre,
                unitPrice = it.precio,
                quantity = it.cantidad
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.OrderDao().insertOrderWithItems(orderEntity, items)
            withContext(Dispatchers.Main) { refreshOrders() }
        }
    }

    /** Construye el texto de resumen (reutilizado por diario/semanal/mensual). */
    private inline fun <T> buildResumen(
        resultados: List<T>,
        crossinline line: (T) -> String
    ): String {
        val sb = StringBuilder()
        var totalGeneral = 0.0
        resultados.forEach { r ->
            sb.appendLine(line(r))
            // Los tipos DailySummary/WeeklySummary/MonthlySummary exponen totalSales
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

    /** Calcula y muestra la ganancia DIARIA. */
    private fun generarGananciaDiaria() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados = appDatabase.OrderDao().getDailySales()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val texto = buildResumen(resultados) { r ->
                "${sdf.format(Date(r.businessDate))}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
        }
    }

    /** Calcula y muestra la ganancia SEMANAL (YYYY-WW). */
    private fun generarGananciaSemanal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados = appDatabase.OrderDao().getWeeklySales()
            val texto = buildResumen(resultados) { r ->
                "Semana ${r.week}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
        }
    }

    /** Calcula y muestra la ganancia MENSUAL (YYYY-MM). */
    private fun generarGananciaMensual() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados = appDatabase.OrderDao().getMonthlySales()
            val texto = buildResumen(resultados) { r ->
                "Mes ${r.month}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
            }
            withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
        }
    }

    /** Carga todas las órdenes (llamar en onCreate). */
    private fun cargarPedidos() = refreshOrders()

    /** Elimina una orden y refresca la lista. */
    private fun eliminarPedido(orderId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.OrderDao().deleteOrderById(orderId)
            withContext(Dispatchers.Main) {
                refreshOrders()
                Toast.makeText(this@MainActivity, "Pedido eliminado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Muestra un diálogo con el detalle de artículos de la orden. */
    private fun mostrarResumenPedido(order: OrderEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = appDatabase.OrderDao().getItemsForOrder(order.orderId)
            val detalle = buildString {
                items.forEach { it ->
                    val total = it.unitPrice * it.quantity
                    appendLine("${it.quantity} x ${it.name} ... ${total.formatMoney()}")
                }
                appendLine("------------------------------")
                appendLine("TOTAL: ${order.grandTotal.formatMoney()}")
            }
            withContext(Dispatchers.Main) {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Pedido ${order.orderId} - ${order.mesa ?: "Para llevar"}")
                    .setMessage(detalle)
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

// === FIN DEL BLOQUE CORREGIDO Y OPTIMIZADO ===

}
