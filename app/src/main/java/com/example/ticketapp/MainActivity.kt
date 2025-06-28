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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.text
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
import kotlin.io.path.outputStream
import java.nio.charset.StandardCharsets // Para codificar texto


class MainActivity : AppCompatActivity() {

// ... (dentro de tu clase MainActivity o en un archivo de utilidades)

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

    private companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.ticketapp.USB_PERMISSION"
        private const val PRINTER_NAME_BLUETOOTH = "BlueTooth Printer" // Or your printer's actual name
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    }

    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null

    private lateinit var inputQuesadillas: EditText
    private lateinit var inputPozoleGrande: EditText
    private lateinit var inputPozoleChico: EditText
    private lateinit var inputTostadas: EditText
    private lateinit var inputGuajolota: EditText
    private lateinit var inputGuajolotaExtra: EditText
    private lateinit var inputGuajoloyetNatural: EditText
    private lateinit var inputGujoloyetNaturalExtra: EditText
    private lateinit var inputGuajoloyetAdobado: EditText
    private lateinit var inputGujoloyetAdobadoExtra: EditText
    private lateinit var inputPambazosNaturales: EditText
    private lateinit var inputPambazosNaturalesExtra: EditText
    private lateinit var inputPambazosAdobados: EditText
    private lateinit var inputPambazosAdobadosExtra: EditText
    private lateinit var inputChalupas: EditText
    private lateinit var inputAlon: EditText
    private lateinit var inputMollejas: EditText
    private lateinit var inputHigados: EditText
    private lateinit var inputPatitas: EditText
    private lateinit var inputHuevo: EditText
    private lateinit var inputRefrescos: EditText
    private lateinit var inputCafe: EditText
    private lateinit var inputAguas: EditText
    private lateinit var inputAguasSabor: EditText
    private lateinit var inputAguasNat: EditText
    private lateinit var inputAguaTe: EditText






    private lateinit var btnImprimir: Button
    private lateinit var imgQR: ImageView

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private var bluetoothSocket: BluetoothSocket? = null

    // --- ActivityResultLaunchers for Permissions and Bluetooth ---
    private val requestBluetoothConnectPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "BLUETOOTH_CONNECT permission granted.")
                // You might want to trigger Bluetooth connection here if it was pending
                imprimirTicket()
            } else {
                Toast.makeText(this, "Permiso Bluetooth Connect denegado", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("InlinedApi") // For BLUETOOTH_SCAN if S+
    private val requestBluetoothScanPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "BLUETOOTH_SCAN permission granted.")
                // Potentially start discovery or find bonded devices again
            } else {
                Toast.makeText(this, "Permiso Bluetooth Scan denegado", Toast.LENGTH_LONG).show()
            }
        }

    private val requestBluetoothAdminPermissionLauncher = // For older Android versions
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "BLUETOOTH_ADMIN permission granted.")
            } else {
                Toast.makeText(this, "Permiso Bluetooth Admin denegado", Toast.LENGTH_LONG).show()
            }
        }


    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show()
                // Proceed with Bluetooth operations
            } else {
                Toast.makeText(this, "No se pudo activar Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }


    // --- USB Permission Receiver ---
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(USB_SERVICE) as UsbManager

        inputQuesadillas = findViewById(R.id.inputQuesadillas)
        inputPozoleGrande = findViewById(R.id.inputPozoleGrande)
        inputPozoleChico = findViewById(R.id.inputPozoleChico)
        inputTostadas = findViewById(R.id.inputTostadas)
        inputGuajolota = findViewById(R.id.inputGuajolota)
        inputGuajolotaExtra = findViewById(R.id.inputGuajolotaExtra)
        inputGuajoloyetNatural = findViewById(R.id.inputGuajoloyetNatural)
        inputGujoloyetNaturalExtra = findViewById(R.id.inputGujoloyetNaturalExtra)
        inputGuajoloyetAdobado = findViewById(R.id.inputGuajoloyetAdobado)
        inputGujoloyetAdobadoExtra = findViewById(R.id.inputGujoloyetAdobadoExtra)
        inputPambazosNaturales = findViewById(R.id.inputPambazosNaturales)
        inputPambazosNaturalesExtra = findViewById(R.id.inputPambazosNaturalesExtra)
        inputPambazosAdobados = findViewById(R.id.inputPambazosAdobados)
        inputPambazosAdobadosExtra = findViewById(R.id.inputPambazosAdobadosExtra)
        inputChalupas = findViewById(R.id.inputChalupas)
        inputAlon = findViewById(R.id.inputAlon)
        inputMollejas = findViewById(R.id.inputMollejas)
        inputHigados = findViewById(R.id.inputHigados)
        inputPatitas = findViewById(R.id.inputPatitas)
        inputHuevo = findViewById(R.id.inputHuevo)
        inputRefrescos = findViewById(R.id.inputRefrescos)
        inputCafe = findViewById(R.id.inputCafe)
        inputAguas = findViewById(R.id.inputAguas)
        inputAguasSabor = findViewById(R.id.inputAguasSabor)
        inputAguasNat = findViewById(R.id.inputAguasNat)
        inputAguaTe = findViewById(R.id.inputAguaTe)


        btnImprimir = findViewById(R.id.btnImprimir)

        imgQR = findViewById(R.id.imgQR)

        // Register USB permission broadcast receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") // Needed for older APIs
            registerReceiver(usbReceiver, filter)
        }


        btnImprimir.setOnClickListener {
            imprimirTicket()
        }

        // Initial setup
        checkAndRequestBluetoothPermissions()
        detectAndRequestUsbPermission()
    }

    private fun imprimirTicket() {
        val textoTicket = generarTextoTicket()
        //val bitmapQR = generarQR(textoTicket) // Generas el QR aquí
        //mostrarQR(bitmapQR) // Muestras en pantalla

        lifecycleScope.launch {
            // Intentar impresión USB
            val usbSuccess = printViaUsb(textoTicket) // Pasa el bitmapQR
            if (usbSuccess) {
                Toast.makeText(this@MainActivity, "Ticket enviado por USB", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Fallo al imprimir por USB. Intentando Bluetooth...", Toast.LENGTH_LONG).show()
                // Intentar impresión Bluetooth si USB falla
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val btSuccess = printViaBluetooth(textoTicket, null) // Pasa el bitmapQR
                    if (btSuccess) {
                        Toast.makeText(this@MainActivity, "Ticket enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Fallo al imprimir por Bluetooth", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Solicitar permiso si no está concedido
                    Toast.makeText(this@MainActivity, "Permiso Bluetooth Connect necesario", Toast.LENGTH_LONG).show()
                    checkAndRequestBluetoothPermissions() // O directamente el launcher
                }
            }
        }
    }


    // --- Bluetooth Methods ---

    private fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
            // BLUETOOTH_SCAN might be needed if you discover devices, not just connect to bonded ones.
            // if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // requestBluetoothScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            // }
        } else {
            // Older versions might need BLUETOOTH and BLUETOOTH_ADMIN for broader operations,
            // though connecting to bonded devices usually just needs BLUETOOTH.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                // For simplicity, using connect launcher, but ideally you'd have one for generic BLUETOOTH
                requestBluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothAdminPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Adaptador Bluetooth no disponible.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Adaptador Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        if (!bluetoothAdapter!!.isEnabled) {
            Log.i(TAG, "Bluetooth no activado. Solicitando activación.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Activando Bluetooth...", Toast.LENGTH_SHORT).show()
            }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return null // Wait for user to enable it, then try again
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
        val printerDevice = pairedDevices?.find { it.name.equals(PRINTER_NAME_BLUETOOTH, ignoreCase = true) }

        if (printerDevice == null) {
            Log.e(TAG, "Impresora Bluetooth '$PRINTER_NAME_BLUETOOTH' no encontrada entre los dispositivos emparejados.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Impresora no emparejada: $PRINTER_NAME_BLUETOOTH", Toast.LENGTH_LONG).show()
            }
            return null
        }

        Log.d(TAG, "Intentando conectar con la impresora Bluetooth: ${printerDevice.name} [${printerDevice.address}]")
        return try {
            val socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect() // This is a blocking call, hence the suspend function and Dispatchers.IO context
            Log.d(TAG, "Conexión Bluetooth establecida con ${printerDevice.name}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Conectado a ${printerDevice.name}", Toast.LENGTH_SHORT).show()
            }
            socket
        } catch (e: IOException) {
            Log.e(TAG, "Error al conectar por Bluetooth con ${printerDevice.name}: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error de conexión Bluetooth", Toast.LENGTH_SHORT).show()
            }
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e(TAG, "Error al cerrar socket Bluetooth: ${closeException.message}", closeException)
            }
            null
        }
    }
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    private suspend fun printViaBluetooth(textoTicket: String, qrBitmap: Bitmap? = null): Boolean {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            socket = connectToBluetoothPrinter()
            if (socket == null || !socket.isConnected) {
                Log.e(TAG, "No se pudo conectar a la impresora Bluetooth")
                return false
            }
            outputStream = socket.outputStream

            val initPrinter = byteArrayOf(0x1B, 0x40) // Initialize printer
            outputStream.write(initPrinter)

            val ticketBytes = textoTicket.toByteArray(Charset.forName("GB18030"))
            outputStream.write(ticketBytes)
            outputStream.flush()

            qrBitmap?.let {
                val qrData = bitmapToEscPosData(it)
                outputStream.write(qrData)
                outputStream.flush()
            }

            val feedLines = "\n\n\n".toByteArray(Charset.forName("GB18030"))
            outputStream.write(feedLines)
            outputStream.flush()

            Log.d(TAG, "Ticket enviado por Bluetooth")
            return true

            }catch (e: IOException) {
                Log.e(TAG, "Error al enviar por Bluetooth: ${e.message}", e)
                return false
            } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al enviar por Bluetooth: ${e.message}", e)
            return false
            } catch (e: Exception) {
                    Log.e(TAG, "Error al enviar por Bluetooth: ${e.message}", e)
                    return false
        } finally {
            try {
                outputStream?.close()
                socket?.close()
                bluetoothSocket = null
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar streams: ${e.message}", e)
            }
        }


    }


    // --- USB Methods ---
    private fun detectAndRequestUsbPermission() {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos USB conectados", Toast.LENGTH_SHORT).show()
            return
        }
        // Simple approach: take the first device. For multiple printers, you'd need more logic.
        val deviceToUse = deviceList.values.firstOrNull {
            // Add more specific checks if needed, e.g., vendor ID, product ID, or interface class
            // For now, let's assume any USB device could be the printer.
            true
        }

        if (deviceToUse == null) {
            Toast.makeText(this, "No se encontró impresora USB compatible", Toast.LENGTH_SHORT).show()
            return
        }

        if (usbManager.hasPermission(deviceToUse)) {
            Log.d(TAG, "Permiso USB ya concedido para: ${deviceToUse.deviceName}")
            setupUsbDevice(deviceToUse)
        } else {
            Log.d(TAG, "Solicitando permiso USB para: ${deviceToUse.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Added FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(deviceToUse, permissionIntent)
        }
    }

    private fun setupUsbDevice(device: UsbDevice) {
        releaseUsbDevice() // Release any previous device

        usbDeviceConnection = usbManager.openDevice(device)
        if (usbDeviceConnection == null) {
            Toast.makeText(this, "No se pudo abrir la conexión USB con ${device.deviceName}", Toast.LENGTH_SHORT).show()
            return
        }

        // Typically, printer interface is the first one.
        // You might need more specific logic to find the correct interface.
        usbInterface = device.getInterface(0)
        if (!usbDeviceConnection!!.claimInterface(usbInterface, true)) {
            Toast.makeText(this, "No se pudo reclamar la interfaz USB", Toast.LENGTH_SHORT).show()
            releaseUsbDevice()
            return
        }

        // Find the OUT endpoint
        for (i in 0 until usbInterface!!.endpointCount) {
            val endpoint = usbInterface!!.getEndpoint(i)
            if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                endpoint.direction == UsbConstants.USB_DIR_OUT
            ) {
                usbEndpointOut = endpoint
                break
            }
        }

        if (usbEndpointOut == null) {
            Toast.makeText(this, "No se encontró endpoint de salida USB", Toast.LENGTH_SHORT).show()
            releaseUsbDevice()
            return
        }
        this.usbDevice = device // Store the successfully setup device
        Toast.makeText(this, "Impresora USB lista: ${device.deviceName}", Toast.LENGTH_SHORT).show()
    }


    private suspend fun printViaUsb(data: String): Boolean {
        if (usbDeviceConnection == null || usbInterface == null || usbEndpointOut == null) {
            Log.w(TAG, "Dispositivo USB no configurado correctamente.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Impresora USB no lista", Toast.LENGTH_SHORT).show()
            }
            detectAndRequestUsbPermission() // Try to re-detect
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val bytes = data.toByteArray(Charset.forName("GB18030"))
                // usbDeviceConnection and usbEndpointOut are asserted non-null due to the check above
                val sentBytes = usbDeviceConnection!!.bulkTransfer(usbEndpointOut!!, bytes, bytes.size, 5000) // 5s timeout

                if (sentBytes >= 0) {
                    Log.d(TAG, "Datos ($sentBytes bytes) enviados por USB.")
                    true
                } else {
                    Log.e(TAG, "Error al enviar datos por USB, sentBytes: $sentBytes")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error de transmisión USB", Toast.LENGTH_SHORT).show()
                    }
                    releaseUsbDevice() // Release on error to force re-setup
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


    // --- Ticket Generation and QR ---
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

    data class Producto(
        val nombre: String,
        val precio: Double,
        val cantidad: Int
    ) {
        val total: Double get() = precio * cantidad
    }

    private fun obtenerProductosDesdeInputs(): List<Producto> {
        val productos = mutableListOf<Producto>()
        try {
            val qtyQuesadillas = inputQuesadillas.text.toString().toIntOrNull() ?: 0
            if (qtyQuesadillas > 0) productos.add(Producto("Quesadilla(s)", 28.0, qtyQuesadillas))

            val qtyPozoleGrande = inputPozoleGrande.text.toString().toIntOrNull() ?: 0
            if (qtyPozoleGrande > 0) productos.add(Producto("Pozole(s) Grande", 110.0, qtyPozoleGrande))

            val qtyPozoleChico = inputPozoleChico.text.toString().toIntOrNull() ?: 0
            if (qtyPozoleChico > 0) productos.add(Producto("Pozole(s) Chico", 90.0, qtyPozoleChico))

            val qtyTostadas = inputTostadas.text.toString().toIntOrNull() ?: 0
            if (qtyTostadas > 0) productos.add(Producto("Tostada(s)", 32.0, qtyTostadas))

            val qtyGuajolota = inputGuajolota.text.toString().toIntOrNull() ?: 0
            if (qtyGuajolota > 0) productos.add(Producto("Guajolota(s) Volcan", 55.0, qtyGuajolota))

            val qtyGuajolotaExtra = inputGuajolotaExtra.text.toString().toIntOrNull() ?: 0
            if (qtyGuajolotaExtra > 0) productos.add(Producto("Guajolota(s) Volcan Queso/Guisado Extra", 65.0, qtyGuajolotaExtra))

            val qtyGuajoloyetNatural = inputGuajoloyetNatural.text.toString().toIntOrNull() ?: 0
            if (qtyGuajoloyetNatural > 0) productos.add(Producto("Guajoloyet(s) Naturales", 55.0, qtyGuajoloyetNatural))

            val qtyGuajoloyetNaturalExtra = inputGujoloyetNaturalExtra.text.toString().toIntOrNull() ?: 0
            if (qtyGuajoloyetNaturalExtra > 0) productos.add(Producto("Guajoloyet(s) Naturales Queso/Guisado Extra", 65.0, qtyGuajoloyetNaturalExtra))

            val qtyGuajoloyetAdobado = inputGuajoloyetAdobado.text.toString().toIntOrNull() ?: 0
            if (qtyGuajoloyetAdobado > 0) productos.add(Producto("Guajoloyet(s) Adobado", 60.0, qtyGuajoloyetAdobado))

            val qtyGuajoloyetAdobadoExtra = inputGujoloyetAdobadoExtra.text.toString().toIntOrNull() ?: 0
            if (qtyGuajoloyetAdobadoExtra > 0) productos.add(Producto("Guajoloyet(s) Adobados Queso/Guisado Extra", 70.0, qtyGuajoloyetAdobadoExtra))

            val qtyPambazosNaturales = inputPambazosNaturales.text.toString().toIntOrNull() ?: 0
            if (qtyPambazosNaturales > 0) productos.add(Producto("Pambazo(s) Naturales", 34.0, qtyPambazosNaturales))

            val qtyPambazosNaturalesExtra = inputPambazosNaturalesExtra.text.toString().toIntOrNull() ?: 0
            if (qtyPambazosNaturalesExtra > 0) productos.add(Producto("Pambazos Naturales Queso Extra", 44.0, qtyPambazosNaturalesExtra))

            val qtyPambazosAdobados = inputPambazosAdobados.text.toString().toIntOrNull() ?: 0
            if (qtyPambazosAdobados > 0) productos.add(Producto("Pambazos Adobados", 39.0, qtyPambazosAdobados))

            val qtyPambazosAdobadosExtra = inputPambazosAdobadosExtra.text.toString().toIntOrNull() ?: 0
            if (qtyPambazosAdobadosExtra > 0) productos.add(Producto("Pambazos Adobados Queso Extra", 49.0, qtyPambazosAdobadosExtra))

            val qtyChalupas = inputChalupas.text.toString().toIntOrNull() ?: 0
            if (qtyChalupas > 0) productos.add(Producto("Chalupa(s)", 5.0, qtyChalupas))

            val qtyAlon = inputAlon.text.toString().toIntOrNull() ?: 0
            if (qtyAlon > 0) productos.add(Producto("Alon(es)", 25.0, qtyAlon))

            val qtyMollejas = inputMollejas.text.toString().toIntOrNull() ?: 0
            if (qtyMollejas > 0) productos.add(Producto("Orden(es) de Mollejas", 25.0, qtyMollejas))

            val qtyHigados = inputHigados.text.toString().toIntOrNull() ?: 0
            if (qtyHigados > 0) productos.add(Producto("Orden(es) de Higados", 22.0, qtyHigados))

            val qtyPatitas = inputPatitas.text.toString().toIntOrNull() ?: 0
            if (qtyPatitas > 0) productos.add(Producto("Orden(es) de Patitas", 22.0, qtyPatitas))

            val qtyHuevo = inputHuevo.text.toString().toIntOrNull() ?: 0
            if (qtyHuevo > 0) productos.add(Producto("Piezas de Huevo", 20.0, qtyHuevo))

            val qtyRefrescos = inputRefrescos.text.toString().toIntOrNull() ?: 0
            if (qtyRefrescos > 0) productos.add(Producto("Refresco(s)", 25.0, qtyRefrescos))

            val qtyCafe = inputCafe.text.toString().toIntOrNull() ?: 0
            if (qtyCafe > 0) productos.add(Producto("Café", 22.0, qtyCafe))

            val qtyAguas = inputAguas.text.toString().toIntOrNull() ?: 0
            if (qtyAguas > 0) productos.add(Producto("Agua(s) de Jamaica/Horchata", 24.0, qtyAguas))

            val qtyAguasSabor = inputAguasSabor.text.toString().toIntOrNull() ?: 0
            if (qtyAguasSabor > 0) productos.add(Producto("Aguas de Sabor", 25.0, qtyAguasSabor))

            val qtyAguasNat = inputAguasNat.text.toString().toIntOrNull() ?: 0
            if (qtyAguasNat > 0) productos.add(Producto("Agua(s) Naturales", 20.0, qtyAguasNat))

            val qtyAguaTe = inputAguaTe.text.toString().toIntOrNull() ?: 0
            if (qtyAguaTe > 0) productos.add(Producto("Agua(s) Para Té", 20.0, qtyAguaTe))













        } catch (e: NumberFormatException) {
            Log.e(TAG, "Error al parsear cantidad de productos", e)
            Toast.makeText(this, "Por favor ingrese números válidos para las cantidades.", Toast.LENGTH_LONG).show()
        }
        return productos
    }

    private fun generarTextoTicket(): String {
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        // Ajusta el ancho de línea si es necesario. Este ejemplo asume unos 32-35 caracteres.
        val anchoTotalLinea = 32

        sb.appendLine("   ANTOJITOS MEXICANOS MARGARITA")
        sb.appendLine("*********************************") // O usa el anchoTotalLinea
        sb.appendLine("     *** TICKET DE COMPRA ***")
        sb.appendLine("*********************************")
        sb.appendLine("Fecha y hora: $fechaHora")

        // Generar línea de guiones del ancho deseado
        val lineaSeparadoraCorta = "-".repeat(anchoTotalLinea)
        sb.appendLine(lineaSeparadoraCorta)

        // Encabezado de la tabla con separadores
        // Anchos: Producto (13), Precio (6), Cant (3), Total (7) -> Suma 29 + 3 separadores = 32
        // Si necesitas más espacio para Producto, reduce de Precio o Total.
        // Ejemplo: Prod(13) Precio(6) Cant(3) Total(7)
        sb.appendLine(String.format("%-13s|%6s|%3s|%7s", "Pdto", "Pcio", "Can", "Total")) // "Can" para ahorrar espacio
        sb.appendLine(lineaSeparadoraCorta)

        var totalGeneral = 0.0
        val productos = obtenerProductosDesdeInputs()

        if (productos.isEmpty()) {
            sb.appendLine("      NINGUN PRODUCTO")
            sb.appendLine("       SELECCIONADO")
            sb.appendLine(lineaSeparadoraCorta)
        } else {
            for (p in productos) {
                totalGeneral += p.total
                // Trunca el nombre del producto si es más largo que el espacio asignado (13 caracteres)
                val nombreProducto = if (p.nombre.length > 13) p.nombre.substring(0, 10) + "..." else p.nombre

                // Formatea precio y total con el $ pegado
                val precioConSimbolo = String.format("$%.2f", p.precio)
                val totalConSimbolo = String.format("$%.2f", p.total)

                // Formatea la línea del producto con separadores
                // Anchos: Producto(%-13s), Precio(%6s), Cant(%3d), Total(%7s)
                sb.appendLine(
                    String.format(
                        "%-13s|%6s|%3d|%7s",
                        nombreProducto,
                        precioConSimbolo,
                        p.cantidad,
                        totalConSimbolo
                    )
                )
            }
        }

        sb.appendLine(lineaSeparadoraCorta)

        // Total General: Asegurarse de que el $ esté pegado
        val totalGeneralConSimbolo = String.format("$%.2f", totalGeneral)
        // Para alinear "TOTAL:" a la izquierda y el monto a la derecha de la línea:
        // "TOTAL: " ocupa 7 caracteres. Si la línea es de 32, quedan 32-7 = 25 para el monto.
        sb.appendLine(String.format("%-7s%25s", "TOTAL:", totalGeneralConSimbolo)) // Ajusta el 25 según anchoTotalLinea

        sb.appendLine()
        sb.appendLine("    Gracias por su compra")
        sb.appendLine("    Vuelva pronto")
        sb.appendLine("\n\n\n")

        return sb.toString()
    }

    // --- Lifecycle Methods ---
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        releaseUsbDevice()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar Bluetooth socket en onDestroy: ${e.message}", e)
        }
    }
}