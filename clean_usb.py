import re

path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove USB imports
content = re.sub(r'import\s+android\.hardware\.usb\..*\n', '', content)

# 2. Remove UsbManager property and variables (they are usually declared as private or similar)
content = re.sub(r'\s*private\s+lateinit\s+var\s+usbManager\s*:\s*UsbManager\s*\n', '', content)
content = re.sub(r'\s*private\s+var\s+usbDevice\s*:\s*UsbDevice\s*\?\s*=\s*null\s*\n', '', content)
content = re.sub(r'\s*private\s+var\s+usbDeviceConnection\s*:\s*UsbDeviceConnection\s*\?\s*=\s*null\s*\n', '', content)
content = re.sub(r'\s*private\s+var\s+usbInterface\s*:\s*UsbInterface\s*\?\s*=\s*null\s*\n', '', content)
content = re.sub(r'\s*private\s+var\s+usbEndpointOut\s*:\s*UsbEndpoint\s*\?\s*=\s*null\s*\n', '', content)

# 3. Remove ACTION_USB_PERMISSION and PRINTER_USB_IDS from companion object
content = re.sub(r'\s*private\s+const\s+val\s+ACTION_USB_PERMISSION\s*=\s*"com\.example\.ticketapp\.USB_PERMISSION"\s*\n', '', content)
content = re.sub(r'\s*\n\s*//\s*Vendor/Product.*?\n', '\n', content)
content = re.sub(r'\s*//\s*(1155|1659).*?\n', '\n', content)
content = re.sub(r'\s*private\s+val\s+PRINTER_USB_IDS\s*=\s*setOf\(Pair\(1155,\s*22339\),\s*Pair\(1659,\s*8963\)\)\s*\n', '', content)

# 4. Remove usbManager init from onCreate
content = re.sub(r'\s*usbManager\s*=\s*getSystemService\(USB_SERVICE\)\s+as\s+UsbManager\s*\n', '', content)

# 5. Replace printViaUsb(...) || printViaBluetooth(...) with printViaBluetooth(...)
content = re.sub(r'printViaUsb\(.*?\)\s*\|\|\s*printViaBluetooth\(', 'printViaBluetooth(', content)

# 6. Remove detectAndRequestUsbPermission call
content = re.sub(r'\s*detectAndRequestUsbPermission\(\)\s*\n', '', content)

# 7. Remove releaseUsbDevice and unregisterReceiver calls in onDestroy
content = re.sub(r'\s*unregisterReceiver\(usbReceiver\)\s*\n', '', content)
content = re.sub(r'\s*releaseUsbDevice\(\)\s*\n', '', content)

# 8. Filter out the broadcast receiver registration
content = re.sub(r'\s*//\s*registrar\s*receiver\s*USB\s*\n', '', content)
content = re.sub(r'\s*val\s+filter\s*=\s*IntentFilter\(ACTION_USB_PERMISSION\)\s*\n', '', content)
content = re.sub(r'\s*if\s*\(Build\.VERSION\.SDK_INT\s*>=\s*Build\.VERSION_CODES\.TIRAMISU\)\s*\{\s*\n\s*registerReceiver\(usbReceiver,\s*filter,\s*RECEIVER_NOT_EXPORTED\)\s*\n\s*\}\s*else\s*\{\s*\n\s*@Suppress\("UnspecifiedRegisterReceiverFlag"\)\s*registerReceiver\(usbReceiver,\s*filter\)\s*\n\s*\}\s*\n', '', content)


with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("USB lines stripped.")
