import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    lines = f.readlines()

def extract_body(start_regex):
    start_idx = -1
    for i, l in enumerate(lines):
        if re.search(start_regex, l):
            start_idx = i
            break
            
    if start_idx == -1: return []
    
    # Simple brace counting
    extracted = []
    braces = 0
    started = False
    
    end_idx = start_idx
    for i in range(start_idx, len(lines)):
        l = lines[i]
        extracted.append(l)
        braces += l.count('{')
        braces -= l.count('}')
        
        if '{' in l:
            started = True
            
        if started and braces == 0:
            end_idx = i
            break
            
    # clear from lines
    for i in range(end_idx, start_idx - 1, -1):
        del lines[i]
        
    return extracted

# 1. Strip classes and methods
extract_body(r'private enum class PrinterType')
extract_body(r'private fun resolveTicketProfile')
extract_body(r'private fun centerText')
extract_body(r'private suspend fun connectToBluetoothPrinter')
extract_body(r'private suspend fun printViaBluetooth')
extract_body(r'private fun closeBluetoothSocket')
extract_body(r'private fun getAppIconEscPos')
extract_body(r'private suspend fun generarTextoTicket')
extract_body(r'private fun printViaNetwork')
extract_body(r'private fun isDarkPixel')
extract_body(r'private fun drawableToBitmap')
extract_body(r'private fun getEscPosImageWithGsV0')
extract_body(r'private fun getEscPosImageWithEscStar')

# also remove printWithSelectedPrinter if it exists
extract_body(r'private suspend fun printWithSelectedPrinter')

# 2. Inject PrinterManager logic
text = "".join(lines)

# Inject PrinterManager into Factory
injection = """    // PrinterManager se crea con applicationContext para sobrevivir rotaciones de pantalla.
    private val printerManager: PrinterManager by lazy { PrinterManager(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            OrderRepository(
                AppDatabase.getDatabase(applicationContext, lifecycleScope).orderDao(),
                AppDatabase.getDatabase(applicationContext, lifecycleScope).productDao()
            ),
            printerManager
        )
    }"""

text = re.sub(r'    private val viewModel: MainViewModel.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n    \}', injection, text)

# Remove old vars
text = re.sub(r'    private val bluetoothManager.*\n.*\n    \}', '', text)
text = re.sub(r'    private val bluetoothAdapter.*\n', '', text)
text = re.sub(r'    private var bluetoothSocket.*\n', '', text)
text = re.sub(r'    private var selectedPrinterType.*\n', '', text)

# Update references to selectedPrinterType
text = text.replace('selectedPrinterType', 'printerManager.selectedPrinterType')
text = text.replace('PrinterType.BLUETOOTH_58MM', 'com.example.ticketapp.PrinterType.BLUETOOTH_58MM')
text = text.replace('PrinterType.NETWORK_80MM', 'com.example.ticketapp.PrinterType.NETWORK_80MM')
text = text.replace('PrinterType.NETWORK_58MM', 'com.example.ticketapp.PrinterType.NETWORK_58MM')


# Change print calls to use printTicket from viewModel
old_call_1 = 'val textoTicket = generarTextoTicket(productosSeleccionados, printerManager.selectedPrinterType)'
new_call_1 = 'val textoTicket = "" // Se delega al viewModel la impresión completa'
text = text.replace(old_call_1, new_call_1)

old_call_2 = 'val ticketTexto = generarTextoTicket(productosSeleccionados, printerManager.selectedPrinterType)'
new_call_2 = 'val ticketTexto = "" // Se delega al viewModel la impresión completa'
text = text.replace(old_call_2, new_call_2)

old_call_3 = 'val ticket = generarTextoTicket(productos, printerManager.selectedPrinterType)'
new_call_3 = 'val ticket = "" // Se delega al viewModel'
text = text.replace(old_call_3, new_call_3)

# Inside showOrderSummaryDialog and new order blocks:
# Where it was: printWithSelectedPrinter(textoTicket) -> viewModel.printTicket
text = text.replace('printWithSelectedPrinter(textoTicket)', 'viewModel.printTicket(productosSeleccionados, editTextMesa.text.toString().trim(), noCuenta.isChecked, getString(R.string.cuenta))')
text = text.replace('printWithSelectedPrinter(ticketTexto)', 'viewModel.printTicket(productosSeleccionados, editTextMesa.text.toString().trim(), noCuenta.isChecked, getString(R.string.cuenta))')
text = text.replace('printWithSelectedPrinter(ticket)', 'viewModel.printTicket(productos, "", false, "")')

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
