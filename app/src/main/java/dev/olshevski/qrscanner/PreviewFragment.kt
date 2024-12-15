package dev.olshevski.qrscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dev.olshevski.qrscanner.databinding.FragmentPreviewBinding
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PreviewFragment : Fragment() {

    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var viewBinding: FragmentPreviewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentPreviewBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.barcodeData.filterNotNull().onEach {
            viewBinding.image.setImageBitmap(it.image)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewBinding.copyTextButton.setOnClickListener {
            viewModel.copyBarcodeTextToClipboard()

        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.clearBarcodeData()
                }
            }
        )
    }

}