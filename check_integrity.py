with open(r'c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\java\com\example\ticketapp\MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

issues = []
funcs = ['fun android.view.View.animateClickAndHaptic', 'fun generateQrBitmap', 'private fun reimprimirTicket', 'private fun ejecutarImpresion', 'private fun showOrderSummaryDialog']
for func in funcs:
    count = content.count(func)
    if count != 1:
        issues.append(f'Function "{func}" appears {count} times (expected 1)')

if '\x00' in content:
    issues.append('File contains null bytes (corruption)')

print('Last 100 chars:', repr(content[-100:]))

if issues:
    print('ISSUES FOUND:')
    for i in issues: print(' -', i)
else:
    print('No structural issues found')

# Also check PrinterManager
with open(r'c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\java\com\example\ticketapp\PrinterManager.kt', 'r', encoding='utf-8') as f:
    pm = f.read()
print('PrinterManager last 80 chars:', repr(pm[-80:]))
if '\x00' in pm:
    print('PrinterManager has null bytes!')
else:
    print('PrinterManager: clean')
