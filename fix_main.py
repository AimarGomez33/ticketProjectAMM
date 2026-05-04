import re
import os

with open(r'c:\Users\jair_\Desktop\ticketProjectAMM\temp_stash_main.kt', 'rb') as f:
    data = f.read()

# Fix BOM
if data.startswith(b'\xff\xfe'):
    text = data[2:].decode('utf-8', errors='ignore')
else:
    text = data.decode('utf-8', errors='ignore')

# 1. Add imports and extensions at the top
import_addition = '''import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.Color
import android.view.HapticFeedbackConstants
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

fun android.view.View.animateClickAndHaptic() {
    this.performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
    this.animate().scaleX(0.95f).scaleY(0.95f).setDuration(50).withEndAction {
        this.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start()
    }.start()
}

fun generateQrBitmap(content: String, size: Int): Bitmap? {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    return try {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        null
    }
}
'''
text = text.replace('import kotlinx.coroutines.withContext', import_addition)

# 2. Add animateClickAndHaptic to buttons
text = text.replace('btnMasNormal.setOnClickListener {', 'btnMasNormal.setOnClickListener {\n            it.animateClickAndHaptic()')
text = text.replace('btnMenosNormal.setOnClickListener {', 'btnMenosNormal.setOnClickListener {\n            it.animateClickAndHaptic()')
text = text.replace('btnMasCombo.setOnClickListener {', 'btnMasCombo.setOnClickListener {\n            it.animateClickAndHaptic()')
text = text.replace('btnMenosCombo.setOnClickListener {', 'btnMenosCombo.setOnClickListener {\n            it.animateClickAndHaptic()')
text = text.replace('card.setOnClickListener {\n            cantidadesNormales[nombre]', 'card.setOnClickListener {\n            it.animateClickAndHaptic()\n            cantidadesNormales[nombre]')

# 3. Update reimprimirTicket for QR and 'llevar'
old_qr = '''                // Genera y muestra el código QR en el diálogo
                val qrImageView = dialogView.findViewById<android.widget.ImageView>(R.id.qrCodeImageView)
                val qrBitmap = generateQrBitmap("https://share.google/28cp0kgjV0ZwsHk5I", 400)
                if (qrBitmap != null) qrImageView.setImageBitmap(qrBitmap)'''

new_qr = '''                val mesaStr = order.mesa ?: ""
                val isParaLlevar = mesaStr.equals("llevar", ignoreCase = true)

                // Genera y muestra el código QR en el diálogo
                val qrImageView = dialogView.findViewById<android.widget.ImageView>(R.id.qrCodeImageView)
                if (!isParaLlevar) {
                    val qrBitmap = generateQrBitmap("https://share.google/28cp0kgjV0ZwsHk5I", 600)
                    if (qrBitmap != null) qrImageView.setImageBitmap(qrBitmap)
                } else {
                    qrImageView.visibility = android.view.View.GONE
                    // También ocultamos el logo y el texto de reseña del dialog
                    dialogView.findViewById<android.widget.ImageView>(R.id.qrCodeImageView).parent.let { parent ->
                        if (parent is android.view.ViewGroup) {
                            val logo = parent.getChildAt(0)
                            if (logo is android.widget.ImageView) logo.visibility = android.view.View.GONE
                            val divider = parent.getChildAt(2) // Divider view
                            if (divider != null) divider.visibility = android.view.View.GONE
                            val title = parent.getChildAt(3) // TextView de "Visitanos..."
                            if (title is android.widget.TextView) title.visibility = android.view.View.GONE
                        }
                    }
                }'''

if old_qr in text:
    text = text.replace(old_qr, new_qr)
else:
    print('Warning: old QR logic not found exactly.')

with open(r'c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\java\com\example\ticketapp\MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print('Restored MainActivity.kt with all fixes. Lines:', len(text.splitlines()))
