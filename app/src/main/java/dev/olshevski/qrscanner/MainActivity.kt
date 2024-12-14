package dev.olshevski.qrscanner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dev.olshevski.qrscanner.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.barcodeData.onEach {
            if (it != null) {
                switchToPreviewFragment()
            } else {
                switchToCameraFragment()
            }
        }.launchIn(lifecycleScope)
    }

    private fun switchToCameraFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, CameraFragment::class.java, null)
            .commitNow()
    }

    private fun switchToPreviewFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, PreviewFragment::class.java, null)
            .commitNow()
    }

}