package com.example.ticketapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.ticketapp.Converters

@Database(
    entities = [
        Product::class,
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 12,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
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
            productDao.deleteAll()
            val defaultProducts = listOf(
                Product(name = "Chalupas", price = 5.0, category = "Platillos Principales"),
                Product(name = "Pambazos Naturales", price = 34.0, category = "Pambazos"),
                Product(name = "Guajoloyet Natural", price = 55.0, category = "Guajoloyets"),
                Product(name = "Alones", price = 25.0, category = "Entradas"),
                Product(name = "Refrescos", price = 25.0, category = "Bebidas"),
                Product(name = "Postres", price = 30.0, category = "Postres y Extras")
            )
            defaultProducts.forEach { productDao.insert(it) }
        }
    }
}
