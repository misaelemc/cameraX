package com.mmunoz.camera_lib.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mmunoz.camera_lib.databinding.CameraLibActivityPreviewBinding
import com.mmunoz.camera_lib.ui.activities.PreviewActivityResultContract.Companion.STATUS_FILE
import java.io.File

// TODO -- Add image transition
class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: CameraLibActivityPreviewBinding

    private var previewFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraLibActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        previewFile = deserializeFrom(intent.extras)

        bindPhoto()
        binding.backImageButton.setOnClickListener { discardFile() }
        binding.buttonAccept.setOnClickListener { acceptFile() }
        binding.buttonCancel.setOnClickListener { discardFile() }
    }

    override fun onDestroy() {
        super.onDestroy()
        previewFile = null
    }

    private fun bindPhoto() {
        if (previewFile != null) {
            val savedUri = Uri.fromFile(previewFile)
            Glide.with(this)
                .load(savedUri)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(binding.imageView)
        }
    }

    private fun acceptFile() {
        sendResult(true)
    }

    private fun discardFile() {
        if (previewFile != null && previewFile!!.exists()) {
            previewFile!!.deleteOnExit()
            sendResult(false)
        }
    }

    private fun sendResult(isAccepted: Boolean) {
        val intent = Intent()
            .putExtra(STATUS_FILE, isAccepted)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {

        private const val EXTRA_FILE = "extra_file"

        fun deserializeFrom(extras: Bundle?): File? {
            return extras?.getString(EXTRA_FILE)?.let { File(it) }
        }
    }
}