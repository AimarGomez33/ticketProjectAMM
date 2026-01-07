package com.example.ticketapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
        entities = [Product::class, OrderEntity::class, OrderItemEntity::class],
        version = 18,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

        abstract fun productDao(): ProductDao
        abstract fun orderDao(): OrderDao

        companion object {
                @Volatile private var INSTANCE: AppDatabase? = null

                val MIGRATION_12_13 =
                        object : Migration(1, 2) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Define aquí los cambios.
                                        // En este caso, añadimos la columna 'esCombo' a la tabla
                                        // 'orders'.
                                        // La columna es de tipo INTEGER (0 para false, 1 para
                                        // true).
                                        // Le damos un valor por defecto de 0 (false) para todos los
                                        // registros
                                        // existentes.
                                        db.execSQL(
                                                "ALTER TABLE orders ADD COLUMN esCombo INTEGER NOT NULL DEFAULT 0"
                                        )
                                }
                        }

                val MIGRATION_16_17 =
                        object : Migration(16, 17) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "ALTER TABLE orders ADD COLUMN daily_order_number INTEGER"
                                        )
                                }
                        }

                val MIGRATION_17_18 =
                        object : Migration(17, 18) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "ALTER TABLE order_items ADD COLUMN comment TEXT"
                                        )
                                }
                        }

                fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
                        return INSTANCE
                                ?: synchronized(this) {
                                        val instance =
                                                Room.databaseBuilder(
                                                                context.applicationContext,
                                                                AppDatabase::class.java,
                                                                "ticket_app_database" // El nombre
                                                                // de tu base
                                                                // de
                                                                // datos
                                                                )
                                                        // ✅ PASO 3: Añade la migración al
                                                        // constructor
                                                        // .addMigrations(MIGRATION_12_13) //
                                                        // Commented out old one
                                                        .addMigrations(
                                                                MIGRATION_16_17,
                                                                MIGRATION_17_18
                                                        )
                                                        .fallbackToDestructiveMigration()
                                                        .build()
                                        INSTANCE = instance
                                        instance
                                }
                }

                private class DatabaseCallback(private val scope: CoroutineScope) :
                        RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                INSTANCE?.let { database ->
                                        scope.launch { populateDatabase(database.productDao()) }
                                }
                        }
                }

                suspend fun populateDatabase(productDao: ProductDao) {
                        productDao.deleteAll()
                        val defaultProducts =
                                listOf(
                                        Product(
                                                name = "Chalupas",
                                                price = 5.0,
                                                category = "Platillos Principales"
                                        ),
                                        Product(
                                                name = "Pambazos Naturales",
                                                price = 34.0,
                                                category = "Pambazos"
                                        ),
                                        Product(
                                                name = "Guajoloyet Natural",
                                                price = 55.0,
                                                category = "Guajoloyets"
                                        ),
                                        Product(
                                                name = "Alones",
                                                price = 25.0,
                                                category = "Entradas"
                                        ),
                                        Product(
                                                name = "Refrescos",
                                                price = 25.0,
                                                category = "Bebidas"
                                        ),
                                        Product(
                                                name = "Postres",
                                                price = 30.0,
                                                category = "Postres y Extras"
                                        )
                                )
                        defaultProducts.forEach { productDao.insert(it) }
                }
        }
}
