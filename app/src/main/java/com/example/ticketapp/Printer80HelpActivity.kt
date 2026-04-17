package com.example.ticketapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Printer80HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_80_help)

        val btnBack = findViewById<ImageButton>(R.id.btnBackPrinterHelp)
        btnBack.setOnClickListener { finish() }
    }
}
