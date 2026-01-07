package com.example.ticketapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TableSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddTable: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: TablesAdapter

    private var tableCount = 12

    companion object {
        private const val PREFS_NAME = "TablePrefs"
        private const val KEY_TABLE_COUNT = "table_count"
        private const val DEFAULT_TABLE_COUNT = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_selection)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Init SharedPrefs
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        tableCount = sharedPreferences.getInt(KEY_TABLE_COUNT, DEFAULT_TABLE_COUNT)

        // Setup Views
        recyclerView = findViewById(R.id.recyclerViewTables)
        fabAddTable = findViewById(R.id.fabAddTable)

        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = TablesAdapter(tableCount) { tableNumber -> openTableOrders(tableNumber) }

        // Auto-fit grid calculation could be added here, but staying with fixed span for now
        // or using a layout manager that adapts
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        fabAddTable.setOnClickListener {
            // Increment table count
            tableCount++

            // Save to prefs
            sharedPreferences.edit().putInt(KEY_TABLE_COUNT, tableCount).apply()

            // Update UI
            adapter.updateCount(tableCount)
            adapter.notifyItemInserted(tableCount - 1)

            // Scroll to bottom
            recyclerView.smoothScrollToPosition(tableCount - 1)

            Toast.makeText(this, "Mesa $tableCount agregada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTableOrders(tableNumber: Int) {
        // Use Coroutine to check DB for open order
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext, lifecycleScope)
            val openOrder = db.orderDao().getOpenOrderByTable("Mesa $tableNumber")

            withContext(Dispatchers.Main) {
                val intent = Intent(this@TableSelectionActivity, MainActivity::class.java)
                intent.putExtra("TABLE_NUMBER", tableNumber)
                if (openOrder != null) {
                    intent.putExtra("ORDER_ID", openOrder.orderId)
                }
                startActivity(intent)
            }
        }
    }

    // Inner Adapter Class for simplicity
    inner class TablesAdapter(private var count: Int, private val onTableClick: (Int) -> Unit) :
            RecyclerView.Adapter<TablesAdapter.TableViewHolder>() {

        fun updateCount(newCount: Int) {
            count = newCount
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
            val view =
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_table_card, parent, false)
            return TableViewHolder(view)
        }

        override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
            val tableNumber = position + 1
            holder.bind(tableNumber)
        }

        override fun getItemCount(): Int = count

        inner class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTableNumber: TextView = itemView.findViewById(R.id.textTableNumber)
            private val card: View = itemView.findViewById(R.id.cardTable)

            fun bind(number: Int) {
                tvTableNumber.text = number.toString()
                card.setOnClickListener { onTableClick(number) }
            }
        }
    }
}
