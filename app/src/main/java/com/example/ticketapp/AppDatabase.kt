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
    version = 2, // subir versión a 2
    exportSchema = true
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
                    .fallbackToDestructiveMigration() // ✅ destruye y recrea base si hay cambios
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
            productDao.deleteAll()

            var product = Product(name = "Chalupas", price = 5.0, category = "Platillos Principales")
            productDao.insert(product)
            product = Product(name = "Pambazos Naturales", price = 34.0, category = "Pambazos")
            productDao.insert(product)
            product = Product(name = "Guajoloyet Natural", price = 55.0, category = "Guajoloyets")
            productDao.insert(product)
            product = Product(name = "Alones", price = 25.0, category = "Entradas")
            productDao.insert(product)
            product = Product(name = "Refrescos", price = 25.0, category = "Bebidas")
            productDao.insert(product)
            product = Product(name = "Postres 30", price = 30.0, category = "Postres y extras")
            productDao.insert(product)
        }
    }
}
