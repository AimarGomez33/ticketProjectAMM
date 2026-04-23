package com.example.ticketapp

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Tipos de Impresora
// ─────────────────────────────────────────────────────────────────────────────

enum class PrinterType {
    BLUETOOTH_58MM,
    NETWORK_80MM,
    NETWORK_58MM
}

// ─────────────────────────────────────────────────────────────────────────────
// Data class que representa el resultado de una impresión
// ─────────────────────────────────────────────────────────────────────────────

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

/**
 * Administrador de Impresoras Térmicas.
 * Toda la lógica de hardware (Bluetooth, Socket TCP) y formato ESC/POS
 * vive aquí, aislada del Activity para evitar bloqueos de UI y crashes
 * en caso de error de hardware.
 *
 * @param context  Application context (para acceder a recursos, BT Manager, etc.)
 */
class PrinterManager(private val context: Context) {

    // Estado
    var selectedPrinterType: PrinterType = PrinterType.BLUETOOTH_58MM

    // Canal de notificaciones a la UI
    private val _printState = MutableStateFlow<PrintResult?>(null)
    val printState: StateFlow<PrintResult?> = _printState.asStateFlow()

    // Constantes BT
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val PRINTER_BT_NAMES = listOf("POS-58", "5890", "BlueTooth Printer")
    private val TAG = "PrinterManager"

    // Socket persistente (se reutiliza entre impresiones)
    private var bluetoothSocket: BluetoothSocket? = null

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

    // ─────────────────────────────────────────────────────────────────────────
    // API Pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime un ticket con el tipo de impresora seleccionada actualmente.
     * Genera el texto ESC/POS y lo envía al hardware de forma segura en IO.
     *
     * @param productosSeleccionados  Lista de productos del pedido
     * @param mesaInfo   Texto de mesa/cliente (puede ser vacío)
     * @param mostrarCuenta  Si true, imprime datos de cuenta bancaria
     * @param numeroCuenta   El número de cuenta a imprimir si [mostrarCuenta] es true
     */
    suspend fun printTicket(
        productosSeleccionados: List<Producto>,
        mesaInfo: String,
        mostrarCuenta: Boolean,
        numeroCuenta: String
    ): PrintResult {
        val ticketText = generarTextoTicket(
            productosSeleccionados = productosSeleccionados,
            printerType = selectedPrinterType,
            mesaInfo = mesaInfo,
            mostrarCuenta = mostrarCuenta,
            numeroCuenta = numeroCuenta
        )
        val result = printWithSelectedPrinter(ticketText)
        _printState.value = result
        return result
    }

    fun resolveTicketProfile(printerType: PrinterType): PrinterType {
        return when (printerType) {
            PrinterType.NETWORK_58MM -> PrinterType.BLUETOOTH_58MM
            else -> printerType
        }
    }

