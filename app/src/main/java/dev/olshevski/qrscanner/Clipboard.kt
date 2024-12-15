package dev.olshevski.qrscanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.widget.Toast

class Clipboard(private val context: Context) {

    private val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    fun setText(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
        Toast.makeText(context, "\"$text\" copied to clipboard", Toast.LENGTH_SHORT).show()
    }

}