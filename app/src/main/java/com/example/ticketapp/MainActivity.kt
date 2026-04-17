package com.example.ticketapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.*
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // --- Constantes ---
    private companion object {
        private const val TAG = "MainActivity"

        // Nombres comunes de impresoras POS-58 / 5890A-L vía Bluetooth.
        private val PRINTER_BT_NAMES = listOf("POS-58", "5890", "BlueTooth Printer")

        // SPP UUID clásico (Serial Port Profile)
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private const val EXTRA_COMBO = 30.0
    }

    private lateinit var txtTotal: TextView
    private var txtNormales = mutableMapOf<String, TextView>()
    private var txtCombos = mutableMapOf<String, TextView>()

    private val preciosHamburguesas =
            mapOf(
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
    private lateinit var printerTypeGroup: RadioGroup
    private lateinit var radioPrinter58: RadioButton
    private lateinit var radioPrinter80Network: RadioButton
    private lateinit var radioPrinter58Network: RadioButton
    private lateinit var layoutPrinter80Help: View
    private lateinit var btnPrinter80Help: ImageButton
        private lateinit var btnEditarCatalogo: MaterialButton
    private lateinit var editTextMesa: EditText
    private lateinit var noCuenta: CheckBox

        private var catalogConfig =
            CatalogConfig(
                categoryOrder = CatalogConfigStore.defaultCategories.toMutableList(),
                productOverrides = mutableMapOf(),
                customProducts = mutableListOf()
            )

        private val catalogEditorLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                recreate()
            }
            }

    private enum class PrinterType {
        BLUETOOTH_58MM,
        NETWORK_80MM,
        NETWORK_58MM
    }

    private fun resolveTicketProfile(printerType: PrinterType): PrinterType {
        return when (printerType) {
            PrinterType.NETWORK_58MM -> PrinterType.BLUETOOTH_58MM
            else -> printerType
        }
    }

    private fun centerText(text: String, width: Int, charScale: Int = 1): String {
        val safeScale = charScale.coerceAtLeast(1)
        // Para fuentes anchas (doble ancho), centrar por columnas efectivas evita
        // desplazamientos hacia la derecha en impresoras 58mm.
        val effectiveWidth = (width / safeScale).coerceAtLeast(text.length)
        val leftPadding = ((effectiveWidth - text.length) / 2).coerceAtLeast(0)
        return " ".repeat(leftPadding) + text
    }

    private val legacyProductAliases =
            mapOf(
                    "Pambazos Naturales Queso" to "Pambazos Naturales Extra",
                    "Pambazos Naturales Combinados Queso" to
                            "Pambazos Naturales Combinados con Queso",
                    "Pambazos Adobados Combinados Queso" to
                            "Pambazos Adobados Combinados con Queso",
                    "Volcan Queso Guisado Extra" to "Volcan Queso/Guisado Extra",
                    "Guajolota Extra" to "Volcan Queso/Guisado Extra"
            )

    private fun normalizeProductLabel(value: String): String {
        val withoutAccents =
                Normalizer.normalize(value, Normalizer.Form.NFD)
                        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents
                .lowercase(Locale.getDefault())
                .replace("[^a-z0-9 ]".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
    }

    private fun resolveExistingProductName(nameFromOrder: String): String? {
        val direct = nameFromOrder.trim()
        if (products.containsKey(direct)) return direct

        val aliased = legacyProductAliases[direct]
        if (aliased != null && products.containsKey(aliased)) return aliased

        val hamburgerSourceByDisplayName =
                catalogConfig.productOverrides.entries.firstOrNull { (_, override) ->
                    override.category.equals(CATEG_HAMB, ignoreCase = true) &&
                            override.name.trim().equals(direct, ignoreCase = true)
                }?.key
        if (hamburgerSourceByDisplayName != null && preciosHamburguesas.containsKey(hamburgerSourceByDisplayName)) {
            return hamburgerSourceByDisplayName
        }

        val normalizedTarget = normalizeProductLabel(direct)
        return products.keys.firstOrNull { key ->
            normalizeProductLabel(key) == normalizedTarget ||
                    normalizeProductLabel(key) == normalizeProductLabel(aliased ?: "")
        }
    }

    private fun getHamburguesaDisplayName(sourceName: String): String {
        val overrideName = catalogConfig.productOverrides[sourceName]?.name?.trim().orEmpty()
        return overrideName.ifBlank { sourceName }
    }

    private fun resolveHamburguesaSourceName(name: String): String? {
        val direct = name.trim()
        if (preciosHamburguesas.containsKey(direct)) return direct

        val byDisplayName =
                catalogConfig.productOverrides.entries.firstOrNull { (_, override) ->
                    override.category.equals(CATEG_HAMB, ignoreCase = true) &&
                            override.name.trim().equals(direct, ignoreCase = true)
                }?.key
        if (byDisplayName != null) return byDisplayName

        return preciosHamburguesas.keys.firstOrNull {
            normalizeProductLabel(it) == normalizeProductLabel(direct)
        }
    }

    private val duplicatedTextViewCache = mutableMapOf<Int, MutableList<TextView>>()

    private fun registerDuplicatedTextView(view: TextView?) {
        if (view == null || view.id == View.NO_ID) return
        val list = duplicatedTextViewCache.getOrPut(view.id) { mutableListOf() }
        if (list.none { it === view }) {
            list.add(view)
        }
    }

    private fun setAllTextViewsById(viewId: Int, text: String) {
        val cached = duplicatedTextViewCache[viewId]
        if (!cached.isNullOrEmpty()) {
            cached.forEach { it.text = text }
            return
        }

        fun walk(view: View?) {
            if (view == null) return
            if (view.id == viewId && view is TextView) {
                view.text = text
                registerDuplicatedTextView(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    walk(view.getChildAt(i))
                }
            }
        }

        walk(findViewById(android.R.id.content))
    }

    private fun syncSpecialDuplicatedProductViews(nombre: String, qty: Int) {
        when (nombre) {
            "Pozole Grande" -> setAllTextViewsById(R.id.cantidadPozoleGrande, qty.toString())
            "Pozole Chico" -> setAllTextViewsById(R.id.cantidadPozoleChico, qty.toString())
        }
    }

    private var selectedPrinterType = PrinterType.BLUETOOTH_58MM

    private val products = mutableMapOf<String, ProductData>()
    private val quantities = mutableMapOf<String, Int>()

    // Map to store the assigned random color for each product
    private val productColors = mutableMapOf<String, Int>()

    /** Retorna un R.color resource ID aleatorio de la paleta pastel. */
    private fun randomizeColor(): Int {
        val colorResIds =
                listOf(
                        R.color.Pale_Olive,
                        R.color.Sage_Green,
                        R.color.Dusty_Seafoam,
                        R.color.Creamy_Beige,
                        R.color.Muted_Fern,
                        R.color.old_grayish_green
                )
        return colorResIds.random()
    }

    private lateinit var appDatabase: AppDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnGananciaDiaria: MaterialButton
    private lateinit var btnGananciaSemanal: MaterialButton
    private lateinit var btnGananciaMensual: MaterialButton
    private lateinit var adminSummaryTextView: TextView
    private lateinit var recyclerViewOrders: RecyclerView

    // El adaptador se declara una sola vez arriba

    private var isEditMode = false
    private var currentEditingOrderId: Long? = null

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

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private var bluetoothSocket: BluetoothSocket? = null

    private val requestBluetoothPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                if (permissions.entries.all { it.value }) {
                    Toast.makeText(this, "Permisos Bluetooth concedidos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                                    this,
                                    "Se requieren permisos de Bluetooth para imprimir",
                                    Toast.LENGTH_LONG
                            )
                            .show()
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

    private val CATEG_HAMB = "Hamburguesas"
    private val CATEG_PAPAS = "Papas"
    private val CATEG_TACOS = "Tacos"
    private val CATEG_ALITAS = "Alitas"
    private val CATEG_BEBIDAS = "Bebidas"
    private val CATEG_POSTRES = "Postres"
    private val CATEG_EXTRAS = "Extras"
    private val CATEG_OTROS = "Otros"

    private val productCategory: MutableMap<String, String> = mutableMapOf()

    // Margen por categoría (0.30 = 30% de ganancia sobre ventas)
    // Ajusta a tus costos reales cuando los tengas.
    private val categoryMargin: MutableMap<String, Double> =
            mutableMapOf(
                    CATEG_HAMB to 0.30,
                    CATEG_PAPAS to 0.30,
                    CATEG_TACOS to 0.30,
                    CATEG_ALITAS to 0.30,
                    CATEG_BEBIDAS to 0.25,
                    CATEG_POSTRES to 0.30,
                    CATEG_EXTRAS to 0.30,
                    CATEG_OTROS to 0.30
            )

    // animación de flechitas para colapsar categorías
    fun View.rotateArrow(expanded: Boolean, duration: Long = 200L) {
        animate()
                .rotation(if (expanded) 180f else 0f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
    }

    var isExpanded = false

    private lateinit var editCategoryStartDate: EditText
    private lateinit var editCategoryEndDate: EditText

    private fun recalcHamburguesasTotalYResumen() {
        var total = 0.0
        for ((nombre, precioBase) in preciosHamburguesas) {
            val n = cantidadesNormales[nombre] ?: 0
            val c = cantidadesCombo[nombre] ?: 0
            total += (precioBase * n) + ((precioBase + extraCombo) * c)
        }
        txtTotal.text = "Total: $%.2f".format(total)

        // Actualiza el resumen en tiempo real (usa tu función existente)
        val productosSeleccionados = obtenerProductosDesdeInputs()
        mostrarResumen(productosSeleccionados)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        catalogConfig = CatalogConfigStore.load(this)

        setupCategoryProfitPanel()
        // setupAdminGananciaPorCategoria() // Replaced by Top Selling Panel setup later

        // Inicializa la base de datos una vez al inicio. La observación se configurará
        // después de que el adaptador de órdenes haya sido configurado para evitar
        // condiciones de carrera.
        appDatabase = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        txtTotal = findViewById(R.id.textViewTotal)

        editCategoryStartDate =
                requireNotNull(
                        findViewById<EditText>(R.id.editCategoryStartDate)
                                ?: findViewById(R.id.editStartDate)
                ) { "Falta editCategoryStartDate y tampoco existe editStartDate en activity_main" }

        editCategoryEndDate =
                requireNotNull(
                        findViewById<EditText>(R.id.editCategoryEndDate)
                                ?: findViewById(R.id.editEndDate)
                ) { "Falta editCategoryEndDate y tampoco existe editEndDate en activity_main" }

        // 3) Y ahora sí, configura los paneles que las usan
        setupCategoryProfitPanel()
        setupTopSellingPanel()
        setupDatePickersCategory()

        // Inicializa los mapas vacíos por ahora, se llenarán al inflar la vista
        txtNormales = mutableMapOf()
        txtCombos = mutableMapOf()

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

        // Botones se encontrarán al inflar

        // DB y panel admin
        // La base de datos ya fue inicializada al inicio de onCreate, no es necesario volver a
        // asignarla aquí
        // appDatabase = AppDatabase.getDatabase(this, lifecycleScope)
        drawerLayout = findViewById(R.id.drawerLayout)
        btnGananciaDiaria = findViewById(R.id.btnGananciaDiaria)
        btnGananciaSemanal = findViewById(R.id.btnGananciaSemanal)
        btnGananciaMensual = findViewById(R.id.btnGananciaMensual)
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
        adminOrderAdapter =
                AdminOrderAdapter(
                        onDelete = { orderId -> eliminarPedido(orderId) },
                        onOrderClick = { order -> mostrarResumenDeOrdenGuardada(order) },
                        onPrint = { order -> reimprimirTicket(order) },
                        onPayClick = { order -> showPaymentConfirmationBottomSheet(order) },
                        onAddItems = { order -> onOrderAddItem(order) }
                )

        recyclerViewOrders.adapter = adminOrderAdapter
        // Comienza a observar la lista de órdenes una vez que el adaptador está listo
        observarOrdenes()

        btnGananciaDiaria.setOnClickListener { generarGananciaDiaria() }
        btnGananciaSemanal.setOnClickListener { generarGananciaSemanal() }
        btnGananciaMensual.setOnClickListener { generarGananciaMensual() }

        // No llamamos a cargarPedidos(). El flujo de datos de Room se encarga
        // de actualizar la lista automáticamente.

        setupProductViews()
        setupButtons()
        setupCollapsibleCategories()

        // Configura el filtro de ventas por producto (spinner, fechas, botón)
        setupProductSalesFilter()

        editCategoryStartDate.setOnClickListener { showDatePicker(editCategoryStartDate) }
        editCategoryEndDate.setOnClickListener { showDatePicker(editCategoryEndDate) }

        // permisos iniciales
        checkAndRequestBluetoothPermissions()

        // Configurar la calculadora (drawer izquierdo)
        setupCalculator()
    }

    // --- HAMBURGUESAS (a nivel de clase) ---

    override fun onDestroy() {
        super.onDestroy()
        closeBluetoothSocket()
    }

    // ─── Calculadora (drawer izquierdo) ──────────────────────────────────────

    // Estado de la calculadora
    private var calcFirstOperand: Double = 0.0
    private var calcOperator: String = ""
    private var calcShouldResetDisplay: Boolean = false

    /** Configura todos los botones del panel calculadora. */
    private fun setupCalculator() {
        val root = findViewById<View>(R.id.calculatorDrawer)

        val tvDisplay = root.findViewById<TextView>(R.id.tvCalcDisplay)
        val tvExpression = root.findViewById<TextView>(R.id.tvCalcExpression)

        // ── Funciones internas ──────────────────────────────────────────────

        fun currentValue(): Double = tvDisplay.text.toString().toDoubleOrNull() ?: 0.0

        fun formatResult(value: Double): String {
            return if (value == kotlin.math.floor(value) && !value.isInfinite()) {
                value.toLong().toString()
            } else {
                "%.6g".format(value).trimEnd('0').trimEnd('.')
            }
        }

        fun appendDigit(digit: String) {
            if (calcShouldResetDisplay) {
                tvDisplay.text = if (digit == ".") "0." else digit
                calcShouldResetDisplay = false
            } else {
                val current = tvDisplay.text.toString()
                if (digit == "." && current.contains('.')) return // evitar doble punto
                tvDisplay.text = if (current == "0" && digit != ".") digit else current + digit
            }
        }

        fun applyOperator(op: String) {
            // Si ya hay operador pendiente, calcular antes de guardar nuevo
            if (calcOperator.isNotEmpty() && !calcShouldResetDisplay) {
                val second = currentValue()
                val result =
                        when (calcOperator) {
                            "+" -> calcFirstOperand + second
                            "−" -> calcFirstOperand - second
                            "×" -> calcFirstOperand * second
                            "÷" -> if (second != 0.0) calcFirstOperand / second else Double.NaN
                            else -> second
                        }
                tvDisplay.text = formatResult(result)
                calcFirstOperand = result
            } else {
                calcFirstOperand = currentValue()
            }
            calcOperator = op
            calcShouldResetDisplay = true
            tvExpression.text = "${formatResult(calcFirstOperand)} $op"
        }

        fun calculate() {
            if (calcOperator.isEmpty()) return
            val second = currentValue()
            val result =
                    when (calcOperator) {
                        "+" -> calcFirstOperand + second
                        "−" -> calcFirstOperand - second
                        "×" -> calcFirstOperand * second
                        "÷" -> if (second != 0.0) calcFirstOperand / second else Double.NaN
                        else -> second
                    }
            tvExpression.text =
                    "${formatResult(calcFirstOperand)} $calcOperator ${formatResult(second)} ="
            tvDisplay.text = if (result.isNaN()) "Error" else formatResult(result)
            calcOperator = ""
            calcShouldResetDisplay = true
        }

        // ── Dígitos ─────────────────────────────────────────────────────────
        listOf(
                        R.id.btnCalc0 to "0",
                        R.id.btnCalc1 to "1",
                        R.id.btnCalc2 to "2",
                        R.id.btnCalc3 to "3",
                        R.id.btnCalc4 to "4",
                        R.id.btnCalc5 to "5",
                        R.id.btnCalc6 to "6",
                        R.id.btnCalc7 to "7",
                        R.id.btnCalc8 to "8",
                        R.id.btnCalc9 to "9",
                        R.id.btnCalcDot to "."
                )
                .forEach { (id, digit) ->
                    root.findViewById<MaterialButton>(id).setOnClickListener { appendDigit(digit) }
                }

        // ── Operadores ──────────────────────────────────────────────────────
        listOf(
                        R.id.btnCalcAdd to "+",
                        R.id.btnCalcSub to "−",
                        R.id.btnCalcMul to "×",
                        R.id.btnCalcDiv to "÷"
                )
                .forEach { (id, op) ->
                    root.findViewById<MaterialButton>(id).setOnClickListener { applyOperator(op) }
                }

        // ── Igual ────────────────────────────────────────────────────────────
        root.findViewById<MaterialButton>(R.id.btnCalcEquals).setOnClickListener { calculate() }

        // ── AC (All Clear) ──────────────────────────────────────────────────
        root.findViewById<MaterialButton>(R.id.btnCalcClear).setOnClickListener {
            tvDisplay.text = "0"
            tvExpression.text = ""
            calcFirstOperand = 0.0
            calcOperator = ""
            calcShouldResetDisplay = false
        }

        // ── +/- (cambio de signo) ────────────────────────────────────────────
        root.findViewById<MaterialButton>(R.id.btnCalcSign).setOnClickListener {
            val v = currentValue()
            tvDisplay.text = formatResult(-v)
        }

        // ── % (porcentaje del primero, o simple /100) ────────────────────────
        root.findViewById<MaterialButton>(R.id.btnCalcPercent).setOnClickListener {
            val v = currentValue()
            tvDisplay.text =
                    formatResult(
                            if (calcOperator.isNotEmpty()) calcFirstOperand * v / 100.0
                            else v / 100.0
                    )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun printViaNetwork(text: String, is80mm: Boolean = true): Boolean {
        return try {
            val ticketBytes = text.toByteArray(Charset.forName("windows-1252"))
            val initPrinter = byteArrayOf(0x1B, 0x40)
            val logoBytes =
                    getAppIconEscPos(
                    if (is80mm) PrinterType.NETWORK_80MM
                    else PrinterType.BLUETOOTH_58MM
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
            true
        } catch (e: Exception) {
            Log.e("PRINT_NET", "Error: ${e.message}", e)
            false
        }
    }

    private suspend fun printWithSelectedPrinter(ticketText: String): Boolean {
        return when (selectedPrinterType) {
            PrinterType.BLUETOOTH_58MM -> printViaBluetooth(ticketText)
            PrinterType.NETWORK_80MM -> printViaNetwork(ticketText, true)
            PrinterType.NETWORK_58MM -> printViaNetwork(ticketText, false)
        }
    }

    private fun showPaymentConfirmationBottomSheet(order: OrderEntity) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_payment_confirmation, null)
        dialog.setContentView(view)

        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPayment)
        val tvMessage = view.findViewById<TextView>(R.id.tvConfirmationMessage)

        tvMessage.text =
                "¿Deseas marcar la orden de la ${order.mesa ?: "Mesa desconocida"} como pagada?\nTotal: $${
                "%.2f".format(
                    order.grandTotal
                )
            }"

        btnConfirm.setOnClickListener {
            onOrderToggleStatus(order.orderId, true)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showOrderSummaryDialog(productosSeleccionados: List<Producto>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_confirm_order, null)
        dialog.setContentView(view)

        val tvTotal = view.findViewById<TextView>(R.id.tvOrderSummaryTotal)
        val tvDetails = view.findViewById<TextView>(R.id.tvOrderSummaryDetails)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmOrder)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelOrder)

        // Calcular total y construir detalle
        var totalGeneral = 0.0
        val sb = StringBuilder()

        val combos = productosSeleccionados.filter { it.esCombo }
        val normales = productosSeleccionados.filter { !it.esCombo }

        if (normales.isNotEmpty()) {
            sb.appendLine("🔸 PLATILLOS:")
            normales.forEach { p ->
                val totalItem = p.precio * p.cantidad
                totalGeneral += totalItem
                sb.appendLine("   • ${p.cantidad} x ${p.nombre} ... $${"%.2f".format(totalItem)}")
            }
            sb.appendLine()
        }

        if (combos.isNotEmpty()) {
            sb.appendLine("🔥 COMBOS:")
            combos.forEach { c ->
                val totalItem = c.precio * c.cantidad
                totalGeneral += totalItem
                sb.appendLine("   • ${c.cantidad} x ${c.nombre} ... $${"%.2f".format(totalItem)}")
            }
        }

        // Agregar info de mesa si existe
        val mesaInfo = editTextMesa.text.toString().trim()
        if (mesaInfo.isNotEmpty()) {
            sb.appendLine("\n📍 Mesa: $mesaInfo")
        }

        tvDetails.text = sb.toString()
        tvTotal.text = "Total a Pagar: $${"%.2f".format(totalGeneral)}"

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            // Aquí movemos la lógica de impresión original
            ejecutarImpresion(productosSeleccionados, dialog)
        }

        dialog.show()
    }

    private fun ejecutarImpresion(
            productosSeleccionados: List<Producto>,
            dialog: BottomSheetDialog
    ) {
        if (isPrinting) return
        isPrinting = true
        // deshabilitar botón del dialog para evitar doble click
        dialog.findViewById<View>(R.id.btnConfirmOrder)?.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1️⃣ Generar el texto del ticket
                val textoTicket = generarTextoTicket(productosSeleccionados, selectedPrinterType)

                // 2️⃣ Guardar la orden en la base de datos
                val mesaInfo = editTextMesa.text.toString().trim()
                guardarOrden(productosSeleccionados, mesaInfo)

                // 3️⃣ Imprimir automáticamente
                val exito =
                        try {
                            printWithSelectedPrinter(textoTicket)
                        } catch (e: Exception) {
                            Log.e("PRINT", "Error al imprimir con impresora seleccionada", e)
                            false
                        }

                // 4️⃣ Mostrar mensaje en la UI
                withContext(Dispatchers.Main) {
                    if (exito) {
                        Toast.makeText(
                                        this@MainActivity,
                                        "Ticket enviado a impresora",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    } else {
                        Toast.makeText(
                                        this@MainActivity,
                                        "Error al conectar con impresora",
                                        Toast.LENGTH_LONG
                                )
                                .show()
                    }
                    limpiarCantidades()
                    isPrinting = false
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    isPrinting = false
                    dialog.dismiss()
                }
            }
        }
    }

    // --- Configuración de la UI (productos, botones, secciones colapsables) ---

    private fun setupProductViews() {
        // --- Parte 1: Definición de productos que NO son hamburguesas ---
        // Inicialmente vacío, los productos se configurarán al inflar las vistas
    }

    // --- Debounce Logic ---
    private var recalculateJob: Job? = null

    private fun requestRecalculate() {
        recalculateJob?.cancel()
        recalculateJob =
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(150) // Debounce de 150ms
                    recalculateAndUpdateAll()
                }
    }

    /** Muestra un AlertDialog con teclado numérico del sistema */
    private fun showNumericKeypad(
            productName: String,
            currentQuantity: Int,
            onConfirm: (Int) -> Unit
    ) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_numeric_keypad, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvKeypadTitle)
        val tvValue = view.findViewById<TextView>(R.id.tvKeypadValue)

        tvTitle.text = productName
        tvValue.text = currentQuantity.toString()

        var workingValue = if (currentQuantity == 0) "" else currentQuantity.toString()

        // Función interna para actualizar el display
        fun updateDisplay(char: String) {
            if (workingValue.length < 4) { // Límite de 4 dígitos
                workingValue += char
                tvValue.text = workingValue
            }
        }

        // Configurar botones numéricos (1-9 y 0)
        val numericButtons =
                listOf(
                        R.id.btnKey0 to "0",
                        R.id.btnKey1 to "1",
                        R.id.btnKey2 to "2",
                        R.id.btnKey3 to "3",
                        R.id.btnKey4 to "4",
                        R.id.btnKey5 to "5",
                        R.id.btnKey6 to "6",
                        R.id.btnKey7 to "7",
                        R.id.btnKey8 to "8",
                        R.id.btnKey9 to "9"
                )

        numericButtons.forEach { (id, value) ->
            view.findViewById<Button>(id).setOnClickListener { updateDisplay(value) }
        }

        // Botón Clear (C)
        view.findViewById<Button>(R.id.btnKeyClear).setOnClickListener {
            workingValue = ""
            tvValue.text = "0"
        }

        // Botón Backspace
        view.findViewById<Button>(R.id.btnKeyBackspace).setOnClickListener {
            if (workingValue.isNotEmpty()) {
                workingValue = workingValue.dropLast(1)
                tvValue.text = if (workingValue.isEmpty()) "0" else workingValue
            }
        }

        // Botón Cancelar
        view.findViewById<Button>(R.id.btnKeyCancel).setOnClickListener { dialog.dismiss() }

        // Botón Confirmar
        view.findViewById<Button>(R.id.btnKeyConfirm).setOnClickListener {
            val finalValue = workingValue.toIntOrNull() ?: 0
            onConfirm(finalValue)
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Cambia el color de fondo de la tarjeta según la cantidad.
     * - qty > 0: muestra el color aleatorio guardado en [productColors] para [nombre]. Si aún no
     * tiene uno asignado (primera vez), genera uno y lo guarda.
     * - qty == 0: restaura CharcoalCool (el color uniforme de reposo).
     * @param view Cualquier vista hija de la MaterialCardView.
     * @param quantity Cantidad actual del producto.
     * @param nombre Clave del producto en [productColors].
     */
    private fun updateCardAppearance(view: View?, quantity: Int, nombre: String) {
        if (view == null) return
        val charcoalCool = ContextCompat.getColor(this, R.color.CharcoalCool)
        try {
            var parent = view.parent
            while (parent != null) {
                if (parent is MaterialCardView) {
                    if (quantity > 0) {
                        // Asignar color aleatorio solo la primera vez que se agrega el producto
                        val color =
                                productColors.getOrPut(nombre) {
                                    ContextCompat.getColor(this, randomizeColor())
                                }
                        parent.setCardBackgroundColor(color)
                    } else {
                        // Volver al color uniforme cuando la cantidad es 0
                        productColors.remove(nombre)
                        parent.setCardBackgroundColor(charcoalCool)
                    }
                    break
                }
                parent = parent.parent
            }
        } catch (e: Exception) {
            // Ignorar
        }
    }

    private fun setupHamburguesasGrid(grid: GridLayout) {
        grid.removeAllViews()
        grid.columnCount = 2 // O 3 si quieres más en tablets

        val inflater = LayoutInflater.from(this)

        // Lista de hamburguesas, obtenida de las claves del mapa de precios.
        val hamburguesas = preciosHamburguesas.keys.toList()

        // Crea una card para cada hamburguesa
        hamburguesas.forEach { nombre ->
            val displayName = getHamburguesaDisplayName(nombre)
            addHamburguesaCard(nombre, displayName, grid, inflater)
        }
    }

    // ✅ NUEVA FUNCIÓN: Crea y configura UNA SOLA card de hamburguesa.
    // La sacamos fuera del bucle para mayor claridad.
        private fun addHamburguesaCard(
            nombre: String,
            displayName: String,
            grid: GridLayout,
            inflater: LayoutInflater
        ) {
        // 0. Las cards arrancan con CharcoalCool (color uniforme en reposo)
        val charcoalCool = ContextCompat.getColor(this, R.color.CharcoalCool)

        // 1. Crear el contenedor (Card)
        val card =
                MaterialCardView(this).apply {
                    layoutParams =
                            GridLayout.LayoutParams().apply {
                                width = 0
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                setMargins(16, 16, 16, 16)
                            }
                    setCardBackgroundColor(charcoalCool)
                    radius = 16f
                    cardElevation = 4f
                }

        // 2. Crear el contenido interno
        val content =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    gravity = Gravity.CENTER_HORIZONTAL
                }

        // 3. Título
        content.addView(
                TextView(this).apply {
                    text = displayName
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                    textSize = 16f
                    gravity = Gravity.CENTER
                }
        )

        // 4. TextView de Cantidades (TARGET PARA EL TECLADO)
        val tvCant =
                TextView(this).apply {
                    val n = cantidadesNormales[nombre] ?: 0
                    val c = cantidadesCombo[nombre] ?: 0
                    text = "Normales: $n   Combos: $c"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    isClickable = true
                    background =
                            ContextCompat.getDrawable(
                                    this@MainActivity,
                                    android.R.drawable.list_selector_background
                            )
                }
        txtNormales[nombre] = tvCant
        content.addView(tvCant)

        // 5. Controles (+/-)
        val controles = inflater.inflate(R.layout.layout_hamburguesa_buttons, content, false)
        content.addView(controles)

        val btnMasNormal = controles.findViewById<Button>(R.id.btnMasNormal)
        val btnMenosNormal = controles.findViewById<Button>(R.id.btnMenosNormal)
        val btnMasCombo = controles.findViewById<Button>(R.id.btnMasCombo)
        val btnMenosCombo = controles.findViewById<Button>(R.id.btnMenosCombo)

        fun refrescarUI() {
            val n = cantidadesNormales[nombre] ?: 0
            val c = cantidadesCombo[nombre] ?: 0
            tvCant.text = "Normales: $n   Combos: $c"
            updateCardAppearance(tvCant, n + c, nombre)
            requestRecalculate()
        }

        // --- LÓGICA DEL TECLADO NUMÉRICO ---
        tvCant.setOnClickListener {
            val opciones = arrayOf("Editar Normales", "Editar Combos")
            AlertDialog.Builder(this)
                    .setTitle("¿Qué deseas editar?")
                    .setItems(opciones) { _, which ->
                        if (which == 0) {
                            // Editar Normales
                            val actual = cantidadesNormales[nombre] ?: 0
                            showNumericKeypad("$nombre (Normal)", actual) { nuevaQty ->
                                cantidadesNormales[nombre] = nuevaQty
                                refrescarUI()
                            }
                        } else {
                            // Editar Combos
                            val actual = cantidadesCombo[nombre] ?: 0
                            showNumericKeypad("$nombre (Combo)", actual) { nuevaQty ->
                                cantidadesCombo[nombre] = nuevaQty
                                refrescarUI()
                            }
                        }
                    }
                    .show()
        }

        // listeners de botones existentes
        btnMasNormal.setOnClickListener {
            cantidadesNormales[nombre] = (cantidadesNormales[nombre] ?: 0) + 1
            refrescarUI()
        }
        btnMenosNormal.setOnClickListener {
            cantidadesNormales[nombre] = ((cantidadesNormales[nombre] ?: 0) - 1).coerceAtLeast(0)
            refrescarUI()
        }
        btnMasCombo.setOnClickListener {
            cantidadesCombo[nombre] = (cantidadesCombo[nombre] ?: 0) + 1
            refrescarUI()
        }
        btnMenosCombo.setOnClickListener {
            cantidadesCombo[nombre] = ((cantidadesCombo[nombre] ?: 0) - 1).coerceAtLeast(0)
            refrescarUI()
        }

        card.setOnClickListener {
            cantidadesNormales[nombre] = (cantidadesNormales[nombre] ?: 0) + 1
            refrescarUI()
        }

        // Long clicks
        btnMenosNormal.setOnLongClickListener {
            cantidadesNormales[nombre] = 0
            refrescarUI()
            true
        }
        btnMenosCombo.setOnLongClickListener {
            cantidadesCombo[nombre] = 0
            refrescarUI()
            true
        }

        card.addView(content)
        grid.addView(card)
    }

    private fun setupGenericProductClick(
            tvCantidad: TextView,
            nombre: String,
            mapa: MutableMap<String, Int>,
            viewCard: View
    ) {
        tvCantidad.setOnClickListener {
            val qty = mapa[nombre] ?: 0
            showNumericKeypad(nombre, qty) { nuevaQty ->
                mapa[nombre] = nuevaQty
                tvCantidad.text = nuevaQty.toString()
                updateCardAppearance(
                        tvCantidad,
                        nuevaQty,
                        nombre
                ) // Pasamos tvCantidad como referencia
                requestRecalculate() // Usamos tu función actual de cálculo
            }
        }
    }
    /**
     * Actualiza la cantidad de CUALQUIER producto y recalcula el total y el resumen.
     * @param nombreProducto El nombre del producto (ej. "Taco (c/u)").
     * @param cambio La cantidad a cambiar (+1 para sumar, -1 para restar).
     */

    /**
     * Recalcula el total SUMANDO las hamburguesas y el resto de productos. Luego, actualiza la UI
     * (Total y Resumen).
     */
    fun recalculateAndUpdateAll() {
        var totalGeneral = 0.0

        // --- Parte 1: Sumar hamburguesas (Normales y Combos) ---
        for ((nombre, precioBase) in preciosHamburguesas) {
            val normales = cantidadesNormales[nombre] ?: 0
            val combos = cantidadesCombo[nombre] ?: 0
            totalGeneral += (precioBase * normales) + ((precioBase + extraCombo) * combos)
        }

        // --- Parte 2: Sumar el resto de los productos ---
        for ((nombre, cantidad) in quantities) {
            val precio = products[nombre]?.precio ?: 0.0
            totalGeneral += precio * cantidad
        }

        // --- Parte 3: Actualizar la UI ---
        val productosSeleccionados = obtenerProductosDesdeInputs()
        txtTotal.text = String.format(Locale.getDefault(), "Total: $%.2f", totalGeneral)
        mostrarResumen(productosSeleccionados, totalGeneral)
    }

    // plegables por categoría
    private fun setupCollapsibleCategories() {
        data class CategoryUi(val headerId: Int, val arrowId: Int, val stubId: Int, val gridId: Int)

        val categoryMap =
            mapOf(
                "Platillos" to
                    CategoryUi(
                        R.id.headerPlatillos,
                        R.id.arrowPlatillos,
                        R.id.stubPlatillos,
                        R.id.gridPlatillos
                    ),
                "Entradas" to
                    CategoryUi(
                        R.id.headerEntradas,
                        R.id.arrowEntradas,
                        R.id.stubEntradas,
                        R.id.gridEntradas
                    ),
                "Pambazos" to
                    CategoryUi(
                        R.id.headerPambazos,
                        R.id.arrowPambazos,
                        R.id.stubPambazos,
                        R.id.gridPambazos
                    ),
                "Guajoloyets" to
                    CategoryUi(
                        R.id.headerGuajoloyets,
                        R.id.arrowGuajoloyets,
                        R.id.stubGuajoloyets,
                        R.id.gridGuajoloyets
                    ),
                "Papas" to
                    CategoryUi(
                        R.id.headerPapas,
                        R.id.arrowPapas,
                        R.id.stubPapas,
                        R.id.gridPapas
                    ),
                "Tacos" to
                    CategoryUi(
                        R.id.headerTacos,
                        R.id.arrowTacos,
                        R.id.stubTacos,
                        R.id.gridTacos
                    ),
                "Alitas" to
                    CategoryUi(
                        R.id.headerAlitas,
                        R.id.arrowAlitas,
                        R.id.stubAlitas,
                        R.id.gridAlitas
                    ),
                "Bebidas" to
                    CategoryUi(
                        R.id.headerBebidas,
                        R.id.arrowBebidas,
                        R.id.stubBebidas,
                        R.id.gridBebidas
                    ),
                "Postres" to
                    CategoryUi(
                        R.id.headerPostres,
                        R.id.arrowPostres,
                        R.id.stubPostres,
                        R.id.gridPostres
                    ),
                "Hamburguesas" to
                    CategoryUi(
                        R.id.headerHamburguesas,
                        R.id.arrowHamburguesas,
                        R.id.stubHamburguesas,
                        R.id.gridHamburguesas
                    )
            )

        val orderedCategories =
            buildList {
                addAll(catalogConfig.categoryOrder.filter { categoryMap.containsKey(it) })
                addAll(categoryMap.keys.filterNot { contains(it) })
            }

        orderedCategories.forEach { category ->
            val ui = categoryMap[category] ?: return@forEach
            setupCollapsibleViewStub(
                findViewById(ui.headerId),
                findViewById(ui.arrowId),
                ui.stubId,
                ui.gridId,
                category
            )
        }
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
        printerTypeGroup = findViewById(R.id.printerTypeGroup)
        radioPrinter58 = findViewById(R.id.radioPrinter58)
        radioPrinter80Network = findViewById(R.id.radioPrinter80Network)
        radioPrinter58Network = findViewById(R.id.radioPrinter58Network)
        layoutPrinter80Help = findViewById(R.id.layoutPrinter80Help)
        btnPrinter80Help = findViewById(R.id.btnPrinter80Help)
        btnEditarCatalogo = findViewById(R.id.btnEditarCatalogo)

        editTextMesa = findViewById(R.id.editMesa)
        noCuenta = findViewById(R.id.noCuenta)

        summaryContainer = findViewById(R.id.summaryContainer)
        summaryTextView = findViewById(R.id.summaryTextView)
        summaryTotalTextView = findViewById(R.id.summaryTotalTextView)
        btnCloseSummary = findViewById(R.id.btnCloseSummary)

        selectedPrinterType =
                if (radioPrinter80Network.isChecked) PrinterType.NETWORK_80MM
                else if (radioPrinter58Network.isChecked) PrinterType.NETWORK_58MM
                else PrinterType.BLUETOOTH_58MM
        btnEmparejar.visibility =
                if (selectedPrinterType == PrinterType.BLUETOOTH_58MM) View.VISIBLE else View.GONE
        layoutPrinter80Help.visibility =
                if (selectedPrinterType == PrinterType.NETWORK_80MM ||
                                selectedPrinterType == PrinterType.NETWORK_58MM
                )
                        View.VISIBLE
                else View.GONE

        printerTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPrinterType =
                    when (checkedId) {
                        R.id.radioPrinter80Network -> PrinterType.NETWORK_80MM
                        R.id.radioPrinter58Network -> PrinterType.NETWORK_58MM
                        else -> PrinterType.BLUETOOTH_58MM
                    }

            btnEmparejar.visibility =
                    if (selectedPrinterType == PrinterType.BLUETOOTH_58MM) View.VISIBLE
                    else View.GONE

            layoutPrinter80Help.visibility =
                    if (selectedPrinterType == PrinterType.NETWORK_80MM ||
                                    selectedPrinterType == PrinterType.NETWORK_58MM
                    )
                            View.VISIBLE
                    else View.GONE
        }

        btnPrinter80Help.setOnClickListener {
            startActivity(Intent(this, Printer80HelpActivity::class.java))
        }

        btnEditarCatalogo.setOnClickListener {
            val intent = Intent(this, CatalogEditorActivity::class.java)
            catalogEditorLauncher.launch(intent)
        }

        btnImprimir.setOnClickListener {
            if (isPrinting) return@setOnClickListener

            // 1️⃣ Obtener los productos seleccionados desde la UI
            val productosSeleccionados = obtenerProductosDesdeInputs()
            if (productosSeleccionados.isEmpty()) {
                Toast.makeText(
                                this@MainActivity,
                                "No hay productos seleccionados",
                                Toast.LENGTH_SHORT
                        )
                        .show()
                return@setOnClickListener
            }

            // 2️⃣ Mostrar el diálogo de confirmación en lugar de imprimir directo
            showOrderSummaryDialog(productosSeleccionados)
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
                        )
                        .show()
            }
        }

        btnLimpiar.setOnClickListener { limpiarCantidades() }

        btnCloseSummary.setOnClickListener { ocultarResumen() }
    }

    // -------------------------------------------------------------------------
    // Filtro de ventas por producto
    // -------------------------------------------------------------------------

    /**
     * Configura el filtro de ventas por producto: pobla los spinners de producto y periodo,
     * muestra/oculta campos de fecha personalizados según la selección, y define la lógica para
     * calcular las ventas del producto seleccionado en el rango seleccionado.
     */
    private fun setupProductSalesFilter() {
        // Poblar spinner de productos: une nombres de hamburguesas y otros productos
        val productNames: MutableList<String> =
                mutableSetOf<String>()
                        .apply {
                            // nombres de hamburguesas (normales y combos comparten nombre base)
                            addAll(preciosHamburguesas.keys)
                            // nombres de productos generales (otros platillos y extras)
                            addAll(products.keys)
                        }
                        .toMutableList()
                        .sorted()
                        .toMutableList()

        // Adaptador para el spinner de productos
        val productoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        productoAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerProductFilter.adapter = productoAdapter

        // Adaptador para el spinner de tipo (Normal, Combo, Todos)
        val typeOptions = listOf("Todos", "Normal", "Combo")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        // Por defecto, ocultar el spinner de tipo hasta que se seleccione un producto que lo
        // requiera
        spinnerTypeFilter.visibility = View.GONE

        // Mostrar u ocultar el spinner de tipo según si el producto tiene variante combo
        spinnerProductFilter.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val selectedName = productNames[position]
                        // Si es una hamburguesa, mostrar el spinner de tipo; de lo contrario
                        // ocultarlo
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
        val periodOptions = listOf("Hoy", "Esta semana", "Este mes", "Rango específico")
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOptions)
        periodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerPeriodFilter.adapter = periodAdapter

        // Mostrar u ocultar el rango personalizado según la selección del periodo
        spinnerPeriodFilter.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
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
            val selectedProduct =
                    spinnerProductFilter.selectedItem as? String ?: return@setOnClickListener
            val selectedPeriod =
                    spinnerPeriodFilter.selectedItem as? String ?: return@setOnClickListener
            val selectedType = spinnerTypeFilter.selectedItem as? String ?: "Todos"

            // Determinar las fechas de inicio y fin según el periodo seleccionado
            val range: Pair<Long, Long>? =
                    when (selectedPeriod) {
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
                            // Obtener fechas de los EditText. Si están vacías, mostrar mensaje y
                            // regresar
                            val startStr = editStartDate.text.toString().trim()
                            val endStr = editEndDate.text.toString().trim()
                            if (startStr.isEmpty() || endStr.isEmpty()) {
                                Toast.makeText(
                                                this,
                                                "Seleccione las fechas de inicio y fin",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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
                                            )
                                            .show()
                                    return@setOnClickListener
                                }
                                Pair(getStartOfDay(startDate), getEndOfDay(endDate))
                            } catch (e: Exception) {
                                Toast.makeText(
                                                this,
                                                "Formato de fecha inválido",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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

    private fun totalPorClaves(vararg claves: String): Double {
        var total = 0.0
        for (k in claves) {
            val pd = products[k] ?: continue
            // Cantidad leída del TextView que ya administra tu flujo (+/-)
            val cantidad =
                    pd.cantidadTV?.text?.toString()?.trim()?.toIntOrNull()
                            ?: extraerNumero(
                                    pd.cantidadTV?.text?.toString()
                            ) // por si el texto es "Normales: 0"
                             ?: 0
            total += cantidad * pd.precio
        }
        return total
    }

    private fun setupCategoryProfitPanel() {
        val spCategory = findViewById<Spinner>(R.id.spinnerCategoryProfit)
        val spPeriod = findViewById<Spinner>(R.id.spinnerCategoryPeriod)
        val rangeBox = findViewById<LinearLayout>(R.id.layoutCategoryCustomRange)
        val chkCombos = findViewById<CheckBox>(R.id.checkIncludeCombos)
        val btnCalc = findViewById<MaterialButton>(R.id.btnCalcularGananciaCategoria)
        val tvResult = findViewById<TextView>(R.id.tvResultadoGananciaCategoria)

        val categorias =
                listOf(
                        "Hamburguesas",
                        "Papas",
                        "Alitas",
                        "Tacos",
                        "Bebidas",
                        "Postres",
                        "Extras",
                        "Guajoloyets",
                        "Pambazos",
                        "Pozole",
                        "Quesadillas",
                        "Chalupas",
                        "Otros"
                )
        spCategory.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

        val periodos = listOf("Hoy", "Esta semana", "Este mes", "Rango específico")
        spPeriod.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, periodos).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

        spPeriod.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        rangeBox.visibility =
                                if (periodos[position] == "Rango específico") View.VISIBLE
                                else View.GONE
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) = Unit
                }

        // DatePickers ya conectados a editCategoryStartDate / editCategoryEndDate en tu onCreate

        btnCalc.setOnClickListener {
            val categoria = spCategory.selectedItem?.toString() ?: return@setOnClickListener
            val periodo = spPeriod.selectedItem?.toString() ?: "Hoy"
            val includeCombos = if (chkCombos.isChecked) 1 else 0

            val (inicio, fin) =
                    when (periodo) {
                        "Hoy" -> {
                            val now = Date()
                            getStartOfDay(now) to getEndOfDay(now)
                        }
                        "Esta semana" -> getStartAndEndOfWeek(Date())
                        "Este mes" -> getStartAndEndOfMonth(Date())
                        else -> {
                            val startStr = editCategoryStartDate.text?.toString()?.trim().orEmpty()
                            val endStr = editCategoryEndDate.text?.toString()?.trim().orEmpty()
                            if (startStr.isEmpty() || endStr.isEmpty()) {
                                Toast.makeText(
                                                this,
                                                "Seleccione fechas de inicio y fin",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
                            }
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val d1 = sdf.parse(startStr) ?: Date()
                                val d2 = sdf.parse(endStr) ?: Date()
                                if (d1.after(d2)) {
                                    Toast.makeText(
                                                    this,
                                                    "La fecha inicio no puede ser > fin",
                                                    Toast.LENGTH_LONG
                                            )
                                            .show()
                                    return@setOnClickListener
                                }
                                getStartOfDay(d1) to getEndOfDay(d2)
                            } catch (e: Exception) {
                                Toast.makeText(
                                                this,
                                                "Formato de fecha inválido (yyyy-MM-dd)",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@setOnClickListener
                            }
                        }
                    }

            // ✅ CORRECCIÓN: El lanzamiento de la corrutina DEBE estar dentro del click listener
            // para que las variables 'categoria', 'inicio', 'fin', etc. sean accesibles.
            lifecycleScope.launch {
                val ventas =
                        withContext(Dispatchers.IO) {
                            appDatabase
                                    .orderDao()
                                    .getProfitByCategory(
                                            category = categoria,
                                            inicio = inicio,
                                            fin = fin,
                                            includeCombos = includeCombos
                                    )
                        }
                                ?: 0.0

                val detalle = buildString {
                    append("Periodo: $periodo\n")
                    append("Categoría: $categoria\n")
                    append("Incluye combos: ${if (includeCombos == 1) "Sí" else "No"}\n\n")
                    append("Ventas Brutas: $${"%.2f".format(ventas)}\n")

                    // Opcional: Si quieres mostrar ganancia real usando el margen:
                    val margen = categoryMargin[categoria] ?: 0.0
                    val gananciaReal = ventas * margen
                    append(
                            "Ganancia Est. (${(margen * 100).toInt()}%): $${
                            "%.2f".format(
                                gananciaReal
                            )
                        }"
                    )
                }
                tvResult.text = detalle
            }
        } // Cierre
    }

    /**
     * Suma de categoría general para items NO hamburguesas, usando el mapa `products`. Toma la
     * cantidad visible del TextView y el precio definido en ProductData.
     */
    private enum class Periodo {
        DIARIO,
        SEMANAL,
        MENSUAL
    }

    // Ajusta a las categorías que guardas en tu tabla `products.category`
    private val categoriasDB =
            listOf(
                    "Hamburguesas", // tus ítems de hamburguesa
                    "Papas",
                    "Alitas",
                    "Tacos",
                    "Bebidas",
                    "Postres",
                    "Extras",
                    "Guajoloyets",
                    "Pambazos",
                    "Pozole",
                    "Quesadillas",
                    "Chalupas"
            )

    // Rango de fechas por período (en millis)
    private fun rangoPara(periodo: Periodo): Pair<Long, Long> {
        val inicio = 0L // Epoch (1970)
        val fin = System.currentTimeMillis()
        return inicio to fin
    }

    private fun setupAdminGananciaPorCategoria() {
        val btnDia = findViewById<MaterialButton>(R.id.btnGananciaDiaria)
        val btnSem = findViewById<MaterialButton>(R.id.btnGananciaSemanal)
        val btnMes = findViewById<MaterialButton>(R.id.btnGananciaMensual)
        val tvSalida = findViewById<TextView>(R.id.adminSummaryTextView)

        fun lanzar(periodo: Periodo) {
            val (inicio, fin) = rangoPara(periodo)
            val includeCombos = true // cámbialo si luego expones un checkbox

            lifecycleScope.launch {
                val pares = mutableListOf<Pair<String, Double>>() // (categoria, ganancia)
                var totalGanancia = 0.0

                for (cat in categoriasDB) {
                    // 1) Ventas brutas desde Room
                    val ventas =
                            withContext(Dispatchers.IO) {
                                appDatabase
                                        .orderDao()
                                        .getProfitByCategory(
                                                category = cat,
                                                inicio = inicio,
                                                fin = fin,
                                                includeCombos = if (includeCombos) 1 else 0
                                        )
                            }
                                    ?: 0.0

                    if (ventas > 0.0) {
                        // 2) Aplica margen para obtener GANANCIA
                        val margen = categoryMargin[cat] ?: 0.0
                        val ganancia = ventas * margen
                        pares += cat to ganancia
                        totalGanancia += ganancia
                    }
                }

                val tituloPeriodo = periodo.name.lowercase().replaceFirstChar { it.uppercase() }
                val sb =
                        StringBuilder().apply {
                            append("Ganancia por categoría ($tituloPeriodo):\n\n")
                            if (pares.isEmpty()) {
                                append("No se encontraron ventas en este periodo.")
                            } else {
                                for ((cat, gan) in pares.sortedByDescending { it.second }) {
                                    append("• $cat: $${"%.2f".format(gan)}\n")
                                }
                                append("\nTOTAL GANANCIA: $${"%.2f".format(totalGanancia)}")
                            }
                        }
                tvSalida.text = sb.toString()
            }
        }

        btnDia.setOnClickListener { lanzar(Periodo.DIARIO) }
        btnSem.setOnClickListener { lanzar(Periodo.SEMANAL) }
        btnMes.setOnClickListener { lanzar(Periodo.MENSUAL) }
    }

    private fun calculateAndShowCategoryGains(
            startDate: Long,
            endDate: Long,
            includeCombos: Boolean = true
    ) {
        lifecycleScope.launch {
            val includeCombosFlag = if (includeCombos) 1 else 0
            val pares = mutableListOf<Pair<String, Double>>() // (categoria, ganancia)
            var totalGanancia = 0.0

            for (cat in categoriasDB) {
                val ventas =
                        withContext(Dispatchers.IO) {
                            appDatabase
                                    .orderDao()
                                    .getProfitByCategory(
                                            category = cat,
                                            inicio = startDate,
                                            fin = endDate,
                                            includeCombos = includeCombosFlag
                                    )
                        }
                                ?: 0.0

                if (ventas > 0.0) {
                    val margen = categoryMargin[cat] ?: 0.0
                    val ganancia = ventas * margen
                    pares += cat to ganancia
                    totalGanancia += ganancia
                }
            }

            val sb =
                    StringBuilder().apply {
                        append("Ganancia por categoría (rango personalizado):\n\n")
                        if (pares.isEmpty()) {
                            append("No se encontraron ventas en este periodo.")
                        } else {
                            for ((cat, gan) in pares.sortedByDescending { it.second }) {
                                append("• $cat: $${"%.2f".format(gan)}\n")
                            }
                            append("\nTOTAL GANANCIA: $${"%.2f".format(totalGanancia)}")
                        }
                    }

            adminSummaryTextView.text = sb.toString()
        }
    }

    // Hamburguesas = precios base (normal) y precio combo = base + extraCombo.
    // Usa `cantidadesNormales`, `cantidadesCombo`, `preciosHamburguesas`, `extraCombo`.
    private fun calcularGananciaHamburguesas(incluirCombos: Boolean): Double {
        var total = 0.0
        for ((nombre, precioBase) in preciosHamburguesas) {
            val n = (cantidadesNormales[nombre] ?: 0)
            val c = (cantidadesCombo[nombre] ?: 0)
            total += (precioBase * n)
            if (incluirCombos) total += ((precioBase + extraCombo) * c)
        }
        return total
    }

    // Intenta extraer enteros de textos como "Normales: 0", "Combos: 3", etc.
    private fun extraerNumero(texto: CharSequence?): Int? {
        if (texto == null) return null
        val m = Regex("""(\d+)""").find(texto)
        return m?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun setupDatePickersCategory() {

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // --- Selección de fecha inicio ---
        editCategoryStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                            this,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                editCategoryStartDate.setText(sdf.format(cal.time))
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                    )
                    .show()
        }

        // --- Selección de fecha fin ---
        editCategoryEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                            this,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                editCategoryEndDate.setText(sdf.format(cal.time))
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                    )
                    .show()
        }
    }

    /** Muestra un DatePickerDialog para seleccionar una fecha y la asigna al EditText objetivo. */
    private fun showDatePicker(targetEdit: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker =
                DatePickerDialog(
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
     * Calcula las ventas de un producto entre dos timestamps (inclusive) y actualiza la UI con el
     * resultado.
     */
    /**
     * Calcula las ventas de un producto (y su variante) entre dos timestamps (inclusive) y
     * actualiza la UI con el resultado.
     *
     * @param nombreProducto El nombre base del producto (por ejemplo, "Hamburguesa Hawaiana").
     * @param inicio Timestamp de inicio del rango (inclusive).
     * @param fin Timestamp de fin del rango (inclusive).
     * @param tipo Variante a filtrar: "Normal", "Combo" o "Todos". Para productos sin variante, se
     * ignora.
     */
    private fun calcularVentasProducto(
            nombreProducto: String,
            inicio: Long,
            fin: Long,
            tipo: String = "Todos"
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Obtener todas las órdenes
            val orders = appDatabase.orderDao().getAllOrders()
            var totalNormales = 0.0
            var unidadesNormales = 0
            var totalCombos = 0.0
            var unidadesCombos = 0
            // Filtrar por rango de fechas
            val filteredOrders =
                    orders.filter { order -> order.createdAt >= inicio && order.createdAt <= fin }
            // Para cada orden en el rango, sumar las ventas del producto
            for (order in filteredOrders) {
                val items = appDatabase.orderDao().getItemsForOrder(order.orderId)
                for (item in items) {
                    // Extraer el nombre base sin sufijo de combo si existe
                    val baseName =
                            if (item.esCombo) {
                                // Soporta tanto los nombres persistidos antiguos " (Combo)" como el
                                // nuevo formato " + Combo"
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
            val (unidadesVendidas, totalVentas) =
                    when (tipo) {
                        "Normal" -> unidadesNormales to totalNormales
                        "Combo" -> unidadesCombos to totalCombos
                        else -> (unidadesNormales + unidadesCombos) to (totalNormales + totalCombos)
                    }
            withContext(Dispatchers.Main) {
                // Actualizar resultado en la UI
                val resultadoTexto: String =
                        if (unidadesVendidas > 0) {
                            when (tipo) {
                                "Normal" ->
                                        "Ventas de $nombreProducto (normal): $unidadesVendidas unidad(es), Total: ${totalVentas.formatMoney()}"
                                "Combo" ->
                                        "Ventas de $nombreProducto combo: $unidadesVendidas unidad(es), Total: ${totalVentas.formatMoney()}"
                                else -> {
                                    // Mostrar resumen separado si hay ventas en ambas variantes
                                    val partes = mutableListOf<String>()
                                    if (unidadesNormales > 0) {
                                        partes.add(
                                                "Normales: $unidadesNormales unidad(es), Total: ${totalNormales.formatMoney()}"
                                        )
                                    }
                                    if (unidadesCombos > 0) {
                                        partes.add(
                                                "Combo: $unidadesCombos unidad(es), Total: ${totalCombos.formatMoney()}"
                                        )
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

    /** Calcula el timestamp de inicio y fin del día para la fecha dada. */
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

    /** Devuelve el inicio (lunes) y fin (domingo) de la semana de la fecha dada. */
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

    /** Devuelve el inicio y fin del mes de la fecha dada. */
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

    /**
     * Actualiza la cantidad para productos que NO son hamburguesas.
     * @param nombreProducto El nombre del producto a actualizar.
     * @param cambio La cantidad a sumar o restar (+1 o -1).
     */
    private fun updateQuantity(nombreProducto: String, cambio: Int) {
        // 1. Obtiene la cantidad actual del mapa 'quantities', o 0 si es la primera vez.
        val cantidadActual = quantities.getOrPut(nombreProducto) { 0 }

        // 2. Calcula la nueva cantidad, asegurando que nunca sea menor a 0.
        val nuevaCantidad = (cantidadActual + cambio).coerceAtLeast(0)

        // 3. Actualiza el mapa con la nueva cantidad.
        quantities[nombreProducto] = nuevaCantidad

        // 4. Actualiza el TextView en la interfaz de usuario.
        val productoData = products[nombreProducto]
        if (productoData?.cantidadTV is TextView) {
            (productoData.cantidadTV as TextView).text = nuevaCantidad.toString()
        } else if (productoData?.cantidadTV is EditText) {
            // Manejo especial si la vista es un EditText, como en "Chalupas"
            val editText = productoData.cantidadTV as EditText
            if (editText.text.toString() != nuevaCantidad.toString()) {
                editText.setText(nuevaCantidad.toString())
            }
        }

        syncSpecialDuplicatedProductViews(nombreProducto, nuevaCantidad)

        // 5. Llama a la función que recalcula el total de TODO y actualiza el resumen.
        requestRecalculate()
    }

    private fun limpiarCantidades() {
        quantities.keys.toList().forEach { key -> quantities[key] = 0 }

        // 🔹 1) Reiniciar todos los productos del mapa general
        for ((nombre, data) in products) {
            quantities[nombre] = 0
            data.cantidadTV.text = "0"
            updateCardAppearance(data.cantidadTV as? View, 0, nombre)
            syncSpecialDuplicatedProductViews(nombre, 0)
        }

        // 🔹 2) Reiniciar hamburguesas (normales y combos)
        for (nombre in preciosHamburguesas.keys) {
            cantidadesNormales[nombre] = 0
            cantidadesCombo[nombre] = 0
        }

        // 🔹 3) Actualizar TextViews de hamburguesas
        for ((nombre, tv) in txtNormales) {
            tv.text = "Normales: 0   Combos: 0"
            updateCardAppearance(tv, 0, nombre)
        }

        // 🔹 6) Limpiar campo de mesa y resumen. Mantener visible el resumen actual
        editTextMesa.setText("")
        summaryTextView.text = ""
        summaryTotalTextView.text = "TOTAL: $0.00"
        // No ocultamos el summaryContainer para que el resumen actual no desaparezca
        summaryContainer.visibility = View.VISIBLE

        // 🔹 7) Confirmación visual
        Toast.makeText(this, "Todas las cantidades se han restablecido a 0", Toast.LENGTH_SHORT)
                .show()
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
                            )
                            .show()
                    return@withContext
                }
            }

            // 🔹 Generar texto del ticket
            val ticketTexto = generarTextoTicket(productosSeleccionados, selectedPrinterType)

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                        .setTitle("Vista previa del ticket")
                        .setMessage(ticketTexto)
                        .setPositiveButton("Imprimir") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val printed = printWithSelectedPrinter(ticketTexto)
                                withContext(Dispatchers.Main) {
                                    if (printed) {
                                        Toast.makeText(
                                                        this@MainActivity,
                                                        "Ticket impreso correctamente",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    } else {
                                        Toast.makeText(
                                                        this@MainActivity,
                                                        "No se pudo imprimir el ticket",
                                                        Toast.LENGTH_LONG
                                                )
                                                .show()
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
        val permissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    )
                } else {
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
                }

        val allPermissionsGranted =
                permissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }

        if (!allPermissionsGranted) {
            requestBluetoothPermissionLauncher.launch(permissions)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED
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
                                )
                                .show()
                    }
                    return@withContext null
                }

                if (!adapter.isEnabled) {
                    Log.i(TAG, "Bluetooth no activado. Solicitando activación.")
                    withContext(Dispatchers.Main) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(enableBtIntent)
                        Toast.makeText(
                                        this@MainActivity,
                                        "Activando Bluetooth...",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
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
                                )
                                .show()
                    }
                    return@withContext null
                }

                // Buscar impresora cuyo nombre contenga "POS-58", "5890", etc.
                val printerDevice =
                        pairedDevices.firstOrNull { device ->
                            val name = device.name ?: ""
                            PRINTER_BT_NAMES.any { sig -> name.contains(sig, ignoreCase = true) }
                        }

                if (printerDevice == null) {
                    Log.e(TAG, "No encontré impresora tipo POS-58 entre los emparejados")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                                        this@MainActivity,
                                        "No encontré impresora POS-58 emparejada",
                                        Toast.LENGTH_LONG
                                )
                                .show()
                    }
                    return@withContext null
                }

                Log.d(
                        TAG,
                        "Intentando conectar con la impresora Bluetooth: ${printerDevice.name} [${printerDevice.address}]"
                )

                return@withContext try {
                    val socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID)
                    adapter.cancelDiscovery()
                    socket.connect()
                    Log.d(TAG, "Conexión Bluetooth establecida con ${printerDevice.name}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                                        this@MainActivity,
                                        "Conectado a ${printerDevice.name}",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
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
                    } catch (_: IOException) {}
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

                    // ESC @ -> reset impresora + centrado global si acaso
                    val initPrinter = byteArrayOf(0x1B, 0x40)

                    // Obtener logo de la app en formato ESC/POS raster bit image
                    val logoBytes = getAppIconEscPos(PrinterType.BLUETOOTH_58MM)

                    // Texto del ticket con codificación occidental
                    // (windows-1252 imprime bien ñ, acentos en muchas POS 58mm)
                    val ticketBytes = textoTicket.toByteArray(Charset.forName("windows-1252"))

                    // Alimentar papel y comando de corte (ignorado si no tiene cortador)
                    val feedAndCut = byteArrayOf(0x0A, 0x1D, 0x56, 0x42, 0x00)

                    outputStream.write(initPrinter)
                    outputStream.write(logoBytes)
                    outputStream.write(ticketBytes)
                    outputStream.write(feedAndCut)
                    outputStream.flush()

                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Error de E/S al imprimir por Bluetooth: ${e.message}", e)
                    false
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error de permisos BT al imprimir: ${e.message}", e)
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

    private fun ensureSummaryViewsInitialized() {
        if (!::summaryContainer.isInitialized) {
            summaryContainer = findViewById(R.id.summaryContainer)
        }
        if (!::summaryTextView.isInitialized) {
            summaryTextView = findViewById(R.id.summaryTextView)
        }
        if (!::summaryTotalTextView.isInitialized) {
            summaryTotalTextView = findViewById(R.id.summaryTotalTextView)
        }
        if (!::btnCloseSummary.isInitialized) {
            btnCloseSummary = findViewById(R.id.btnCloseSummary)
        }
    }

    private fun mostrarResumen(productos: List<Producto>, precomputedTotal: Double? = null) {
        ensureSummaryViewsInitialized()

        val sb = StringBuilder()
        var totalGeneral = precomputedTotal ?: 0.0
        val shouldAccumulateTotal = precomputedTotal == null

        // Separar normales vs combos (requiere Producto.esCombo)
        val combos = mutableListOf<Producto>()
        val normales = mutableListOf<Producto>()
        for (p in productos) if (p.esCombo) combos.add(p) else normales.add(p)

        if (normales.isNotEmpty()) {
            sb.appendLine(" PRODUCTOS")
            for (p in normales) {
                val total = p.cantidad * p.precio
                if (shouldAccumulateTotal) totalGeneral += total
                sb.appendLine("${p.cantidad} x ${p.nombre} ... $${"%.2f".format(total)}")
            }
            sb.appendLine("-----------------------------------")
        }

        if (combos.isNotEmpty()) {
            sb.appendLine(" COMBOS")
            for (c in combos) {
                val total = c.cantidad * c.precio
                if (shouldAccumulateTotal) totalGeneral += total
                sb.appendLine("${c.cantidad} x ${c.nombre} ... $${"%.2f".format(total)}")
            }
            sb.appendLine("-----------------------------------")
        }

        // Mostrar número de cuenta si aplica (ANTES de pintar el TextView)
        if (noCuenta.isChecked) {
            sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)}")
        }

        // Pintar
        this.summaryTextView.text = sb.toString()
        this.summaryTotalTextView.text = "TOTAL: $${"%.2f".format(totalGeneral)}"
        this.summaryContainer.visibility = View.VISIBLE

        // Botón cerrar
        this.btnCloseSummary.setOnClickListener { this.summaryContainer.visibility = View.GONE }
    }

    private fun mostrarResumenDeOrdenGuardada(order: OrderEntity) {
        // 1. Usar una corrutina para acceder a la base de datos en un hilo secundario
        lifecycleScope.launch(Dispatchers.IO) {
            // 2. Obtener la lista de artículos para la orden seleccionada
            val itemsDeLaOrden = appDatabase.orderDao().getItemsForOrder(order.orderId)

            val productosParaResumen =
                    itemsDeLaOrden.map { itemEntity ->
                        Producto(
                                nombre = itemEntity.name,
                                precio = itemEntity.unitPrice,
                                cantidad = itemEntity.quantity,
                                esCombo = itemEntity.esCombo
                        )
                    }

            // 3. Volver al hilo principal para mostrar el diálogo
            withContext(Dispatchers.Main) {
                // 4. Llamar a la función que ya sabe cómo mostrar un resumen, pero con la lista
                // convertida
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

        // 🔹 2) Hamburguesas normales
        for ((nombre, cantidad) in cantidadesNormales) {
            if (cantidad > 0) {
                val precio = preciosHamburguesas[nombre] ?: 0.0
            val displayName = getHamburguesaDisplayName(nombre)
                lista.add(
                        Producto(
                    nombre = displayName,
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
            val displayName = getHamburguesaDisplayName(nombre)
                lista.add(
                        Producto(
                                // Utilizar "+ Combo" en lugar de paréntesis para unificar el nombre
                    nombre = "$displayName + Combo",
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
                            )
                            .show()
                }
                return@launch
            }

            // 🔹 Convertir productos a entidades para guardar en BD
            val comboItems =
                    combos.map {
                        OrderItemEntity(
                                orderId = 0L, // se asignará automáticamente al insertar
                                name = it.nombre,
                                unitPrice = it.precio,
                                quantity = quantities[it.nombre.substringBefore(" (x")]
                                                ?: 1, // cantidad desde UI
                                esCombo = true
                        )
                    }

            // 🔹 Crear la orden (solo combos)
            val order =
                    OrderEntity(
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
                        )
                        .show()
            }
        }
    }
    // -------------------------------------------------------------------------
    // Impresión ESC/POS: Logos y Texto
    // -------------------------------------------------------------------------

    /** Convierte el ícono vectorial de la app a un comando ESC/POS raster bit image (GS v 0). */
    private fun getAppIconEscPos(printerType: PrinterType): ByteArray {
        val drawable =
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.pambazo)
                        ?: return ByteArray(0)

        return if (printerType == PrinterType.NETWORK_80MM) {
            // Impresora 80mm: ~576 dots imprimibles. Usamos 512×256 para llenar el ancho
            // del ticket sin sacrificar nitidez (el bitmap se escala antes de codificar ESC/POS).
            getEscPosImageWithEscStar(drawable, width = 512, height = 256)
        } else {
            // 58mm: usar ancho completo del ticket y centrar dentro del bitmap evita
            // desplazamientos laterales en modelos que interpretan irregular ESC a 1.
            getEscPosImageWithGsV0(
                    drawable,
                    width = 384,
                    height = 128,
                    usePrinterCenter = false
            )
        }
    }

    private fun drawableToBitmap(
            drawable: android.graphics.drawable.Drawable,
            width: Int,
            height: Int
    ): android.graphics.Bitmap {
        val bitmap =
                android.graphics.Bitmap.createBitmap(
                        width,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                )
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

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
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luminance = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        return luminance < 128
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

        val result =
                ByteArray(
                alignStart.size + header.size + imageData.size + feed.size + alignLeft.size
                )
        var offset = 0
        System.arraycopy(alignStart, 0, result, offset, alignStart.size)
        offset += alignStart.size
        System.arraycopy(header, 0, result, offset, header.size)
        offset += header.size
        System.arraycopy(imageData, 0, result, offset, imageData.size)
        offset += imageData.size
        System.arraycopy(feed, 0, result, offset, feed.size)
        offset += feed.size
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

        // En ESC * 24-dot (m=33), nL/nH representan columnas (dots), no bytes.
        // Si se envía el valor en bytes, la impresora interpreta parte de la imagen como texto.
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
                        val pixelOn =
                                if (yy < height && isDarkPixel(bitmap.getPixel(x, yy))) 1 else 0
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

    // Construcción del ticket ESC/POS (solo texto)
    private suspend fun generarTextoTicket(
            productosSeleccionados: List<Producto>,
            printerType: PrinterType
    ): String =
            withContext(Dispatchers.IO) {
            val ticketProfile = resolveTicketProfile(printerType)
                val fechaHora =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val sb = StringBuilder()
                val anchoTotalLinea = if (ticketProfile == PrinterType.NETWORK_80MM) 48 else 32
                val lineaSeparadora = "-".repeat(anchoTotalLinea)

                // ── Encabezado ────────────────────────────────────────────
                // ESC/POS: centrar + negrita + doble tamaño para el nombre
                val ESC = "\u001B"
                val GS = "\u001D"
                val centerOn = "$ESC\u0061\u0001" // ESC a 1  → centrar
                val boldOn = "$ESC\u0045\u0001" // ESC E 1  → negrita ON
                val sizeXL = "$GS\u0021\u0011" // GS ! 0x11 → doble ancho + doble alto
                val sizeWide = "$GS\u0021\u0010" // GS ! 0x10 → doble ancho, alto normal
                val sizeNorm =
                    "$GS\u0021\u0000" // GS ! 0x00 → revertido a tamaño verdaderamente normal
                // para no romper columnas
                val boldOff = "$ESC\u0045\u0000" // ESC E 0  → negrita OFF
                val centerOff = "$ESC\u0061\u0000" // ESC a 0  → alinear izquierda

                val headerSize =
                    if (ticketProfile == PrinterType.NETWORK_80MM) sizeXL else sizeWide
                val headerCharSpacing = "$GS\u0020\u0000"
                // GS SP n: espaciado extra entre chars para el cuerpo del ticket en 58mm.
                val bodyCharSpacing =
                    if (ticketProfile == PrinterType.NETWORK_80MM) "$GS\u0020\u0000"
                    else "$GS\u0020\u000E"
                val charSpacingReset = "$GS\u0020\u0000" // GS SP  0 → normal

                val is58Profile = ticketProfile != PrinterType.NETWORK_80MM
                if (is58Profile) {
                    // En varias 58mm, ESC a 1 desplaza visualmente a la derecha con fuentes anchas.
                    // Centramos por padding en modo left para mantener el bloque realmente centrado.
                    sb.append(centerOff)
                } else {
                    sb.append(centerOn)
                }
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

                // Datos de cuenta si aplica
                if (noCuenta.isChecked) {
                    sb.appendLine("No. de cuenta: ${getString(R.string.cuenta)} ")
                    sb.appendLine("Nombre: Margarita Daniel Pérez")
                    sb.appendLine("Banco: BBVA")
                }

                // Mesa/cliente
                val mesaInfo = editTextMesa.text.toString().trim()
                if (mesaInfo.isNotEmpty()) {
                    sb.appendLine(
                            String.format("%-${anchoTotalLinea}s", "Mesa: ${mesaInfo.uppercase()}")
                    )
                    sb.appendLine("*****************************")
                }

                // Obtener productos seleccionados desde tus inputs (debe devolver List<Producto>)

                // Guardar orden en BD y mostrar resumen en el layou

                // Separación normales vs combos
                val combos = mutableListOf<Producto>()
                val normales = mutableListOf<Producto>()
                for (p in productosSeleccionados) if (p.esCombo) combos.add(p) else normales.add(p)

                // Tabla cabecera
                sb.appendLine(lineaSeparadora)
                val productColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 23 else 13
                val priceColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 8 else 6
                val qtyColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 5 else 3
                val amountColWidth = if (ticketProfile == PrinterType.NETWORK_80MM) 9 else 7
                sb.appendLine(
                        String.format(
                                "%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}s %${amountColWidth}s",
                                "Producto",
                                "Precio",
                                "Cant",
                                "Total"
                        )
                )
                sb.appendLine(lineaSeparadora)

                var totalGeneral = 0.0

                // Listado de productos normales
                if (normales.isNotEmpty()) {
                    sb.appendLine("PRODUCTOS")
                    for (p in normales) {
                        val totalProducto = p.precio * p.cantidad
                        totalGeneral += totalProducto

                        val nombreVisible = (productColWidth - 3).coerceAtLeast(5)
                        val nombreCorto =
                                if (p.nombre.length > productColWidth)
                                        p.nombre.substring(0, nombreVisible) + "..."
                                else p.nombre
                        val precioFmt = String.format("$%.2f", p.precio)
                        val totalFmt = String.format("$%.2f", totalProducto)

                        sb.appendLine(
                                String.format(
                                        "%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}d %${amountColWidth}s",
                                        nombreCorto,
                                        precioFmt,
                                        p.cantidad,
                                        totalFmt
                                )
                        )
                    }
                    sb.appendLine(lineaSeparadora)
                }

                // Listado de combos
                if (combos.isNotEmpty()) {
                    sb.appendLine("COMBOS")
                    for (c in combos) {
                        val totalCombo = c.precio * c.cantidad
                        totalGeneral += totalCombo

                        val nombreVisible = (productColWidth - 3).coerceAtLeast(5)
                        val nombreCorto =
                                if (c.nombre.length > productColWidth)
                                        c.nombre.substring(0, nombreVisible) + "..."
                                else c.nombre
                        val precioFmt = String.format("$%.2f", c.precio)
                        val totalFmt = String.format("$%.2f", totalCombo)

                        sb.appendLine(
                                String.format(
                                        "%-${productColWidth}s %${priceColWidth}s %${qtyColWidth}d %${amountColWidth}s",
                                        nombreCorto,
                                        precioFmt,
                                        c.cantidad,
                                        totalFmt
                                )
                        )
                    }
                    sb.appendLine(lineaSeparadora)
                }

                // Totales
                val totalValueWidth = (anchoTotalLinea - 15).coerceAtLeast(8)
                sb.appendLine(
                        String.format(
                                "%-15s %${totalValueWidth}s",
                                "TOTAL:",
                                String.format("$%.2f", totalGeneral)
                        )
                )
                sb.appendLine(lineaSeparadora)
                sb.appendLine("")
                sb.appendLine("    Gracias por su compra")
                sb.appendLine("    Vuelva pronto")

                return@withContext sb.toString()
            }

    // -------------------------------------------------------------------------
    // Persistencia en base de datos y panel admin
    // -------------------------------------------------------------------------

    /** Helper: formato money */
    private fun Double.formatMoney(): String = "$" + String.format("%.2f", this)

    /** Actualiza lista en panel admin */
    private fun refreshOrders(order: OrderEntity, items: List<OrderItemEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 🔹 Inserta la orden con sus ítems
            appDatabase.orderDao().insertOrderWithItems(order, items)

            // 🔹 Luego obtiene todas las órdenes actualizadas
            val updatedOrders = appDatabase.orderDao().getAllOrders()

            withContext(Dispatchers.Main) { adminOrderAdapter.updateOrders(updatedOrders) }
        }
    }

    /** Guarda la orden en Room y refresca el panel */
    /** Guarda la orden en Room y refresca el panel */
    /** Guarda la orden en Room. No se encarga de actualizar la UI directamente. */
    private fun guardarOrden(productos: List<Producto>, mesaInfo: String) {
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay productos para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val editingId = currentEditingOrderId
            if (editingId != null) {
                // ACTUALIZAR ORDEN EXISTENTE: borrar todos sus ítems y reemplazarlos
                // con la lista completa actual (originales ya cargados + nuevos).
                // Esto garantiza que no haya duplicados ni ítems huérfanos.
                appDatabase.orderDao().deleteItemsForOrder(editingId)

                val allItems =
                        productos.map {
                            OrderItemEntity(
                                    orderId = editingId,
                                    name = it.nombre,
                                    unitPrice = it.precio,
                                    quantity = it.cantidad,
                                    esCombo = it.esCombo
                            )
                        }
                appDatabase.orderDao().insertOrderItems(allItems)

                // Recalcular total desde los ítems recién insertados
                val newGrandTotal = allItems.sumOf { it.quantity * it.unitPrice }
                appDatabase.orderDao().updateOrderTotal(editingId, newGrandTotal)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✅ Pedido actualizado", Toast.LENGTH_SHORT)
                            .show()
                    currentEditingOrderId = null
                    limpiarCantidades()
                }
            } else {
                // NUEVA ORDEN
                val createdAt = System.currentTimeMillis()
                // ... (tu lógica para businessDate, etc.) ...

                val orderEntity =
                        OrderEntity(
                                mesa = mesaInfo.ifBlank { null },
                                createdAt = createdAt,
                                businessDate = createdAt, // o tu variable businessDate
                                grandTotal = productos.sumOf { it.total }
                        )

                val items =
                        productos.map {
                            OrderItemEntity(
                                    // itemId se genera automáticamente
                                    orderId = 0, // se rellena en la transacción
                                    name = it.nombre,
                                    unitPrice = it.precio,
                                    quantity = it.cantidad,
                                    esCombo =
                                            it.esCombo // Asegúrate de que tu OrderItemEntity tenga
                                    // este
                                    // campo
                                    )
                        }

                // Inserta en la BD. El observador se encargará del resto.
                appDatabase.orderDao().insertOrderWithItems(orderEntity, items)
            }
        }
    }

    /**
     * Observa la tabla de órdenes y actualiza el adaptador cada vez que hay un cambio. Esta es la
     * ÚNICA forma en que el adaptador debe recibir datos.
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
            val total =
                    when (r) {
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
        val calendar = Calendar.getInstance()
        DatePickerDialog(
                        this,
                        { _, year, month, dayOfMonth ->
                            val selectedCal =
                                    Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                            val startOfDay = selectedCal.timeInMillis
                            // End of day
                            selectedCal.set(Calendar.HOUR_OF_DAY, 23)
                            selectedCal.set(Calendar.MINUTE, 59)
                            selectedCal.set(Calendar.SECOND, 59)
                            val endOfDay = selectedCal.timeInMillis

                            lifecycleScope.launch(Dispatchers.IO) {
                                val orders =
                                        appDatabase
                                                .orderDao()
                                                .getOrdersBetween(startOfDay, endOfDay)
                                val total = orders.sumOf { it.grandTotal }
                                val count = orders.size

                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dateStr = sdf.format(Date(startOfDay))

                                val texto =
                                        "Ganancias del día ($dateStr):\n\n" +
                                                "Órdenes: $count\n" +
                                                "Total: ${total.formatMoney()}"

                                withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                )
                .show()
    }

    /** Ganancia semanal */
    private fun generarGananciaSemanal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados = appDatabase.orderDao().getWeeklySales()
            val texto =
                    buildResumen(resultados) { r ->
                        "Semana ${r.week}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
                    }
            withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
        }
    }

    /** Ganancia mensual */
    private fun generarGananciaMensual() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultados = appDatabase.orderDao().getMonthlySales()
            val texto =
                    buildResumen(resultados) { r ->
                        "Mes ${r.month}: ${r.ordersCount} órdenes, ${r.totalSales.formatMoney()}"
                    }
            withContext(Dispatchers.Main) { adminSummaryTextView.text = texto }
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

    /**
     * Elimina un pedido y deja que el observador de la base de datos actualice la UI
     * automáticamente.
     */
    private fun eliminarPedido(orderId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Elimina la orden de la base de datos en un hilo secundario.
            //    La llave foránea con onDelete = CASCADE se encargará de borrar sus items.
            appDatabase.orderDao().deleteOrderById(orderId)

            // 2. Muestra un mensaje de confirmación en el hilo principal.
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Pedido eliminado", Toast.LENGTH_SHORT).show()
            }
            // ❌ NO se necesita llamar a refreshOrders() o updateOrders() aquí.
            //    El observador (getAllOrdersFlow().collect) se activará solo
            //    y le pasará la nueva lista (sin el pedido borrado) al adaptador.
        }
    }

    /**
     * Reimprime el ticket de una orden existente. Utiliza los ítems almacenados en la base de datos
     * para reconstruir el ticket, en lugar de leer la UI actual. Muestra un diálogo de confirmación
     * previo a la impresión.
     */
    private fun reimprimirTicket(order: OrderEntity) {
        // Utiliza una rutina de corrutina para recuperar los ítems y construir el ticket
        lifecycleScope.launch(Dispatchers.IO) {
            // Obtén los items de la orden desde la base de datos
            val items = appDatabase.orderDao().getItemsForOrder(order.orderId)

            // Construye una lista de Producto a partir de los items guardados
            val productos =
                    items.map {
                        Producto(
                                nombre = it.name,
                                precio = it.unitPrice,
                                cantidad = it.quantity,
                                esCombo = it.esCombo
                        )
                    }

            // Genera el texto del ticket con los productos recuperados
            val ticket = generarTextoTicket(productos, selectedPrinterType)

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
                val alertDialog =
                        AlertDialog.Builder(this@MainActivity)
                                .setView(dialogView)
                                .setCancelable(true)
                                .create()

                // Al imprimir, ejecuta la impresión en un hilo de fondo y cierra el diálogo
                btnPrint.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) { printWithSelectedPrinter(ticket) }
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

    // --- Lógica del Panel "Top Selling Products" ---
    private fun setupTopSellingPanel() {
        val btnDia = findViewById<MaterialButton>(R.id.btnGananciaDiaria)
        val btnSem = findViewById<MaterialButton>(R.id.btnGananciaSemanal)
        val btnMes = findViewById<MaterialButton>(R.id.btnGananciaMensual)
        val tvSalida = findViewById<TextView>(R.id.adminSummaryTextView)

        fun lanzar(periodo: Periodo) {
            val (inicio, fin) = rangoPara(periodo)

            lifecycleScope.launch {
                val topProducts =
                        withContext(Dispatchers.IO) {
                            appDatabase.orderDao().getTopSellingProducts(inicio, fin)
                        }

                val tituloPeriodo = periodo.name.lowercase().replaceFirstChar { it.uppercase() }
                val sb =
                        StringBuilder().apply {
                            append("Productos Más Vendidos ($tituloPeriodo):\n\n")
                            if (topProducts.isEmpty()) {
                                append("No se encontraron ventas en este periodo.")
                            } else {
                                topProducts.forEachIndexed { index, p ->
                                    append("${index + 1}. ${p.name}: ${p.totalQty} und.\n")
                                }
                            }
                        }
                tvSalida.text = sb.toString()
            }
        }

        btnDia.setOnClickListener { lanzar(Periodo.DIARIO) }
        btnSem.setOnClickListener { lanzar(Periodo.SEMANAL) }
        btnMes.setOnClickListener { lanzar(Periodo.MENSUAL) }
    }

    // --- Helpers de Order History (Status y Add Items) ---

    private fun onOrderToggleStatus(orderId: Long, isCompleted: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.orderDao().updateOrderStatus(orderId, isCompleted)
        }
    }

    private fun onOrderAddItem(order: OrderEntity) {
        AlertDialog.Builder(this)
                .setTitle("✏️ Editar pedido")
                .setMessage(
                        "Los productos del pedido de la Mesa ${order.mesa ?: "N/A"} " +
                                "se cargarán en la pantalla.\n\n" +
                                "Agrega o modifica los productos y presiona " +
                                "'COBRAR / GUARDAR' para actualizar el pedido."
                )
                .setPositiveButton("Editar") { _, _ -> enableEditMode(order) }
                .setNegativeButton("Cancelar", null)
                .show()
    }

    private fun enableEditMode(order: OrderEntity) {
        quantities.keys.toList().forEach { key -> quantities[key] = 0 }

        // 1. Limpiar selección actual sin mostrar el Toast de "cantidades restablecidas"
        for ((nombre, data) in products) {
            quantities[nombre] = 0
            data.cantidadTV.text = "0"
            updateCardAppearance(data.cantidadTV as? View, 0, nombre)
        }
        for (nombre in preciosHamburguesas.keys) {
            cantidadesNormales[nombre] = 0
            cantidadesCombo[nombre] = 0
        }
        for ((nombre, tv) in txtNormales) {
            tv.text = "Normales: 0   Combos: 0"
            updateCardAppearance(tv, 0, nombre)
        }

        // 2. Setear flag de edición
        currentEditingOrderId = order.orderId

        // 3. Cargar ítems existentes de la orden en la UI
        lifecycleScope.launch(Dispatchers.IO) {
            val existingItems = appDatabase.orderDao().getItemsForOrder(order.orderId)

            withContext(Dispatchers.Main) {
                var itemsLoaded = 0
                for (item in existingItems) {
                    val rawName = item.name.trim()
                    val qty = item.quantity
                    if (qty <= 0) continue

                    val comboBaseName =
                        rawName.removeSuffix(" + Combo").removeSuffix(" (Combo)").trim()
                    val resolvedHamburguesaName =
                        resolveHamburguesaSourceName(comboBaseName) ?: comboBaseName
                    val isComboName =
                            rawName.endsWith(" + Combo", ignoreCase = true) ||
                                    rawName.endsWith(" (Combo)", ignoreCase = true)

                    if ((item.esCombo || isComboName) && preciosHamburguesas.containsKey(resolvedHamburguesaName)) {
                        // Hamburguesa en combo
                    cantidadesCombo[resolvedHamburguesaName] =
                        (cantidadesCombo[resolvedHamburguesaName] ?: 0) + qty
                    txtNormales[resolvedHamburguesaName]?.let { tv ->
                        val n = cantidadesNormales[resolvedHamburguesaName] ?: 0
                        val c = cantidadesCombo[resolvedHamburguesaName] ?: 0
                            tv.text = "Normales: $n   Combos: $c"
                        updateCardAppearance(tv, n + c, resolvedHamburguesaName)
                        }
                        itemsLoaded++
                    } else if (preciosHamburguesas.containsKey(resolvedHamburguesaName)) {
                        // Hamburguesa normal
                    cantidadesNormales[resolvedHamburguesaName] =
                        (cantidadesNormales[resolvedHamburguesaName] ?: 0) + qty
                    txtNormales[resolvedHamburguesaName]?.let { tv ->
                        val n = cantidadesNormales[resolvedHamburguesaName] ?: 0
                        val c = cantidadesCombo[resolvedHamburguesaName] ?: 0
                            tv.text = "Normales: $n   Combos: $c"
                        updateCardAppearance(tv, n + c, resolvedHamburguesaName)
                        }
                        itemsLoaded++
                    } else {
                        // Producto genérico (no hamburguesa), con soporte de nombres legacy.
                        val resolvedName = resolveExistingProductName(rawName)
                        if (resolvedName != null) {
                            quantities[resolvedName] = (quantities[resolvedName] ?: 0) + qty
                            val totalQty = quantities[resolvedName] ?: 0
                            val data = products[resolvedName]
                            if (data != null) {
                                data.cantidadTV.text = totalQty.toString()
                                updateCardAppearance(data.cantidadTV as? View, totalQty, resolvedName)
                                syncSpecialDuplicatedProductViews(resolvedName, totalQty)
                            }
                            itemsLoaded++
                        } else {
                            Log.w(
                                    TAG,
                                    "No se pudo mapear el producto '${item.name}' al catálogo actual al editar pedido"
                            )
                        }
                    }
                }

                // Restaurar mesa
                order.mesa?.let { editTextMesa.setText(it) }

                // Recalcular totales y resumen
                requestRecalculate()

                // Cerrar el drawer para que el usuario vea la pantalla de selección
                drawerLayout.closeDrawers()

                Toast.makeText(
                                this@MainActivity,
                                "✏️ Editando pedido Mesa ${order.mesa ?: "N/A"} · $itemsLoaded producto(s) cargado(s)",
                                Toast.LENGTH_LONG
                        )
                        .show()
            }
        }
    }

    // --- ViewStub Lazy Loading Helpers ---

    private fun setupCollapsibleViewStub(
            header: View,
            arrow: ImageView,
            stubId: Int,
            inflatedId: Int,
            category: String
    ) {
        header.setOnClickListener {
            // Check if already inflated
            val inflatedView = findViewById<ViewGroup>(inflatedId)
            if (inflatedView != null) {
                // Toggle
                val expand = inflatedView.visibility == View.GONE
                inflatedView.visibility = if (expand) View.VISIBLE else View.GONE
                arrow.rotateArrow(expand)
            } else {
                // Inflate
                val stub = findViewById<ViewStub>(stubId)
                if (stub != null) {
                    val newView = stub.inflate()
                    arrow.rotateArrow(true)
                    initializeCategoryProducts(category, newView)
                }
            }
        }
    }

    private fun applyCatalogOverridesToLoadedProducts() {
        val renames = mutableListOf<Pair<String, String>>()
        for ((sourceName, override) in catalogConfig.productOverrides) {
            val current = products[sourceName] ?: continue
            val baseConfig = CatalogConfigStore.defaultProducts.firstOrNull { it.sourceName == sourceName }
            val shouldLockName = baseConfig?.category.equals("Hamburguesas", ignoreCase = true)
            val newName =
                if (shouldLockName) {
                    sourceName
                } else {
                    override.name.trim().ifBlank { sourceName }
                }
            val newPrice = if (override.price > 0.0) override.price else current.precio

            products.remove(sourceName)
            products[newName] = ProductData(current.cantidadTV, current.btnMenos, current.btnMas, newPrice)

            val qty = quantities.remove(sourceName) ?: 0
            quantities[newName] = qty

            val color = productColors.remove(sourceName)
            if (color != null) {
                productColors[newName] = color
            }

            if (sourceName != newName) {
                renames.add(sourceName to newName)
            }
        }

        if (renames.isNotEmpty()) {
            renameVisibleProductLabels(renames)
        }
    }

    private fun renameVisibleProductLabels(renames: List<Pair<String, String>>) {
        val renameMap = renames.toMap()

        fun walk(view: View?) {
            if (view == null) return
            if (view is TextView) {
                val original = view.text?.toString() ?: ""
                val updated = renameMap[original]
                if (updated != null) {
                    view.text = updated
                }
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    walk(view.getChildAt(i))
                }
            }
        }

        walk(findViewById(android.R.id.content))
    }

    private fun addCustomProductsForCategory(category: String, root: View) {
        val rootGroup = root as? ViewGroup ?: return
        val customForCategory =
                catalogConfig.customProducts.filter {
                    it.category.equals(category, ignoreCase = true) && it.name.isNotBlank() && it.price > 0.0
                }

        customForCategory.forEach { configProduct ->
            if (products.containsKey(configProduct.name)) return@forEach

            val itemView =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_custom_product, rootGroup, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvProductName)
            val tvQty = itemView.findViewById<TextView>(R.id.tvCantidad)
            val btnMenos = itemView.findViewById<Button>(R.id.btnMenos)
            val btnMas = itemView.findViewById<Button>(R.id.btnMas)

            tvName.text = configProduct.name
            tvQty.text = "0"

            products[configProduct.name] =
                    ProductData(
                            cantidadTV = tvQty,
                            btnMenos = btnMenos,
                            btnMas = btnMas,
                            precio = configProduct.price
                    )
            quantities.putIfAbsent(configProduct.name, 0)

            if (rootGroup is GridLayout) {
                itemView.layoutParams =
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(12, 12, 12, 12)
                    }
            }

            rootGroup.addView(itemView)
        }
    }

    private fun initializeCategoryProducts(category: String, root: View) {
        when (category) {
            "Platillos" -> {

                products["Quesadillas"] =
                        ProductData(
                                root.findViewById<TextView>(R.id.cantidadQuesadillas),
                                root.findViewById<Button>(R.id.btnMenosQuesadillas),
                                root.findViewById<Button>(R.id.btnMasQuesadillas),
                                30.0
                        )
                products["Tostadas"] =
                        ProductData(
                                root.findViewById<TextView>(R.id.cantidadTostadas),
                                root.findViewById<Button>(R.id.btnMenosTostadas),
                                root.findViewById<Button>(R.id.btnMasTostadas),
                                35.0
                        )
                products["Chalupas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadChalupas),
                                root.findViewById(R.id.btnMenosChalupas),
                                root.findViewById(R.id.btnMasChalupas),
                                5.0
                        )
                products["Volcanes"] =
                        ProductData(
                                root.findViewById(R.id.cantidadVolcanes),
                                root.findViewById(R.id.btnMenosVolcanes),
                                root.findViewById(R.id.btnMasVolcanes),
                                60.0
                        )

                products["Volcan Queso/Guisado Extra"] =
                        ProductData(
                                root.findViewById(R.id.cantidadGuajolotaExtra),
                                root.findViewById(R.id.btnMenosGuajolotaExtra),
                                root.findViewById(R.id.btnMasGuajolotaExtra),
                                72.0
                        )
                val pozoleGrandeTv = root.findViewById<TextView>(R.id.cantidadPozoleGrande)
                val pozoleChicoTv = root.findViewById<TextView>(R.id.cantidadPozoleChico)

                products["Pozole Grande"] =
                    ProductData(
                        pozoleGrandeTv,
                        root.findViewById(R.id.btnMenosPozoleGrande),
                        root.findViewById(R.id.btnMasPozoleGrande),
                        110.0
                    )
                products["Pozole Chico"] =
                    ProductData(
                        pozoleChicoTv,
                        root.findViewById(R.id.btnMenosPozoleChico),
                        root.findViewById(R.id.btnMasPozoleChico),
                        90.0
                    )
                registerDuplicatedTextView(pozoleGrandeTv)
                registerDuplicatedTextView(pozoleChicoTv)
            }
            "Pambazos" -> {
                products["Pambazos Naturales"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosNaturales),
                                root.findViewById(R.id.btnMenosPambazosNaturales),
                                root.findViewById(R.id.btnMasPambazosNaturales),
                                35.0
                        )
                products["Pambazos Naturales Combinados"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosNaturalesCombinados),
                                root.findViewById(R.id.btnMenosPambazosNaturalesCombinados),
                                root.findViewById(R.id.btnMasPambazosNaturalesCombinados),
                                42.0
                        )
                products["Pambazos Naturales Combinados con Queso"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosNaturalesCombinadosQueso),
                                root.findViewById(R.id.btnMenosPambazosNaturalesCombinadosQueso),
                                root.findViewById(R.id.btnMasPambazosNaturalesCombinadosQueso),
                                54.0
                        )
                products["Pambazos Naturales Extra"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosNaturalesQueso),
                                root.findViewById(R.id.btnMenosPambazosNaturalesQueso),
                                root.findViewById(R.id.btnMasPambazosNaturalesQueso),
                                47.0
                        )
                products["Pambazos Adobados"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosAdobados),
                                root.findViewById(R.id.btnMenosPambazosAdobados),
                                root.findViewById(R.id.btnMasPambazosAdobados),
                                40.0
                        )
                products["Pambazos Adobados Combinados"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosAdobadosCombinados),
                                root.findViewById(R.id.btnMenosPambazosAdobadosCombinados),
                                root.findViewById(R.id.btnMasPambazosAdobadosCombinados),
                                47.0
                        )
                products["Pambazos Adobados Combinados con Queso"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosAdobadosCombinadosQueso),
                                root.findViewById(R.id.btnMenosPambazosAdobadosCombinadosQueso),
                                root.findViewById(R.id.btnMasPambazosAdobadosCombinadosQueso),
                                59.0
                        )
                products["Pambazos Adobados Extra"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPambazosAdobadosExtra),
                                root.findViewById(R.id.btnMenosPambazosAdobadosExtra),
                                root.findViewById(R.id.btnMasPambazosAdobadosExtra),
                                52.0
                        )
                val pozoleGrandeTv = root.findViewById<TextView>(R.id.cantidadPozoleGrande)
                val pozoleChicoTv = root.findViewById<TextView>(R.id.cantidadPozoleChico)

                products["Pozole Grande"] =
                    ProductData(
                        pozoleGrandeTv,
                        root.findViewById(R.id.btnMenosPozoleGrande),
                        root.findViewById(R.id.btnMasPozoleGrande),
                        110.0
                    )
                products["Pozole Chico"] =
                    ProductData(
                        pozoleChicoTv,
                        root.findViewById(R.id.btnMenosPozoleChico),
                        root.findViewById(R.id.btnMasPozoleChico),
                        90.0
                    )
                registerDuplicatedTextView(pozoleGrandeTv)
                registerDuplicatedTextView(pozoleChicoTv)
            }
            "Entradas" -> {

                products["Alones"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAlones),
                                root.findViewById(R.id.btnMenosAlones),
                                root.findViewById(R.id.btnMasAlones),
                                25.0
                        )
                products["Mollejas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadMollejas),
                                root.findViewById(R.id.btnMenosMollejas),
                                root.findViewById(R.id.btnMasMollejas),
                                25.0
                        )
                products["Higados"] =
                        ProductData(
                                root.findViewById(R.id.cantidadHigados),
                                root.findViewById(R.id.btnMenosHigados),
                                root.findViewById(R.id.btnMasHigados),
                                22.0
                        )
                products["Patitas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPatitas),
                                root.findViewById(R.id.btnMenosPatitas),
                                root.findViewById(R.id.btnMasPatitas),
                                22.0
                        )
                products["Huevos"] =
                        ProductData(
                                root.findViewById(R.id.cantidadHuevos),
                                root.findViewById(R.id.btnMenosHuevos),
                                root.findViewById(R.id.btnMasHuevos),
                                20.0
                        )
            }
            "Guajoloyets" -> {
                products["Guajoloyets Naturales"] =
                        ProductData(
                                root.findViewById(R.id.cantidadGuajoloyetNatural),
                                root.findViewById(R.id.btnMenosGuajoloyetNatural),
                                root.findViewById(R.id.btnMasGuajoloyetNatural),
                                60.0
                        )
                products["Guajoloyets Naturales Extra"] =
                        ProductData(
                                root.findViewById(R.id.cantidadGujoloyetNaturalExtra),
                                root.findViewById(R.id.btnMenosGujoloyetNaturalExtra),
                                root.findViewById(R.id.btnMasGujoloyetNaturalExtra),
                                72.0
                        )
                products["Guajoloyets Adobados"] =
                        ProductData(
                                root.findViewById(R.id.cantidadGuajoloyetAdobado),
                                root.findViewById(R.id.btnMenosGuajoloyetAdobado),
                                root.findViewById(R.id.btnMasGuajoloyetAdobado),
                                65.0
                        )
                products["Guajoloyets Adobados Extra"] =
                        ProductData(
                                root.findViewById(R.id.cantidadGujoloyetAdobadoExtra),
                                root.findViewById(R.id.btnMenosGujoloyetAdobadoExtra),
                                root.findViewById(R.id.btnMasGujoloyetAdobadoExtra),
                                77.0
                        )
            }
            "Papas" -> {
                products["Orden de Papas Sencillas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPapasSencillas),
                                root.findViewById(R.id.btnMenosPapasSencillas),
                                root.findViewById(R.id.btnMasPapasSencillas),
                                50.0
                        )
                products["Orden de Papas Queso y Tocino"] =
                        ProductData(
                                root.findViewById(R.id.cantidadPapasQuesoTocinoqueso),
                                root.findViewById(R.id.btnMenosPapasQuesoTocinoqueso),
                                root.findViewById(R.id.btnMasPapasQuesoTocinoqueso),
                                65.0
                        )
            }
            "Tacos" -> {
                products["Taco (c/u)"] =
                        ProductData(
                                root.findViewById(R.id.cantidadTacoUnitario),
                                root.findViewById(R.id.btnMenosTacoUnitario),
                                root.findViewById(R.id.btnMasTacoUnitario),
                                25.0
                        )
                products["Taco con Queso (c/u)"] =
                        ProductData(
                                root.findViewById(R.id.cantidadTacoConQueso),
                                root.findViewById(R.id.btnMenosTacoConQueso),
                                root.findViewById(R.id.btnMasTacoConQueso),
                                30.0
                        )
            }
            "Alitas" -> {
                products["Alitas 6 pzas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAlitas6),
                                root.findViewById(R.id.btnMenosAlitas6),
                                root.findViewById(R.id.btnMasAlitas6),
                                65.0
                        )
                products["Alitas 10 pzas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAlitas10),
                                root.findViewById(R.id.btnMenosAlitas10),
                                root.findViewById(R.id.btnMasAlitas10),
                                100.0
                        )
                products["Alitas 15 pzas"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAlitas15),
                                root.findViewById(R.id.btnMenosAlitas15),
                                root.findViewById(R.id.btnMasAlitas15),
                                140.0
                        )
                products["Combo"] =
                        ProductData(
                                root.findViewById(R.id.cantidadCombo),
                                root.findViewById(R.id.btnMenosCombo),
                                root.findViewById(R.id.btnMasCombo),
                                30.0
                        )
            }
            "Bebidas" -> {
                products["Refrescos"] =
                        ProductData(
                                root.findViewById(R.id.cantidadRefrescos),
                                root.findViewById(R.id.btnMenosRefrescos),
                                root.findViewById(R.id.btnMasRefrescos),
                                26.0
                        )
                products["Cafe"] =
                        ProductData(
                                root.findViewById(R.id.cantidadCafe),
                                root.findViewById(R.id.btnMenosCafe),
                                root.findViewById(R.id.btnMasCafe),
                                22.0
                        )
                products["Aguas de Sabor"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAguasSabor),
                                root.findViewById(R.id.btnMenosAguasSabor),
                                root.findViewById(R.id.btnMasAguasSabor),
                                25.0
                        )
                products["Agua Natural"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAguasNat),
                                root.findViewById(R.id.btnMenosAguasNat),
                                root.findViewById(R.id.btnMasAguasNat),
                                20.0
                        )
                products["Agua para Te"] =
                        ProductData(
                                root.findViewById(R.id.cantidadAguaTe),
                                root.findViewById(R.id.btnMenosAguaTe),
                                root.findViewById(R.id.btnMasAguaTe),
                                20.0
                        )
            }
            "Postres" -> {
                val etPrecio = root.findViewById<TextInputEditText>(R.id.etPrecioPostre)
                val btnAgregar = root.findViewById<MaterialButton>(R.id.btnAgregarPostre)
                val container = root.findViewById<LinearLayout>(R.id.containerPostres)

                btnAgregar.setOnClickListener {
                    val priceStr = etPrecio.text.toString()
                    val price = priceStr.toDoubleOrNull()

                    if (price != null && price > 0) {
                        val name = "Extra $${"%.2f".format(price)}"

                        if (products.containsKey(name)) {
                            // Si ya existe, incrementar cantidad 1
                            val currentQty = quantities.getOrPut(name) { 0 }
                            quantities[name] = currentQty + 1
                            val data = products[name]
                            if (data != null) {
                                // Actualizar visualmente
                                val tv = data.cantidadTV as? TextView
                                tv?.text = quantities[name].toString()
                                updateCardAppearance(tv, quantities[name] ?: 0, name)
                            }
                            requestRecalculate()
                        } else {
                            // Crear nueva fila
                            val itemView =
                                    LayoutInflater.from(this)
                                            .inflate(R.layout.item_custom_product, container, false)
                            val tvName = itemView.findViewById<TextView>(R.id.tvProductName)
                            val tvQty = itemView.findViewById<TextView>(R.id.tvCantidad)
                            val btnMenos = itemView.findViewById<Button>(R.id.btnMenos)
                            val btnMas = itemView.findViewById<Button>(R.id.btnMas)

                            tvName.text = name
                            tvQty.text = "1"

                            // Crear entry en products
                            val productData = ProductData(tvQty, btnMenos, btnMas, price)
                            products[name] = productData
                            quantities[name] = 1

                            // --- Configurar listeners MANUALMENTE pues setupAllProductListeners ya
                            // corrió ---
                            fun updateProductState() {
                                val qty = quantities[name] ?: 0
                                tvQty.text = qty.toString()
                                updateCardAppearance(tvQty, qty, name)
                                requestRecalculate()
                            }

                            btnMas.setOnClickListener {
                                val cantidadActual = quantities.getOrPut(name) { 0 }
                                quantities[name] = cantidadActual + 1
                                updateProductState()
                            }

                            btnMas.setOnLongClickListener {
                                val cantidadActual = quantities.getOrPut(name) { 0 }
                                quantities[name] = cantidadActual + 5
                                updateProductState()
                                true
                            }

                            btnMenos.setOnClickListener {
                                val cantidadActual = quantities.getOrPut(name) { 0 }
                                quantities[name] = (cantidadActual - 1).coerceAtLeast(0)
                                updateProductState()
                            }

                            btnMenos.setOnLongClickListener {
                                quantities[name] = 0
                                updateProductState()
                                true
                            }

                            tvQty.setOnClickListener {
                                val currentQty = quantities[name] ?: 0
                                showNumericKeypad(name, currentQty) { newQty ->
                                    quantities[name] = newQty
                                    updateProductState()
                                }
                            }

                            // Feedback visual inicial (primera vez = cantidad 1 -> color aleatorio)
                            updateCardAppearance(tvQty, 1, name)

                            container.addView(itemView, 0)
                            requestRecalculate()
                        }

                        // Limpiar input
                        etPrecio.text?.clear()
                        etPrecio.clearFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(etPrecio.windowToken, 0)
                    } else {
                        Toast.makeText(this, "Ingrese un precio válido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "Hamburguesas" -> {
                setupHamburguesasGrid(root as GridLayout)
            }
        }

        addCustomProductsForCategory(category, root)
        applyCatalogOverridesToLoadedProducts()

        // Inicializar cards nuevas con CharcoalCool (sin color aleatorio aún)
        products.forEach { (name, data) ->
            val qty = quantities[name] ?: 0
            updateCardAppearance(data.cantidadTV, qty, name)
        }

        setupAllProductListeners()
        updateAllProductViews()
    }

    private fun setupAllProductListeners() {
        for ((nombre, data) in products) {
            val tvCantidad = data.cantidadTV as? TextView

            fun updateProductState() {
                val qty = quantities[nombre] ?: 0
                tvCantidad?.text = qty.toString()

                // Color aleatorio al 1er ítem; CharcoalCool al volver a 0
                updateCardAppearance(tvCantidad, qty, nombre)

                requestRecalculate()
            }

            data.btnMas.setOnClickListener {
                val cantidadActual = quantities.getOrPut(nombre) { 0 }
                quantities[nombre] = cantidadActual + 1
                updateProductState()
            }

            data.btnMas.setOnLongClickListener {
                val cantidadActual = quantities.getOrPut(nombre) { 0 }
                quantities[nombre] = cantidadActual + 5
                updateProductState()
                true
            }

            data.btnMenos.setOnClickListener {
                val cantidadActual = quantities.getOrPut(nombre) { 0 }
                quantities[nombre] = (cantidadActual - 1).coerceAtLeast(0)
                updateProductState()
            }

            data.btnMenos.setOnLongClickListener {
                quantities[nombre] = 0
                updateProductState()
                true
            }

            // Click en el número -> Teclado numérico
            tvCantidad?.setOnClickListener {
                val currentQty = quantities[nombre] ?: 0
                showNumericKeypad(nombre, currentQty) { newQty ->
                    quantities[nombre] = newQty
                    updateProductState()
                }
            }

            // --- TAP ANYWHERE LOGIC ---
            // Buscamos el contenedor principal (CardView o similar) para que al tocarlo sume 1
            var view = data.btnMas.parent
            var cardFound: View? = null
            // Subimos hasta encontrar un MaterialCardView o un contenedor significativo
            while (view != null) {
                if (view is MaterialCardView) {
                    cardFound = view
                    break
                }
                // Si no es card, tal vez es el root de un include manually inflated
                view = view.parent
            }

            if (cardFound != null) {
                cardFound.setOnClickListener {
                    val cantidadActual = quantities.getOrPut(nombre) { 0 }
                    quantities[nombre] = cantidadActual + 1
                    updateProductState()
                }

                cardFound.setOnLongClickListener {
                    val currentQty = quantities[nombre] ?: 0
                    showNumericKeypad(nombre, currentQty) { newQty ->
                        quantities[nombre] = newQty
                        updateProductState()
                    }
                    true
                }
            } else {
                // Fallback: Si no hay card (ej. layouts viejos), intentar con el label
                // como estaba antes en el código original "Quick Add"
                val parent = data.btnMas.parent
                if (parent is ViewGroup) {
                    val grandParent = parent.parent
                    if (grandParent is ViewGroup) {
                        for (i in 0 until grandParent.childCount) {
                            val child = grandParent.getChildAt(i)
                            if (child is TextView && child.id != data.cantidadTV.id) {
                                child.setOnClickListener {
                                    val cantidadActual = quantities.getOrPut(nombre) { 0 }
                                    quantities[nombre] = cantidadActual + 1
                                    updateProductState()
                                }
                                child.setOnLongClickListener {
                                    val currentQty = quantities[nombre] ?: 0
                                    showNumericKeypad(nombre, currentQty) { newQty ->
                                        quantities[nombre] = newQty
                                        updateProductState()
                                    }
                                    true
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateAllProductViews() {
        for ((nombre, data) in products) {
            val qty = quantities[nombre] ?: 0
            // CharcoalCool en reposo, color aleatorio si hay cantidad
            updateCardAppearance(data.cantidadTV as? View, qty, nombre)
        }
    }
}
