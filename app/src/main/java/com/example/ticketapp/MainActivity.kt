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
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    // --- Constantes y variables de la clase ---
    private companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.ticketapp.USB_PERMISSION"
        private const val PRINTER_NAME_BLUETOOTH = "BlueTooth Printer" // Nombre de tu impresora
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar de SPP
    }

    // Vistas
    private val products = mutableMapOf<String, ProductData>()
    private lateinit var btnImprimir: Button
    private lateinit var btnEmparejar: Button
    private lateinit var btnLimpiar: Button // <<-- Nuevo botón
    private lateinit var imgQR: ImageView

    // Gestión de USB
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null

    // Gestión de Bluetooth
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private var bluetoothSocket: BluetoothSocket? = null

    // --- ActivityResultLaunchers para Permisos y Bluetooth ---
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Log.d(TAG, "Permisos Bluetooth concedidos.")
                Toast.makeText(this, "Permisos Bluetooth concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Permisos Bluetooth denegados.")
                Toast.makeText(this, "Se requieren permisos de Bluetooth para imprimir", Toast.LENGTH_LONG).show()
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

    // --- Receptor de permisos USB ---
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

    // --- Ciclo de vida de la actividad ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(USB_SERVICE) as UsbManager

        setupProductViews()
        setupButtons()

        // Registrar el BroadcastReceiver para los permisos USB
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbReceiver, filter)
        }

        // Configuración inicial de permisos
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
        // Mapea los productos con sus IDs de TextView, Button y precio
        products["Quesadillas"] = ProductData(findViewById(R.id.cantidadQuesadillas), findViewById(R.id.btnMenosQuesadillas), findViewById(R.id.btnMasQuesadillas), 28.0)
        products["Pozole Grande"] = ProductData(findViewById(R.id.cantidadPozoleGrande), findViewById(R.id.btnMenosPozoleGrande), findViewById(R.id.btnMasPozoleGrande), 110.0)
        products["Pozole Chico"] = ProductData(findViewById(R.id.cantidadPozoleChico), findViewById(R.id.btnMenosPozoleChico), findViewById(R.id.btnMasPozoleChico), 90.0)
        products["Tostadas"] = ProductData(findViewById(R.id.cantidadTostadas), findViewById(R.id.btnMenosTostadas), findViewById(R.id.btnMasTostadas), 32.0)
        products["Volcanes"] = ProductData(findViewById(R.id.cantidadVolcanes), findViewById(R.id.btnMenosVolcanes), findViewById(R.id.btnMasVolcanes), 55.0)
        products["Volcan Queso/Guisado Extra"] = ProductData(findViewById(R.id.cantidadGuajolotaExtra), findViewById(R.id.btnMenosGuajolotaExtra), findViewById(R.id.btnMasGuajolotaExtra), 65.0)
        products["Guajoloyets Naturales"] = ProductData(findViewById(R.id.cantidadGuajoloyetNatural), findViewById(R.id.btnMenosGuajoloyetNatural), findViewById(R.id.btnMasGuajoloyetNatural), 55.0)
        products["Guajoloyets Naturales Extra"] = ProductData(findViewById(R.id.cantidadGujoloyetNaturalExtra), findViewById(R.id.btnMenosGujoloyetNaturalExtra), findViewById(R.id.btnMasGujoloyetNaturalExtra), 65.0)
        products["Guajoloyets Adobados"] = ProductData(findViewById(R.id.cantidadGuajoloyetAdobado), findViewById(R.id.btnMenosGuajoloyetAdobado), findViewById(R.id.btnMasGuajoloyetAdobado), 60.0)
        products["Guajoloyets Adobados Extra"] = ProductData(findViewById(R.id.cantidadGujoloyetAdobadoExtra), findViewById(R.id.btnMenosGujoloyetAdobadoExtra), findViewById(R.id.btnMasGujoloyetAdobadoExtra), 70.0)
        products["Pambazos Naturales"] = ProductData(findViewById(R.id.cantidadPambazosNaturales), findViewById(R.id.btnMenosPambazosNaturales), findViewById(R.id.btnMasPambazosNaturales), 34.0)
        products["Pambazos Naturales Combinados"] = ProductData(findViewById(R.id.cantidadPambazosNaturalesCombinados), findViewById(R.id.btnMenosPambazosNaturalesCombinados), findViewById(R.id.btnMasPambazosNaturalesCombinados), 41.0)
        products["Pambazos Naturales Combinados con Queso"] = ProductData(findViewById(R.id.cantidadPambazosNaturalesCombinadosQueso), findViewById(R.id.btnMenosPambazosNaturalesCombinadosQueso), findViewById(R.id.btnMasPambazosNaturalesCombinadosQueso), 46.0)
        products["Pambazos Naturales Extra"] = ProductData(findViewById(R.id.cantidadPambazosNaturalesExtra), findViewById(R.id.btnMenosPambazosNaturalesExtra), findViewById(R.id.btnMasPambazosNaturalesExtra), 44.0)
        products["Pambazos Adobados"] = ProductData(findViewById(R.id.cantidadPambazosAdobados), findViewById(R.id.btnMenosPambazosAdobados), findViewById(R.id.btnMasPambazosAdobados), 39.0)
        products["Pambazos Adobados Combinados"] = ProductData(findViewById(R.id.cantidadPambazosAdobadosCombinados), findViewById(R.id.btnMenosPambazosAdobadosCombinados), findViewById(R.id.btnMasPambazosAdobadosCombinados), 44.0)
        products["Pambazos Adobados Combinados con Queso"] = ProductData(findViewById(R.id.cantidadPambazosAdobadosCombinadosQueso), findViewById(R.id.btnMenosPambazosAdobadosCombinadosQueso), findViewById(R.id.btnMasPambazosAdobadosCombinadosQueso), 49.0)
        products["Pambazos Adobados Extra"] = ProductData(findViewById(R.id.cantidadPambazosAdobadosExtra), findViewById(R.id.btnMenosPambazosAdobadosExtra), findViewById(R.id.btnMasPambazosAdobadosExtra), 49.0)
        products["Chalupas"] = ProductData(findViewById(R.id.cantidadChalupas), findViewById(R.id.btnMenosChalupas), findViewById(R.id.btnMasChalupas), 5.0)
        products["Alones"] = ProductData(findViewById(R.id.cantidadAlones), findViewById(R.id.btnMenosAlones), findViewById(R.id.btnMasAlones), 25.0)
        products["Mollejas"] = ProductData(findViewById(R.id.cantidadMollejas), findViewById(R.id.btnMenosMollejas), findViewById(R.id.btnMasMollejas), 25.0)
        products["Higados"] = ProductData(findViewById(R.id.cantidadHigados), findViewById(R.id.btnMenosHigados), findViewById(R.id.btnMasHigados), 22.0)
        products["Patitas"] = ProductData(findViewById(R.id.cantidadPatitas), findViewById(R.id.btnMenosPatitas), findViewById(R.id.btnMasPatitas), 22.0)
        products["Huevos"] = ProductData(findViewById(R.id.cantidadHuevos), findViewById(R.id.btnMenosHuevos), findViewById(R.id.btnMasHuevos), 20.0)
        products["Refrescos"] = ProductData(findViewById(R.id.cantidadRefrescos), findViewById(R.id.btnMenosRefrescos), findViewById(R.id.btnMasRefrescos), 25.0)
        products["Cafe"] = ProductData(findViewById(R.id.cantidadCafe), findViewById(R.id.btnMenosCafe), findViewById(R.id.btnMasCafe), 22.0)
        products["Agua Jamaica/Horchata"] = ProductData(findViewById(R.id.cantidadAguas), findViewById(R.id.btnMenosAguas), findViewById(R.id.btnMasAguas), 24.0)
        products["Aguas de Sabor"] = ProductData(findViewById(R.id.cantidadAguasSabor), findViewById(R.id.btnMenosAguasSabor), findViewById(R.id.btnMasAguasSabor), 25.0)
        products["Agua Natural"] = ProductData(findViewById(R.id.cantidadAguasNat), findViewById(R.id.btnMenosAguasNat), findViewById(R.id.btnMasAguasNat), 20.0)
        products["Agua para Te"] = ProductData(findViewById(R.id.cantidadAguaTe), findViewById(R.id.btnMenosAguaTe), findViewById(R.id.btnMasAguaTe), 20.0)
        products["Extra +5"] = ProductData(findViewById(R.id.cantidadExtra5), findViewById(R.id.btnMenosExtra5), findViewById(R.id.btnMasExtra5), 5.0)
        products["Extra +10"] = ProductData(findViewById(R.id.cantidadExtra10), findViewById(R.id.btnMenosExtra10), findViewById(R.id.btnMasExtra10), 10.0)
        products["Postres 25"] = ProductData(findViewById(R.id.cantidadPostres25), findViewById(R.id.btnMenosPostres25), findViewById(R.id.btnMasPostres25), 25.0)
        products["Postres 30"] = ProductData(findViewById(R.id.cantidadPostres30), findViewById(R.id.btnMenosPostres30), findViewById(R.id.btnMasPostres30), 30.0)
        products["Postres 35"] = ProductData(findViewById(R.id.cantidadPostres35), findViewById(R.id.btnMenosPostres35), findViewById(R.id.btnMasPostres35), 35.0)

        // Configura los listeners para los botones +/- de cada producto
        products.values.forEach { productData ->
            productData.btnMas.setOnClickListener {
                updateQuantity(productData.cantidadTV, 1)
            }
            productData.btnMenos.setOnClickListener {
                updateQuantity(productData.cantidadTV, -1)
            }
        }
    }

    private fun setupButtons() {
        btnImprimir = findViewById(R.id.btnImprimir)
        btnEmparejar = findViewById(R.id.btnEmparejar)
        btnLimpiar = findViewById(R.id.btnLimpiar) // <<-- Inicialización del nuevo botón
        imgQR = findViewById(R.id.imgQR)

        btnImprimir.setOnClickListener {
            imprimirTicket()
        }

        btnEmparejar.setOnClickListener {
            // Abre la configuración de Bluetooth para que el usuario pueda emparejar
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se pudo abrir la configuración de Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

        // <<-- Lógica del botón Limpiar Cantidades
        btnLimpiar.setOnClickListener {
            limpiarCantidades()
        }
    }

    /**
     * Actualiza la cantidad de un producto en la UI.
     * @param textView El TextView que muestra la cantidad.
     * @param change El cambio de cantidad (+1 o -1).
     */
    private fun updateQuantity(textView: TextView, change: Int) {
        val currentQuantity = textView.text.toString().toIntOrNull() ?: 0
        val newQuantity = (currentQuantity + change).coerceAtLeast(0) // No permitir números negativos
        textView.text = newQuantity.toString()
    }

    /**
     * Restablece todas las cantidades de los productos a cero en la interfaz de usuario.
     */
    private fun limpiarCantidades() {
        products.values.forEach { productData ->
            productData.cantidadTV.text = "0"
        }
        Toast.makeText(this, "Cantidades restablecidas a 0", Toast.LENGTH_SHORT).show()
        // Opcional: También podrías limpiar la imagen del QR si la tienes visible
        imgQR.setImageDrawable(null)
    }

    // --- Lógica de Impresión ---
    private fun imprimirTicket() {
        val textoTicket = generarTextoTicket()
        val bitmapQR = generarQR(textoTicket)
        mostrarQR(bitmapQR)

        // Ejecutar la impresión en un coroutine en el hilo de IO
        lifecycleScope.launch {
            // Intentar impresión USB primero
            val usbSuccess = printViaUsb(textoTicket)
            if (usbSuccess) {
                Toast.makeText(this@MainActivity, "Ticket enviado por USB", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "Fallo al imprimir por USB. Intentando Bluetooth...")
                // Si USB falla, intentar Bluetooth
                if (checkBluetoothPermissions()) {
                    val btSuccess = printViaBluetooth(textoTicket)
                    if (btSuccess) {
                        Toast.makeText(this@MainActivity, "Ticket enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Fallo al imprimir por Bluetooth", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Permisos de Bluetooth no concedidos.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Métodos de Bluetooth ---

    /**
     * Verifica y solicita los permisos de Bluetooth necesarios para la versión de Android.
     */
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

    /**
     * Verifica si se tienen los permisos de Bluetooth en tiempo de ejecución.
     */
    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Intenta conectar con la impresora Bluetooth.
     * Es una función suspendida para ser llamada desde una coroutine.
     * @return El socket Bluetooth conectado o null si falla.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Adaptador Bluetooth no disponible.")
            withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Adaptador Bluetooth no disponible", Toast.LENGTH_SHORT).show() }
            return@withContext null
        }
        if (!bluetoothAdapter!!.isEnabled) {
            Log.i(TAG, "Bluetooth no activado. Solicitando activación.")
            withContext(Dispatchers.Main) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
                Toast.makeText(this@MainActivity, "Activando Bluetooth...", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }

        // Buscar dispositivo emparejado
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
        val printerDevice = pairedDevices?.find { it.name.equals(PRINTER_NAME_BLUETOOTH, ignoreCase = true) }

        if (printerDevice == null) {
            Log.e(TAG, "Impresora Bluetooth '$PRINTER_NAME_BLUETOOTH' no encontrada entre los dispositivos emparejados.")
            withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Impresora no emparejada: $PRINTER_NAME_BLUETOOTH", Toast.LENGTH_LONG).show() }
            return@withContext null
        }

        Log.d(TAG, "Intentando conectar con la impresora Bluetooth: ${printerDevice.name} [${printerDevice.address}]")
        return@withContext try {
            val socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothAdapter?.cancelDiscovery()
            socket.connect()
            Log.d(TAG, "Conexión Bluetooth establecida con ${printerDevice.name}")
            withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Conectado a ${printerDevice.name}", Toast.LENGTH_SHORT).show() }
            socket
        } catch (e: IOException) {
            Log.e(TAG, "Error al conectar por Bluetooth con ${printerDevice.name}: ${e.message}", e)
            try { bluetoothSocket?.close() } catch (closeException: IOException) {}
            null
        }
    }

    /**
     * Imprime datos a través de la conexión Bluetooth.
     * @return true si la impresión fue exitosa, false en caso contrario.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun printViaBluetooth(textoTicket: String): Boolean = withContext(Dispatchers.IO) {
        var success = false
        try {
            bluetoothSocket = connectToBluetoothPrinter()
            val socket = bluetoothSocket
            if (socket == null || !socket.isConnected) {
                Log.e(TAG, "No se pudo conectar a la impresora Bluetooth")
                return@withContext false
            }

            val outputStream: OutputStream = socket.outputStream
            val initPrinter = byteArrayOf(0x1B, 0x40) // Inicializar impresora
            val cutPaper = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Cortar papel

            // Enviar texto
            val ticketBytes = textoTicket.toByteArray(Charset.forName("GB18030"))
            outputStream.write(initPrinter)
            outputStream.write(ticketBytes)
            outputStream.write(cutPaper)
            outputStream.flush()
            success = true

        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al imprimir por Bluetooth: ${e.message}", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad (permisos) al imprimir por Bluetooth: ${e.message}", e)
        } finally {
            // No cierres el socket aquí si planeas reutilizarlo.
            // Para una conexión efímera por ticket, ciérralo.
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

    // --- Métodos de USB ---
    private fun detectAndRequestUsbPermission() {
        // Busca impresoras USB por ID de Vendor y Product si los conoces
        val deviceList = usbManager.deviceList
        val printerDevice = deviceList.values.firstOrNull {
            // Ejemplo de IDs para una impresora ESC/POS genérica
            // Reemplaza con los IDs de tu impresora si los conoces
            it.vendorId == 1155 && it.productId == 22339 // Ejemplo: 04B8:0E23 para Epson
        } ?: deviceList.values.firstOrNull() // Si no se encuentra, toma el primer dispositivo disponible

        if (printerDevice == null) {
            Toast.makeText(this, "No se encontró impresora USB compatible", Toast.LENGTH_SHORT).show()
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

        // Busca la interfaz y el endpoint correctos
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
            Toast.makeText(this, "No se encontró interfaz de impresora USB", Toast.LENGTH_SHORT).show()
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
            val sentBytes = usbDeviceConnection!!.bulkTransfer(usbEndpointOut!!, bytes, bytes.size, 5000)

            if (sentBytes >= 0) {
                Log.d(TAG, "Datos ($sentBytes bytes) enviados por USB.")
                true
            } else {
                Log.e(TAG, "Error al enviar datos por USB, sentBytes: $sentBytes")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error de transmisión USB", Toast.LENGTH_SHORT).show()
                }
                releaseUsbDevice()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al imprimir por USB: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Excepción de USB: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // --- Generación de Ticket y QR ---
    private fun mostrarQR(bitmap: Bitmap) {
        imgQR.setImageBitmap(bitmap)
    }

    private fun generarQR(texto: String): Bitmap {
        val writer = QRCodeWriter()
        val size = 300 // pixels
        val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Estructura de datos para un producto en la UI.
     */
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
        val productos = mutableListOf<Producto>()
        products.forEach { (nombre, data) ->
            val cantidad = data.cantidadTV.text.toString().toIntOrNull() ?: 0
            if (cantidad > 0) {
                productos.add(Producto(nombre, data.precio, cantidad))
            }
        }
        return productos
    }

    private fun generarTextoTicket(): String {
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        val anchoTotalLinea = 32 // Ancho de caracteres de la impresora

        sb.appendLine("   ANTOJITOS MEXICANOS MARGARITA")
        sb.appendLine("********************************") // 32 asteriscos
        sb.appendLine("     *** TICKET DE COMPRA ***")
        sb.appendLine("********************************")
        sb.appendLine("Fecha y hora: $fechaHora")

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
        sb.appendLine("\n\n\n") // Saltos de línea para empujar el ticket hacia arriba
        sb.append(byteArrayOf(0x1D, 0x56, 0x42, 0x01).toString(StandardCharsets.ISO_8859_1)) // Comando de corte

        return sb.toString()
    }

    /**
     * Convierte un Bitmap a un array de bytes para imprimir en una impresora ESC/POS.
     * Utiliza un enfoque común para imprimir gráficos.
     * ¡Ajusta el umbral y el algoritmo según sea necesario para tu impresora!
     */
    private fun bitmapToEscPosData(bitmap: Bitmap, printerDotsPerLine: Int = 384): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()

        // 1. Redimensionar si es necesario y asegurar que el ancho sea múltiplo de 8
        //    Para este ejemplo, asumimos que el bitmap ya tiene un tamaño adecuado
        //    o que la impresora manejará el recorte/escalado.
        //    Un bitmap más ancho que printerDotsPerLine será recortado por muchas impresoras.
        var scaledBitmap = bitmap
        if (bitmap.width > printerDotsPerLine) {
            val newHeight = (bitmap.height.toFloat() * (printerDotsPerLine.toFloat() / bitmap.width.toFloat())).toInt()
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, printerDotsPerLine, newHeight, true)
        }

        val width = scaledBitmap.width
        val height = scaledBitmap.height

        // 2. Convertir a monocromático y preparar datos de píxeles
        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = 128 // Umbral para decidir si un píxel es blanco o negro

        // ESC/POS comúnmente procesa la imagen en "columnas" de 8 píxeles de alto.
        // O en "rodajas" horizontales de 8 píxeles de ancho (o 24).
        // Este ejemplo toma un enfoque más simple, enviando datos de píxeles para el comando GS v 0
        // que espera datos por puntos.

        // Comando GS v 0 pL pH d1...dk
        // pL = (widthBytes % 256)
        // pH = (widthBytes / 256)
        // widthBytes = (width + 7) / 8  (bytes por fila de la imagen)

        val widthBytes = (width + 7) / 8
        val pL = widthBytes % 256
        val pH = widthBytes / 256

        // Iniciar modo de imagen de bits (GS v 0 m xL xH yL yH d1...dk)
        // m = 0 (modo normal)
        // xL, xH = ancho en puntos
        // yL, yH = alto en puntos
        outputStream.write(0x1D) // GS
        outputStream.write(0x76) // v
        outputStream.write(0x30) // 0 (comando para imprimir imagen de bits)
        outputStream.write(0x00) // m = 0 (modo normal)
        outputStream.write(pL)   // xL
        outputStream.write(pH)   // xH
        outputStream.write(height % 256) // yL
        outputStream.write(height / 256) // yH

        // Convertir píxeles a datos de bytes
        for (y in 0 until height) {
            var byteVal = 0
            for (x in 0 until width) {
                val pixelColor = pixels[y * width + x]
                val r = Color.red(pixelColor)
                val g = Color.green(pixelColor)
                val b = Color.blue(pixelColor)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                if (luminance < threshold) { // Píxel negro
                    byteVal = byteVal or (1 shl (7 - (x % 8)))
                }

                if (x % 8 == 7 || x == width - 1) { // Byte completo o fin de fila
                    outputStream.write(byteVal)
                    byteVal = 0 // Reset para el siguiente byte
                }
            }
        }
        return outputStream.toByteArray()
    }
}