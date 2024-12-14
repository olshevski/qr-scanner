package dev.olshevski.qrscanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _barcodeData = MutableStateFlow<BarcodeData?>(null)
    val barcodeData = _barcodeData.asStateFlow()

    fun setBarcodeData(barcodeData: BarcodeData) {
        _barcodeData.value = barcodeData
    }

    fun clearBarcodeData() {
        _barcodeData.value = null
    }

}