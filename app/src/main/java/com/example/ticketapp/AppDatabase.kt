package com.example.ticketapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticket_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDatabase(database.productDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(productDao: ProductDao) {
            // Delete all content here.
            productDao.deleteAll()

            // Add sample products
            val products = listOf(
                Product(name = "Tacos de Pastor", price = 3.50),
                Product(name = "Quesadillas", price = 4.00),
                Product(name = "Tortas", price = 5.50),
                Product(name = "Agua Fresca", price = 2.00),
                Product(name = "Refresco", price = 2.50),
                Product(name = "Cerveza", price = 3.00),
                Product(name = "Nachos", price = 4.50),
                Product(name = "Guacamole", price = 3.00),
                Product(name = "Elote", price = 2.50),
                Product(name = "Churros", price = 3.50)
            )

            products.forEach { product ->
                productDao.insert(product)
            }
        }
    }
}
