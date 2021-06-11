package com.mmunoz.camera_lib.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mmunoz.camera_lib.databinding.CameraLibFragmentBinding
import com.mmunoz.camera_lib.ui.managers.CameraHandler
import com.mmunoz.camera_lib.ui.managers.CameraHandlerImpl

class CameraFragment : Fragment() {

    private var _binding: CameraLibFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraHandler: CameraHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CameraLibFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraHandler = CameraHandlerImpl(
            requireActivity() as AppCompatActivity,
            binding.cameraView,
            viewLifecycleOwner
        )
        cameraHandler.start()
//        binding.root.setOnKeyListener { _, keyCode, _ ->
//            when (keyCode) {
//                KeyEvent.KEYCODE_VOLUME_UP -> {
//                    if (linearZoom <= 0.9) {
//                        linearZoom += 0.1f
//                    }
//                    cameraControl?.setLinearZoom(linearZoom)
//                    true
//                }
//                KeyEvent.KEYCODE_VOLUME_DOWN -> {
//                    if (linearZoom >= 0.1) {
//                        linearZoom -= 0.1f
//                    }
//                    cameraControl?.setLinearZoom(linearZoom)
//                    true
//                }
//                else -> false
//            }
//        }
    }

    override fun onDestroyView() {
        _binding = null
        cameraHandler.clear()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): CameraFragment {
            return CameraFragment().apply { }
        }
    }
}