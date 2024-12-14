package dev.olshevski.qrscanner

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.google.mlkit.vision.barcode.common.Barcode

class QrCodeTouchListener(
    private val barcodes: List<Barcode>,
    private val barcodeClickListener: (Barcode) -> Unit
) : OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            val clickedBarcode = barcodes.find {
                it.boundingBox?.contains(e.x.toInt(), e.y.toInt()) == true
            }
            if (clickedBarcode != null) {
                barcodeClickListener(clickedBarcode)
                return true
            }
        }

        return false
    }

}