package com.moyinoluwa.camerabarcodedetection.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.vision.CameraSource
import java.io.IOException
import java.lang.Math.max
import java.lang.Math.min

private const val TAG = "CameraSourcePreview"

class CameraSourcePreview(private val mContext: Context, attrs: AttributeSet)
    : ViewGroup(mContext, attrs) {

    private val surfaceView: SurfaceView
    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null
    private var overlay: GraphicOverlay? = null

    private val isPortraitMode: Boolean
        get() {
            val orientation = mContext.resources.configuration.orientation
            if (orientation == ORIENTATION_LANDSCAPE) return false
            if (orientation == ORIENTATION_PORTRAIT) return true

            Log.d(TAG, "isPortraitMode returning false by default")
            return false
        }

    init {
        startRequested = false
        surfaceAvailable = false

        surfaceView = SurfaceView(mContext)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) stop()

        this.cameraSource = cameraSource

        this.cameraSource?.let {
            startRequested = true
            startIfReady()
        }
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource, overlay: GraphicOverlay) {
        this.overlay = overlay
        start(cameraSource)
    }

    fun stop() {
        cameraSource?.stop()
    }

    fun release() {
        cameraSource?.let {
            it.release()
            cameraSource = null
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    private fun startIfReady() {
        cameraSource?.let { cameraSource ->
            if (startRequested && surfaceAvailable) {
                cameraSource.start(surfaceView.holder)

                overlay?.let {
                    val size = cameraSource.previewSize
                    val min = min(size.width, size.height)
                    val max = max(size.width, size.height)
                    if (isPortraitMode) {
                        // Swap width and height sizes when in portrait, since it will be rotated by
                        // 90 degrees
                        it.setCameraInfo(min, max, cameraSource.cameraFacing)
                    } else {
                        it.setCameraInfo(max, min, cameraSource.cameraFacing)
                    }
                    it.clear()
                }
                startRequested = false
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var width = 320
        var height = 240

        cameraSource?.let {
            it.previewSize?.let {
                width = it.width
                height = it.height
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode) {
            val tmp = width
            width = height
            height = tmp
        }

        val layoutWidth = right - left
        val layoutHeight = bottom - top

        // Computes height and width for potentially doing fit width.
        var childWidth = layoutWidth
        var childHeight = (layoutWidth.toFloat() / width.toFloat() * height).toInt()

        // If height is too tall using fit width, does fit height instead.
        if (childHeight < layoutHeight) {
            childHeight = layoutHeight
            childWidth = (layoutHeight.toFloat() / height.toFloat() * width).toInt()
        }

        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }

    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {

        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }
}