package dev.olshevski.qrscanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val clipboard = (application as QRScannerApp).clipboard

    private val _barcodeData = MutableStateFlow<BarcodeData?>(null)
    val barcodeData = _barcodeData.asStateFlow()

    fun setBarcodeData(barcodeData: BarcodeData) {
        _barcodeData.value = barcodeData
    }

    fun clearBarcodeData() {
        _barcodeData.value = null
    }

    fun copyBarcodeTextToClipboard() {
        barcodeData.value?.let {
            clipboard.setText(it.text)
        }
    }

}