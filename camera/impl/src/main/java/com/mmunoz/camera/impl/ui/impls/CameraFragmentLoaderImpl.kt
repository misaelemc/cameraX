package com.mmunoz.camera.impl.ui.impls

import com.mmunoz.camera.api.ui.loader.CameraFragmentLoader
import com.mmunoz.camera.impl.ui.fragments.CameraFragment
import com.mmunoz.core.BaseFragment
import javax.inject.Inject

class CameraFragmentLoaderImpl @Inject constructor() : CameraFragmentLoader {

    override fun getFragment(): BaseFragment {
        return CameraFragment.newInstance()
    }
}