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
        entities =
                [
                        Product::class,
                        OrderEntity::class,
                        OrderItemEntity::class,
                        AguaSaborEntity::class,
                        RefrescoEntity::class],
        version = 21,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

        abstract fun productDao(): ProductDao
        abstract fun orderDao(): OrderDao
        abstract fun aguassaborDao(): AguaSaborDao
        abstract fun refrescoDao(): RefrescoDao

        companion object {
                @Volatile private var INSTANCE: AppDatabase? = null

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

                val MIGRATION_18_19 =
                        object : Migration(18, 19) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "CREATE TABLE IF NOT EXISTS `refrescos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flavor_name` TEXT NOT NULL, `quantity_available` INTEGER NOT NULL)"
                                        )
                                }
                        }

                // Antes esta migración creaba refrescos de nuevo (bug),
                // ahora crea aguas_sabor de forma independiente.
                val MIGRATION_19_20 =
                        object : Migration(19, 20) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "CREATE TABLE IF NOT EXISTS `aguas_sabor` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flavor_name` TEXT NOT NULL, `quantity_available` INTEGER NOT NULL)"
                                        )
                                }
                        }

                // Migración de seguridad: garantiza que ambas tablas existen
                val MIGRATION_20_21 =
                        object : Migration(20, 21) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "CREATE TABLE IF NOT EXISTS `aguas_sabor` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flavor_name` TEXT NOT NULL, `quantity_available` INTEGER NOT NULL)"
                                        )
                                        db.execSQL(
                                                "CREATE TABLE IF NOT EXISTS `refrescos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flavor_name` TEXT NOT NULL, `quantity_available` INTEGER NOT NULL)"
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
                                                                "ticket_app_database"
                                                        )
                                                        .addMigrations(
                                                                MIGRATION_16_17,
                                                                MIGRATION_17_18,
                                                                MIGRATION_18_19,
                                                                MIGRATION_19_20,
                                                                MIGRATION_20_21
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

                suspend fun pupalateAguas(aguaDao: AguaSaborDao) {
                        aguaDao.deleteAll()
                        val defaultAguas =
                                listOf(
                                        AguaSaborEntity(
                                                flavorName = "Horchata",
                                                quantityAvailable = 100
                                        ),
                                        AguaSaborEntity(
                                                flavorName = "Jamaica",
                                                quantityAvailable = 100
                                        ),
                                )
                        defaultAguas.forEach { aguaDao.insert(it) }
                }

                suspend fun populateRefrescos(refrescoDao: RefrescoDao) {
                        refrescoDao.deleteAll()
                        val defaultRefrescos =
                                listOf(
                                        RefrescoEntity(
                                                flavorName = "Coca Cola",
                                                quantityAvailable = 50
                                        ),
                                        RefrescoEntity(
                                                flavorName = "Manzanita",
                                                quantityAvailable = 50
                                        ),
                                        RefrescoEntity(
                                                flavorName = "Sprite",
                                                quantityAvailable = 50
                                        ),
                                )
                        defaultRefrescos.forEach { refrescoDao.insert(it) }
                }
        }
}
