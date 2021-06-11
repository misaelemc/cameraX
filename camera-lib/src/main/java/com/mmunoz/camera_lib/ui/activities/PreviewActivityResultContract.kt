package com.mmunoz.camera_lib.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class PreviewActivityResultContract : ActivityResultContract<String, Boolean>() {

    companion object {
        const val EXTRA_FILE = "extra_file"
        const val STATUS_FILE = "status_file"
    }

    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, PreviewActivity::class.java).apply {
            putExtra(EXTRA_FILE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return when (resultCode) {
            Activity.RESULT_OK -> intent?.getBooleanExtra(STATUS_FILE, false) ?: false
            else -> false
        }
    }
}