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

# PrinterType is an enum
ptype_lines = extract_body(r'private enum class PrinterType')
resolve_profile = extract_body(r'private fun resolveTicketProfile')
center_text = extract_body(r'private fun centerText')
connect_bt = extract_body(r'private suspend fun connectToBluetoothPrinter')
print_bt = extract_body(r'private suspend fun printViaBluetooth')
close_bt = extract_body(r'private fun closeBluetoothSocket')
get_app_icon = extract_body(r'private fun getAppIconEscPos')
generar_ticket = extract_body(r'private suspend fun generarTextoTicket')
print_net = extract_body(r'private fun printViaNetwork')

bt_socket_decl = -1
for i, l in enumerate(lines):
    if 'private var bluetoothSocket: BluetoothSocket? = null' in l:
        bt_socket_decl = i
        break
if bt_socket_decl != -1:
    del lines[bt_socket_decl]
    
for i, l in enumerate(lines):
    if 'private var selectedPrinterType =' in l:
        lines[i] = l.replace('private var selectedPrinterType', 'var selectedPrinterType')
        break

# Create PrinterManager.kt
pm = """package com.example.ticketapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

"""
for l in ptype_lines: pt = l.replace('private ', '')
pm += "".join(ptype_lines).replace('private enum', 'enum') + "\n"

pm += """class PrinterManager(private val context: Context) {
    var bluetoothSocket: BluetoothSocket? = null
    var selectedPrinterType = PrinterType.BLUETOOTH_58MM
    
    val PRINTER_BT_NAMES = listOf("POS-58", "5890", "BlueTooth Printer")
    val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

"""

all_funcs = resolve_profile + center_text + connect_bt + print_bt + close_bt + get_app_icon + generar_ticket + print_net
all_funcs_str = "".join(all_funcs)
all_funcs_str = all_funcs_str.replace('private fun ', 'fun ')
all_funcs_str = all_funcs_str.replace('private suspend fun ', 'suspend fun ')
all_funcs_str = all_funcs_str.replace('this@MainActivity', 'context')
all_funcs_str = all_funcs_str.replace('getString(', 'context.getString(')
all_funcs_str = all_funcs_str.replace('contextcontext', 'context')
all_funcs_str = all_funcs_str.replace('resources', 'context.resources')
all_funcs_str = all_funcs_str.replace('getSystemService(Context.BLUETOOTH_SERVICE)', 'context.getSystemService(Context.BLUETOOTH_SERVICE)')

# Quick fix for getString when inside context.getString(...)
# wait, if generating ticket needs `noCuenta` or variables, those must be passed!
# Let's check generarTextoTicket to see what MainActivity vars it borrows 

pm += all_funcs_str
pm += "\n}"

with open('app/src/main/java/com/example/ticketapp/PrinterManager.kt', 'w') as f:
    f.write(pm)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.writelines(lines)
