package com.moyinoluwa.camerabarcodedetection

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.moyinoluwa.camerabarcodedetection.ui.camera.CameraSourcePreview
import com.moyinoluwa.camerabarcodedetection.ui.camera.GraphicOverlay
import java.io.IOException

private const val TAG = "BarcodeTracker"
private const val RC_HANDLE_GMS = 9001
// permission request codes need to be < 256
private const val RC_HANDLE_CAMERA_PERM = 2

class MainActivity : AppCompatActivity() {

    private lateinit var cameraSourcePreview: CameraSourcePreview
    lateinit var graphicOverlay: GraphicOverlay

    private var cameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraSourcePreview = findViewById(R.id.preview)
        graphicOverlay = findViewById(R.id.overlay)

        //Check for the camera permission before accessing the camera.  If the
        //permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(this, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(graphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private fun createCameraSource() {
        val detector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build()

        detector.setProcessor(MultiProcessor.Builder(GraphicBarcodeTrackerFactory()).build())

        if (!detector.isOperational) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Barcode detector dependencies are not yet available.")
        }

//        cameraSource = CameraSource.Builder(this, detector)
//                .setRequestedPreviewSize(1024, 768)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
//                .setRequestedFps(15.0f)
//                .build()

        cameraSource = CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 1024)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .build()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        cameraSourcePreview.stop()
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *

     * @param requestCode  The request code passed in [.requestPermissions].
     * *
     * @param permissions  The requested permissions. Never null.
     * *
     * @param grantResults The grant results for the corresponding permissions
     * *                     which is either [PackageManager.PERMISSION_GRANTED]
     * *                     or [PackageManager.PERMISSION_DENIED]. Never null.
     * *
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            createCameraSource()
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { _, _ -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Barcode Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {

        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }

        if (cameraSource != null) {
            try {
                cameraSourcePreview.start(cameraSource as CameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSourcePreview.release()
                cameraSource = null
            }

        }
    }

    //==============================================================================================
    // Graphic Barcode Tracker
    //==============================================================================================

    /**
     * Factory for creating a barcode tracker to be associated with a new barcode.  The multiprocessor
     * uses this factory to create barcode trackers as needed -- one for each individual.
     */
    private inner class GraphicBarcodeTrackerFactory : MultiProcessor.Factory<Barcode> {

        override fun create(barcode: Barcode): Tracker<Barcode> = GraphicBarcodeTracker(graphicOverlay)
    }

    /**
     * Barcode tracker for each detected individual. This maintains a barcode graphic within the app's
     * associated barcode overlay.
     */
    private inner class GraphicBarcodeTracker internal constructor(
            private val mOverlay: GraphicOverlay)
        : Tracker<Barcode>() {

        private val mBarcodeGraphic: BarcodeGraphic = BarcodeGraphic(mOverlay)

        /**
         * Start tracking the detected barcode instance within the face overlay.
         */
        override fun onNewItem(barcodeId: Int, item: Barcode) {
            mBarcodeGraphic.setId(barcodeId)
        }

        /**
         * Update the position/characteristics of the barcode within the overlay.
         */
        override fun onUpdate(detectionResults: Detector.Detections<Barcode>, barcode: Barcode) {
            mOverlay.add(mBarcodeGraphic)
            mBarcodeGraphic.updateBarcode(barcode)
        }

        /**
         * Hide the graphic when the corresponding barcode was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the barcode was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detector.Detections<Barcode>) {
            mOverlay.remove(mBarcodeGraphic)
        }

        /**
         * Called when the barcode is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            mOverlay.remove(mBarcodeGraphic)
        }
    }
}