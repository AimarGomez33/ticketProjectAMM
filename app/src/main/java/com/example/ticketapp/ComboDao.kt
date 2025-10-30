package com.example.ticketapp


import androidx.room.*

// 🧱 Entidad que representa un combo en la base de datos
@Entity(tableName = "combos")
data class ComboEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,           // Ej: "Hamburguesa Clásica"
    val cantidad: Int,            // Cuántos combos de ese tipo
    val precioUnitario: Double,   // Precio por combo
    val total: Double,            // cantidad * precioUnitario
    val tipoHamburguesa: String   // Ej: "Clásica", "Hawaiana", etc.
)

// 🧭 DAO exclusivo para operaciones sobre combos
@Dao
interface ComboDao {

    // 🔹 Inserta un combo individual
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCombo(combo: ComboEntity)

    // 🔹 Inserta varios combos a la vez
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarListaCombos(combos: List<ComboEntity>)

    // 🔹 Obtiene todos los combos guardados
    @Query("SELECT * FROM combos")
    suspend fun obtenerTodos(): List<ComboEntity>

    // 🔹 Obtiene combos por tipo de hamburguesa
    @Query("SELECT * FROM combos WHERE tipoHamburguesa = :tipo")
    suspend fun obtenerPorTipo(tipo: String): List<ComboEntity>

    // 🔹 Devuelve la cantidad total de combos vendidos
    @Query("SELECT SUM(cantidad) FROM combos")
    suspend fun contarTotalCombos(): Int?

    // 🔹 Calcula la ganancia total de todos los combos
    @Query("SELECT SUM(total) FROM combos")
    suspend fun calcularGananciaTotal(): Double?

    // 🔹 Elimina un combo específico
    @Delete
    suspend fun eliminarCombo(combo: ComboEntity)

    // 🔹 Vacía toda la tabla
    @Query("DELETE FROM combos")
    suspend fun eliminarTodos()
}