    fun closeBluetoothSocket() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.d(TAG, "Socket Bluetooth cerrado.")
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar socket Bluetooth: ${e.message}", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación de Ticket ESC/POS
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generarTextoTicket(
        productosSeleccionados: List<Producto>,
        printerType: PrinterType,
        mesaInfo: String = "",
        mostrarCuenta: Boolean = false,
        numeroCuenta: String = ""
    ): String = withContext(Dispatchers.IO) {
        val ticketProfile = resolveTicketProfile(printerType)
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        val anchoTotalLinea = if (ticketProfile == PrinterType.NETWORK_80MM) 48 else 32
        val lineaSeparadora = "-".repeat(anchoTotalLinea)

        val ESC = "\u001B"
        val GS = "\u001D"
        val centerOn = "$ESC\u0061\u0001"
        val boldOn = "$ESC\u0045\u0001"
        val sizeXL = "$GS\u0021\u0011"
        val sizeWide = "$GS\u0021\u0010"
        val sizeNorm = "$GS\u0021\u0000"
        val boldOff = "$ESC\u0045\u0000"
        val centerOff = "$ESC\u0061\u0000"

        val headerSize = if (ticketProfile == PrinterType.NETWORK_80MM) sizeXL else sizeWide
        val headerCharSpacing = "$GS\u0020\u0000"
        val charSpacingReset = "$GS\u0020\u0000"

        val is58Profile = ticketProfile != PrinterType.NETWORK_80MM
        if (is58Profile) sb.append(centerOff) else sb.append(centerOn)
        sb.append(boldOn)
        sb.append(headerSize)
        sb.append(headerCharSpacing)
        val headerScale = if (is58Profile) 2 else 1
        if (ticketProfile == PrinterType.NETWORK_80MM) {
            sb.appendLine("ANTOJITOS")
            sb.appendLine("MEXICANOS")
            sb.appendLine("MARGARITA")
        } else {
            sb.appendLine(centerText("ANTOJITOS", anchoTotalLinea, headerScale))
            sb.appendLine(centerText("MEXICANOS", anchoTotalLinea, headerScale))
            sb.appendLine(centerText("MARGARITA", anchoTotalLinea, headerScale))
        }
        sb.append(charSpacingReset)
        sb.append(sizeNorm)
        sb.append(boldOff)
        sb.append(centerOff)
        sb.appendLine("=".repeat(anchoTotalLinea))
        sb.appendLine(centerText("*** TICKET DE COMPRA ***", anchoTotalLinea))
        sb.appendLine("=".repeat(anchoTotalLinea))
        sb.appendLine("Fecha y hora: $fechaHora")

        if (mostrarCuenta) {
            sb.appendLine("No. de cuenta: $numeroCuenta")
            sb.appendLine("Nombre: Margarita Daniel Pérez")
            sb.appendLine("Banco: BBVA")
        }

        if (mesaInfo.isNotEmpty()) {
            sb.appendLine(String.format("%-${anchoTotalLinea}s", "Mesa: ${mesaInfo.uppercase()}"))
            sb.appendLine("*****************************")
        }

        val combos = mutableListOf<Producto>()
        val normales = mutableListOf<Producto>()
        for (p in productosSeleccionados) if (p.esCombo) combos.add(p) else normales.add(p)

        sb.appendLine(lineaSeparadora)
        val productColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 23 else 13
        val priceColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 8 else 6
        val qtyColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 5 else 3
        val amountColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 9 else 7
        sb.appendLine(String.format("%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}s %${amountColWidth}s", "Producto", "Precio", "Cant", "Total"))
        sb.appendLine(lineaSeparadora)

        var totalGeneral = 0.0

        if (normales.isNotEmpty()) {
            sb.appendLine("PRODUCTOS")
            for (p in normales) {
                val totalProducto = p.precio * p.cantidad
                totalGeneral += totalProducto
                val nombreVisible = (productColWidth - 3).coerceAtLeast(5)
                val nombreCorto = if (p.nombre.length > productColWidth) p.nombre.substring(0, nombreVisible) + "..." else p.nombre
                val precioFmt = String.format("$%.2f", p.precio)
                val totalFmt = String.format("$%.2f", totalProducto)
                sb.appendLine(String.format("%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}d %${amountColWidth}s", nombreCorto, precioFmt, p.cantidad, totalFmt))
            }
            sb.appendLine(lineaSeparadora)
        }

        if (combos.isNotEmpty()) {
            sb.appendLine("COMBOS")
            for (c in combos) {
                val totalCombo = c.precio * c.cantidad
                totalGeneral += totalCombo
                val nombreVisible = (productColWidth - 3).coerceAtLeast(5)
                val nombreCorto = if (c.nombre.length > productColWidth) c.nombre.substring(0, nombreVisible) + "..." else c.nombre
                val precioFmt = String.format("$%.2f", c.precio)
                val totalFmt = String.format("$%.2f", totalCombo)
                sb.appendLine(String.format("%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}d %${amountColWidth}s", nombreCorto, precioFmt, c.cantidad, totalFmt))
            }
            sb.appendLine(lineaSeparadora)
        }

        val totalValueWidth = (anchoTotalLinea - 15).coerceAtLeast(8)
        sb.appendLine(String.format("%-15s %${totalValueWidth}s", "TOTAL:", String.format("$%.2f", totalGeneral)))
        sb.appendLine(lineaSeparadora)
        sb.appendLine("")
        sb.appendLine("    Gracias por su compra")
        sb.appendLine("    Vuelva pronto")

        return@withContext sb.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Impresión: Despacho según tipo seleccionado
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun printWithSelectedPrinter(ticketText: String): PrintResult {
        return when (selectedPrinterType) {
            PrinterType.BLUETOOTH_58MM -> printViaBluetooth(ticketText)
            PrinterType.NETWORK_80MM -> printViaNetwork(ticketText, is80mm = true)
            PrinterType.NETWORK_58MM -> printViaNetwork(ticketText, is80mm = false)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Red (TCP Socket)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun printViaNetwork(text: String, is80mm: Boolean = true): PrintResult =
        withContext(Dispatchers.IO) {
            try {
                val ticketBytes = text.toByteArray(Charset.forName("windows-1252"))
                val initPrinter = byteArrayOf(0x1B, 0x40)
                val logoBytes = getAppIconEscPos(
                    if (is80mm) PrinterType.NETWORK_80MM else PrinterType.BLUETOOTH_58MM
                )
                val feedAndCut = byteArrayOf(0x0A, 0x1D, 0x56, 0x42, 0x00)

                java.net.Socket("192.168.10.3", 9100).use { socket ->
                    socket.tcpNoDelay = true
                    socket.soTimeout = 4000
                    socket.getOutputStream().use { output ->
                        output.write(initPrinter)
                        output.write(logoBytes)
                        output.write(ticketBytes)
                        output.write(feedAndCut)
                        output.flush()
                    }
                }
                PrintResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error red: ${e.message}", e)
                PrintResult.Error("Error de red: ${e.message}")
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Bluetooth
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private suspend fun connectToBluetoothPrinter(): BluetoothSocket? =
        withContext(Dispatchers.IO) {
            val adapter = bluetoothAdapter
            if (adapter == null) {
                Log.e(TAG, "Adaptador Bluetooth no disponible.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bluetooth no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }

            if (!adapter.isEnabled) {
                Log.i(TAG, "Bluetooth no activado.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Activa el Bluetooth para imprimir", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }

            val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No hay dispositivos Bluetooth emparejados", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }

            val printerDevice = pairedDevices.firstOrNull { device ->
                val name = device.name ?: ""
                PRINTER_BT_NAMES.any { sig -> name.contains(sig, ignoreCase = true) }
            }

            if (printerDevice == null) {
                Log.e(TAG, "No encontré impresora POS-58 emparejada")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No encontré impresora POS-58 emparejada", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }

            return@withContext try {
                val socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
                adapter.cancelDiscovery()
                socket.connect()
                Log.d(TAG, "Conexión Bluetooth establecida con ${printerDevice.name}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Conectado a ${printerDevice.name}", Toast.LENGTH_SHORT).show()
                }
                bluetoothSocket = socket
                socket
            } catch (e: IOException) {
                Log.e(TAG, "Error al conectar BT con ${printerDevice.name}: ${e.message}", e)
                try { bluetoothSocket?.close() } catch (_: IOException) {}
                bluetoothSocket = null
                null
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(textoTicket: String): PrintResult =
        withContext(Dispatchers.IO) {
            try {
                val socket = bluetoothSocket?.takeIf { it.isConnected } ?: connectToBluetoothPrinter()
                if (socket == null || !socket.isConnected) {
                    return@withContext PrintResult.Error("No se pudo conectar a la impresora Bluetooth")
                }

                val outputStream: OutputStream = socket.outputStream
                val initPrinter = byteArrayOf(0x1B, 0x40)
                val logoBytes = getAppIconEscPos(PrinterType.BLUETOOTH_58MM)
                val ticketBytes = textoTicket.toByteArray(Charset.forName("windows-1252"))
                val feedAndCut = byteArrayOf(0x0A, 0x1D, 0x56, 0x42, 0x00)

                outputStream.write(initPrinter)
                outputStream.write(logoBytes)
                outputStream.write(ticketBytes)
                outputStream.write(feedAndCut)
                outputStream.flush()

                PrintResult.Success
            } catch (e: IOException) {
                Log.e(TAG, "Error de E/S BT: ${e.message}", e)
                PrintResult.Error("Error de comunicación: ${e.message}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de permisos BT: ${e.message}", e)
                PrintResult.Error("Error de permisos Bluetooth")
            } finally {
                closeBluetoothSocket()
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // ESC/POS: Logo Raster
    // ─────────────────────────────────────────────────────────────────────────

    fun getAppIconEscPos(printerType: PrinterType): ByteArray {
        val drawable = ContextCompat.getDrawable(context, R.drawable.pambazo)
            ?: return ByteArray(0)
        return if (printerType == PrinterType.NETWORK_80MM) {
            getEscPosImageWithEscStar(drawable, width = 512, height = 256)
        } else {
            getEscPosImageWithGsV0(drawable, width = 384, height = 128, usePrinterCenter = false)
        }
    }

    private fun drawableToBitmap(
        drawable: android.graphics.drawable.Drawable,
        width: Int,
        height: Int
    ): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val srcW = drawable.intrinsicWidth.takeIf { it > 0 } ?: width
        val srcH = drawable.intrinsicHeight.takeIf { it > 0 } ?: height
        val scale = minOf(width.toFloat() / srcW.toFloat(), height.toFloat() / srcH.toFloat())
        val drawW = (srcW * scale).toInt().coerceAtLeast(1)
        val drawH = (srcH * scale).toInt().coerceAtLeast(1)
        val left = (width - drawW) / 2
        val top = (height - drawH) / 2
        drawable.setBounds(left, top, left + drawW, top + drawH)
        drawable.draw(canvas)
        return bitmap
    }

    private fun isDarkPixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (r * 0.299 + g * 0.587 + b * 0.114).toInt() < 128
    }

    private fun getEscPosImageWithGsV0(
        drawable: android.graphics.drawable.Drawable,
        width: Int,
        height: Int,
        usePrinterCenter: Boolean = true
    ): ByteArray {
        val bitmap = drawableToBitmap(drawable, width, height)
        val xL = (width / 8).toByte()
        val xH = ((width / 8) / 256).toByte()
        val yL = (height % 256).toByte()
        val yH = (height / 256).toByte()
        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
        val imageData = ByteArray((width / 8) * height)
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width step 8) {
                var byteValue = 0
                for (k in 0..7) {
                    val bit = if (x + k < width && isDarkPixel(bitmap.getPixel(x + k, y))) 1 else 0
                    byteValue = byteValue or (bit shl (7 - k))
                }
                imageData[index++] = byteValue.toByte()
            }
        }
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val feed = byteArrayOf(0x0A, 0x0A)
        val alignStart = if (usePrinterCenter) alignCenter else alignLeft
        val result = ByteArray(alignStart.size + header.size + imageData.size + feed.size + alignLeft.size)
        var offset = 0
        System.arraycopy(alignStart, 0, result, offset, alignStart.size); offset += alignStart.size
        System.arraycopy(header, 0, result, offset, header.size); offset += header.size
        System.arraycopy(imageData, 0, result, offset, imageData.size); offset += imageData.size
        System.arraycopy(feed, 0, result, offset, feed.size); offset += feed.size
        System.arraycopy(alignLeft, 0, result, offset, alignLeft.size)
        return result
    }

    private fun getEscPosImageWithEscStar(
        drawable: android.graphics.drawable.Drawable,
        width: Int,
        height: Int
    ): ByteArray {
        val bitmap = drawableToBitmap(drawable, width, height)
        val out = ByteArrayOutputStream()
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val lineSpacing24 = byteArrayOf(0x1B, 0x33, 24)
        val lineSpacingDefault = byteArrayOf(0x1B, 0x32)
        out.write(alignCenter)
        out.write(lineSpacing24)
        val widthDots = width
        for (y in 0 until height step 24) {
            val nL = (widthDots % 256).toByte()
            val nH = (widthDots / 256).toByte()
            out.write(byteArrayOf(0x1B, 0x2A, 33, nL, nH))
            for (x in 0 until width) {
                for (block in 0 until 3) {
                    var byteValue = 0
                    for (bit in 0 until 8) {
                        val yy = y + block * 8 + bit
                        val pixelOn = if (yy < height && isDarkPixel(bitmap.getPixel(x, yy))) 1 else 0
                        byteValue = byteValue or (pixelOn shl (7 - bit))
                    }
                    out.write(byteValue)
                }
            }
            out.write(0x0A)
        }
        out.write(lineSpacingDefault)
        out.write(byteArrayOf(0x0A, 0x0A))
        out.write(alignLeft)
        return out.toByteArray()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades de texto
    // ─────────────────────────────────────────────────────────────────────────

    fun centerText(text: String, width: Int, charScale: Int = 1): String {
        val safeScale = charScale.coerceAtLeast(1)
        val effectiveWidth = (width / safeScale).coerceAtLeast(text.length)
        val leftPadding = ((effectiveWidth - text.length) / 2).coerceAtLeast(0)
        return " ".repeat(leftPadding) + text
    }
}
