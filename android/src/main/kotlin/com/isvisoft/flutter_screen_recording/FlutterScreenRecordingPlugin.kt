package com.isvisoft.flutter_screen_recording

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterScreenRecordingPlugin(private val registrar: Registrar) :
        MethodCallHandler,
        PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {

    private val mHBRecorder: HBRecorder
    private var mRequestCallback: RequestCallback? = null

    companion object {
        private const val METHOD_ON_RECORDER_LISTENER = "onRecorderListener"
        private const val ERROR_CODE_PERMISSION_DENIED = "ERROR_CODE_PERMISSION_DENIED"
        private const val ERROR_CODE_CANCEL = "ERROR_CODE_CANCEL"
        private const val SCREEN_RECORD_REQUEST_CODE = 333
        private const val PERMISSIONS_REQUEST_CODE = 555

        private lateinit var channel: MethodChannel

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            channel = MethodChannel(registrar.messenger(), "com.isvisoft/flutter_screen_recording")
            val plugin = FlutterScreenRecordingPlugin(registrar)
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
            registrar.addRequestPermissionsResultListener(plugin)
        }
    }

    init {
        mHBRecorder = HBRecorder(registrar.context(), object : HBRecorderListener {
            override fun HBRecorderOnError(errorCode: Int, reason: String?) {
                Log.d("--RECORDING Error", "$errorCode")
                channel.invokeMethod(METHOD_ON_RECORDER_LISTENER, mapOf(Pair("isCompleted", false), Pair("errCode", errorCode)))
            }

            override fun HBRecorderOnComplete() {
                Log.d("--RECORDING FINISH", "finish")
                channel.invokeMethod(METHOD_ON_RECORDER_LISTENER, mapOf(Pair("isCompleted", true), Pair("errCode", 0)))
            }
        })
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "startRecordScreen" -> {
                mRequestCallback = createRequestRecordScreenResultCallback(call, result)
                if (checkPermission()) {
                    ActivityCompat.requestPermissions(registrar.activity(), arrayOf(Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
                } else {
                    requestRecordScreen()
                }
            }
            "stopRecordScreen" -> {
                stopRecordScreen()
                result.success(0)
            }
            "isRecording" -> {
                result.success(isRecording())
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun stopRecordScreen() {
        if (isRecording()) {
            mHBRecorder.stopScreenRecording()
        }
    }

    private fun isRecording(): Boolean = mHBRecorder.isBusyRecording

    private fun createRequestRecordScreenResultCallback(startCall: MethodCall, startResult: Result): RequestCallback {
        return object : RequestCallback {
            override fun onComplete(requestCode: Int, resultCode: Int, data: Intent?) {
                mRequestCallback = null
                //Start screen recording
                startCall.argument<String>("outputPath")?.let { mHBRecorder.setOutputPath(it) }
                startCall.argument<String>("fileName")?.let { mHBRecorder.fileName = it }
                startCall.argument<Boolean>("isAudioEnabled")?.let { mHBRecorder.isAudioEnabled(it) }
                startCall.argument<Boolean>("isRecordHDVideo")?.let { mHBRecorder.recordHDVideo(it) }
                startCall.argument<Int>("audioBitrate")?.let { mHBRecorder.setAudioBitrate(it) }
                startCall.argument<Int>("audioSamplingRate")?.let { mHBRecorder.setAudioSamplingRate(it) }
                startCall.argument<Int>("videoBitrate")?.let { mHBRecorder.setVideoBitrate(it) }
                startCall.argument<Int>("videoFrameRate")?.let { mHBRecorder.setVideoFrameRate(it) }
                startCall.argument<ByteArray>("notificationIcon")?.let { mHBRecorder.setNotificationSmallIcon(it) }
                startCall.argument<String>("notificationTitle")?.let { mHBRecorder.setNotificationTitle(it) }
                startCall.argument<String>("notificationButtonText")?.let { mHBRecorder.setNotificationButtonText(it) }
                startCall.argument<String>("notificationDescription")?.let { mHBRecorder.setNotificationDescription(it) }

                mHBRecorder.startScreenRecording(data, resultCode, registrar.activity())
                startResult.success(mHBRecorder.filePath)
            }

            override fun onError(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
                mRequestCallback = null
                startResult.error(errorCode, errorMessage, errorDetails)
            }
        }
    }

    private fun checkPermission() =
            ActivityCompat.checkSelfPermission(registrar.context(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(registrar.context(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    private fun requestRecordScreen() {
        val mediaProjectionManager = registrar.context().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
        val permissionIntent = mediaProjectionManager?.createScreenCaptureIntent()
        ActivityCompat.startActivityForResult(registrar.activity(), permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.indexOf(PackageManager.PERMISSION_DENIED) < 0) {
                requestRecordScreen()
                return true
            } else {
                mRequestCallback?.onError(ERROR_CODE_PERMISSION_DENIED, null, null)
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mRequestCallback?.onComplete(requestCode, resultCode, data)
                return true
            } else {
                mRequestCallback?.onError(ERROR_CODE_CANCEL, null, null)
            }
        }
        return false
    }

    private interface RequestCallback {
        fun onComplete(requestCode: Int, resultCode: Int, data: Intent?)
        fun onError(errorCode: String?, errorMessage: String?, errorDetails: Any?)
    }
}