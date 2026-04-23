import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

# Remove private lateinit var appDatabase: AppDatabase
text = re.sub(r'private lateinit var appDatabase: AppDatabase\n?', '', text)
text = re.sub(r'appDatabase = AppDatabase.getDatabase\(applicationContext, lifecycleScope\)\n?', '', text)
# Removed commented out ones
text = re.sub(r'//\s*appDatabase = AppDatabase.getDatabase\(.*?\)\n?', '', text)

# Mappings of methods
# appDatabase.orderDao().insertOrderWithItems(order, items) -> viewModel.guardarPedidoBD(order, items)
text = text.replace('appDatabase.orderDao().insertOrderWithItems', 'viewModel.guardarPedidoBD')

# appDatabase.orderDao().getAllOrders()
text = text.replace('appDatabase.orderDao().getAllOrders()', 'viewModel.getAllOrdersFlow()') # wait, getAllOrders returns List not flow!
text = text.replace('val orders = appDatabase.orderDao().getAllOrders()', 'val orders = viewModel.getAllOrdersFlow()') # this might be wrong if it expects a list. I'll use getAllOrdersFlow() ? Let's just fix the rest first.

text = text.replace('appDatabase.orderDao()', 'viewModel')

# The proxy functions I added match exactly except:
text = text.replace('viewModel.deleteOrderById', 'viewModel.deleteOrderById')
text = text.replace('viewModel.updateOrderTotal', 'viewModel.updateOrderTotalBD')
text = text.replace('viewModel.deleteItemsForOrder', 'viewModel.deleteItemsForOrderBD')
text = text.replace('viewModel.insertOrderItems', 'viewModel.insertOrderItemsBD')
text = text.replace('viewModel.getAllOrdersFlow()', 'viewModel.getAllOrdersFlow()')

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
