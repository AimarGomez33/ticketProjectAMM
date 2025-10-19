package com.example.ticketapp

/**
 * Clase de datos para devolver resúmenes diarios agregados desde la base de datos.
 * No es una @Entity, es un POJO para los resultados de la consulta.
 */
data class DailySummary(
    /** Fecha contable (época en milisegundos a medianoche) que representa el día del resumen. */
    val businessDate: Long,

    /** Suma de los totales de todas las órdenes en este día. */
    val totalSales: Double,

    /** Número de órdenes registradas en este día. */
    val ordersCount: Int
)

/**
 * Clase de datos para resúmenes semanales agregados. El campo 'week' usa el formato
 * ISO 'YYYY-WW' (ej: "2025-34" para la semana 34 de 2025).
 */
data class WeeklySummary(
    val week: String,
    val totalSales: Double,
    val ordersCount: Int
)

/**
 * Clase de datos para resúmenes mensuales agregados. El campo 'month' tiene
 * el formato "YYYY-MM" (ej: "2025-08").
 */
data class MonthlySummary(
    val month: String,
    val totalSales: Double,
    val ordersCount: Int
)
