package com.example.ticketapp.repository

import android.util.Log
import com.example.ticketapp.data.kds.KdsOrder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KdsRepository {

    companion object {
        private const val TAG = "KdsRepository"
        private const val ORDERS_NODE = "orders"
    }

    private val db: FirebaseDatabase =
            FirebaseDatabase.getInstance("https://ticketa-349dc-default-rtdb.firebaseio.com/")
                    .apply {
                        // Allows orders to be queued and sent when the device reconnects to the
                        // internet
                        try {
                            setPersistenceEnabled(true)
                        } catch (e: Exception) {
                            /* already enabled */
                        }
                    }
    private val database = db.reference

    init {
        // Keep the orders node in sync so the kitchen sees changes immediately
        database.child(ORDERS_NODE).keepSynced(true)
        Log.d(TAG, "KdsRepository inicializado. DB URL: ${db.app.options.databaseUrl}")
    }

    fun getPendingOrders(): Flow<List<KdsOrder>> = callbackFlow {
        Log.d(TAG, "getPendingOrders: registrando listener...")
        val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d(
                                TAG,
                                "getPendingOrders: onDataChange - ${snapshot.childrenCount} nodos"
                        )
                        val orders =
                                snapshot.children
                                        .mapNotNull { it.getValue(KdsOrder::class.java) }
                                        .filter { it.status == "PENDING" }
                                        .sortedBy { it.timestamp }
                        Log.d(TAG, "getPendingOrders: ${orders.size} ordenes PENDING")
                        trySend(orders)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "getPendingOrders: cancelado - ${error.code}: ${error.message}")
                        close(error.toException())
                    }
                }
        val ref = database.child(ORDERS_NODE)
        ref.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "getPendingOrders: removiendo listener")
            ref.removeEventListener(listener)
        }
    }

    fun getCompletedOrders(): Flow<List<KdsOrder>> = callbackFlow {
        val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val orders =
                                snapshot.children
                                        .mapNotNull { it.getValue(KdsOrder::class.java) }
                                        .filter { it.status == "COMPLETED" }
                                        .sortedByDescending { it.timestamp }
                        trySend(orders)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                                TAG,
                                "getCompletedOrders: cancelado - ${error.code}: ${error.message}"
                        )
                        close(error.toException())
                    }
                }
        val ref = database.child(ORDERS_NODE)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun emitOrder(order: KdsOrder) {
        Log.d(
                TAG,
                "emitOrder: enviando orden id=${order.id}, mesa=${order.tableNumber}, items=${order.items.size}"
        )
        val orderRef = database.child(ORDERS_NODE).child(order.id)
        orderRef.setValue(order).await()
        Log.d(TAG, "emitOrder: orden ${order.id} escrita en Firebase exitosamente")
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        Log.d(TAG, "updateOrderStatus: $orderId -> $newStatus")
        database.child(ORDERS_NODE).child(orderId).child("status").setValue(newStatus).await()
    }

    suspend fun updateOrderItemStatus(orderId: String, itemIndex: Int, newStatus: String) {
        database.child(ORDERS_NODE)
                .child(orderId)
                .child("items")
                .child(itemIndex.toString())
                .child("status")
                .setValue(newStatus)
                .await()
    }

    /**
     * Deletes COMPLETED orders older than [maxAgeDays] days from Firebase. Safe to call on every
     * app start — only removes completed tickets.
     */
    suspend fun cleanupOldOrders(maxAgeDays: Int = 3) {
        val cutoff = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000)
        Log.d(TAG, "cleanupOldOrders: buscando órdenes completadas con más de ${maxAgeDays}d")

        val snapshot = database.child(ORDERS_NODE).get().await()
        var deleted = 0
        snapshot.children.forEach { child ->
            val order = child.getValue(KdsOrder::class.java)
            if (order != null && order.status == "COMPLETED" && order.timestamp < cutoff) {
                child.ref.removeValue().await()
                deleted++
                Log.d(TAG, "cleanupOldOrders: eliminada orden ${order.id} (${order.tableNumber})")
            }
        }
        Log.d(TAG, "cleanupOldOrders: $deleted órdenes eliminadas")
    }
}
