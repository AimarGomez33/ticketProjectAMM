import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

# Fix duplicates
text = text.replace('printerManager.printerManager.', 'printerManager.')
text = text.replace('com.example.ticketapp.com.example.ticketapp.', 'com.example.ticketapp.')

# Also add the missing import for viewModels
if 'import androidx.activity.viewModels' not in text:
    text = text.replace('import androidx.appcompat.app.AppCompatActivity', 'import androidx.appcompat.app.AppCompatActivity\nimport androidx.activity.viewModels')

# Fix unresolved reference `insertOrderWithItems` because previously it was replaced by `guardarPedidoBD` in `refactor_db.py`, but maybe some left?
# What errors specifically:
# e: ...:2422:23 Unresolved reference 'insertOrderWithItems'.
# e: ...:2446:27 Unresolved reference 'deleteItemsForOrder'.
# e: ...:2458:27 Unresolved reference 'insertOrderItems'.
# e: ...:2462:27 Unresolved reference 'updateOrderTotal'.
# e: ...:2499:27 Unresolved reference 'insertOrderWithItems'.
# All of these are on `viewModel`. Because my MainViewModel has them but with `BD` suffix!!
text = text.replace('viewModel.insertOrderWithItems', 'viewModel.guardarPedidoBD')
text = text.replace('viewModel.deleteItemsForOrder', 'viewModel.deleteItemsForOrderBD')
text = text.replace('viewModel.insertOrderItems', 'viewModel.insertOrderItemsBD')
text = text.replace('viewModel.updateOrderTotal', 'viewModel.updateOrderTotalBD')

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
