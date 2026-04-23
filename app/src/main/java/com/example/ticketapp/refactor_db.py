import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

# Multiline getProfitByCategory
text = re.sub(
    r'appDatabase\s*\.\s*orderDao\(\)\s*\.\s*getProfitByCategory\(',
    r'viewModel.getProfitByCategory(',
    text
)

# Fix filter method
text = re.sub(
    r'val orders = viewModel\.getAllOrdersFlow\(\)\s*\n\s*val hoyOrders = orders\.filter',
    r'val orders = viewModel.getAllOrdersFlow() // <-- this returns a Flow\n                        // This needs fixing manually',
    text
)

text = re.sub(
    r'appDatabase\s*\.\s*orderDao\(\)',
    r'viewModel',
    text
)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
