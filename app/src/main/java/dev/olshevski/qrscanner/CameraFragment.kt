package dev.olshevski.qrscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import dev.olshevski.qrscanner.databinding.FragmentCameraBinding
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var viewBinding: FragmentCameraBinding
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var barcodeScanner: BarcodeScanner

    private var takePictureExecutor = Executors.newSingleThreadExecutor()

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    requireContext(),
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    @OptIn(ExperimentalZeroShutterLag::class)
    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        cameraController = LifecycleCameraController(requireContext())
        val previewView: PreviewView = viewBinding.root

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(requireContext())
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResults = result?.getValue(barcodeScanner)
                if ((barcodeResults == null) ||
                    (barcodeResults.size == 0) ||
                    (barcodeResults.first() == null)
                ) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ -> false } //no-op
                    return@MlKitAnalyzer
                }

                previewView.setOnTouchListener(
                    QrCodeTouchListener(
                        barcodeResults,
                        ::handleBarcodeClick
                    )
                )
                previewView.overlay.clear()
                previewView.overlay.add(QrCodeDrawable(barcodeResults))
            }
        )

        cameraController.imageCaptureMode = ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
        cameraController.isPinchToZoomEnabled = false

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    private fun handleBarcodeClick(barcode: Barcode) {
        if (barcode.boundingBox == null || barcode.rawValue == null) return

        cameraController.takePicture(takePictureExecutor, object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                image.setCropRect(barcode.boundingBox)
                viewModel.setBarcodeData(BarcodeData(
                    text = barcode.rawValue!!,
                    image = image.toBitmap()
                ))
            }
        })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeScanner.close()
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

}