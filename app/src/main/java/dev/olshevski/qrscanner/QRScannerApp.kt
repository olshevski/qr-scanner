package dev.olshevski.qrscanner

import android.app.Application

class QRScannerApp : Application() {

    lateinit var clipboard: Clipboard

    override fun onCreate() {
        super.onCreate()
        clipboard = Clipboard(this)
    }

}