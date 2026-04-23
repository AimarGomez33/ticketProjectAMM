import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

# I previously replaced `appDatabase.orderDao().getAllOrders()` with `viewModel.getAllOrdersFlow()`. Let's revert that specific mistake.
text = text.replace('val orders = viewModel.getAllOrdersFlow()', 'val orders = viewModel.getAllOrders()')
text = text.replace('val updatedOrders = viewModel.getAllOrdersFlow()', 'val updatedOrders = viewModel.getAllOrders()')
text = text.replace('val pedidos = viewModel.getAllOrdersFlow()', 'val pedidos = viewModel.getAllOrders()')

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
