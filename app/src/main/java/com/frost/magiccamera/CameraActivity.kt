package com.frost.magiccamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.frost.magiccamera.databinding.ActivityCameraBinding
import com.frost.magiccamera.widgets.FlashSwitchView
import com.frost.magiccamera.widgets.FocusPointDrawable
import com.frost.magiccamera.widgets.MediaActionSwitchView
import com.frost.magiccamera.widgets.RecordButton
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

enum class MediaAction(val value: Int) {
    ACTION_PHOTO(0), ACTION_VIDEO(1)
}

class CameraActivity : AppCompatActivity() {


    private lateinit var viewBinding: ActivityCameraBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null

    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var camera: Camera

    private lateinit var flashSwitchView: FlashSwitchView

    private lateinit var frontBackCameraSwitcher: ImageView

    private lateinit var photoVideoCameraSwitcher: MediaActionSwitchView

    private lateinit var recordButton: RecordButton

    private lateinit var focusPointView: View

    private lateinit var root: View

    private var context: Context  = this

    // 创建 CameraSelector 对象
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    // 记录闪光灯模式
    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO

    private var currentMediaActionState: MediaAction = MediaAction.ACTION_PHOTO

    private var rotateMode: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        root = findViewById(R.id.root)
        viewBinding.cameraBack.setOnClickListener { finish() }

        // 如果已授予权限，开启摄像头
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        initAll()
        initFocus()

        cameraExecutor = Executors.newSingleThreadExecutor()

    }


    /**
     * 获取ID等
     * */
    private fun initAll() {

        //ID
        flashSwitchView = findViewById(R.id.flash_switch_view)
        frontBackCameraSwitcher = findViewById(R.id.front_back_camera_switcher)
        photoVideoCameraSwitcher = findViewById(R.id.photo_video_camera_switcher)
        recordButton = findViewById(R.id.record_button)

        //Listener
        flashSwitchView.setOnClickListener { switchFlash() }
        frontBackCameraSwitcher.setOnClickListener { switchFrontBackCamera() }
        photoVideoCameraSwitcher.setOnClickListener { switchPhotoVideoCamera() }
        recordButton.setOnClickListener { takePhotoOrCaptureVideo() }

        //Animation
        PushDownAnim.setPushDownAnimTo(flashSwitchView)
        PushDownAnim.setPushDownAnimTo(frontBackCameraSwitcher)
        PushDownAnim.setPushDownAnimTo(photoVideoCameraSwitcher)
        PushDownAnim.setPushDownAnimTo(recordButton)

    }

    /**
     * 焦点、缩放初始化
     * */
    @SuppressLint("ClickableViewAccessibility")
    private fun initFocus() {
        focusPointView = findViewById(R.id.focusPoint)
        val previewView: PreviewView = findViewById(R.id.viewFinder)
        val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val meteringPointFactory = previewView.meteringPointFactory
                val focusPoint = meteringPointFactory.createPoint(e.x, e.y)
                //focus
                val meteringAction = FocusMeteringAction.Builder(focusPoint).build()
                camera.cameraControl.startFocusAndMetering(meteringAction)

                showFocusPoint(focusPointView, e.x, e.y)
                return true
            }
        })
        val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    //scale
                    val currentZoomRatio: Float = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    camera.cameraControl.setZoomRatio(detector.scaleFactor * currentZoomRatio)
                    return true
                }
            })

        previewView.setOnTouchListener { _, event ->
            var didConsume = scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                didConsume = gestureDetector.onTouchEvent(event)
            }
            didConsume
        }
    }

    /**
     * 录制或拍摄
     * */
    private fun takePhotoOrCaptureVideo() {

        when (currentMediaActionState) {
            MediaAction.ACTION_PHOTO -> {
                takePhoto()
            }
            MediaAction.ACTION_VIDEO -> {
                captureVideo()
            }
        }
    }


    /**
     * 切换前后摄像头
     * */
    private fun switchFrontBackCamera() {
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) lensFacing =
            CameraSelector.DEFAULT_BACK_CAMERA;
        else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) lensFacing =
            CameraSelector.DEFAULT_FRONT_CAMERA;
        startCamera();
    }

    /**
     * 切换模式
     * */
    private fun switchPhotoVideoCamera() {
        photoVideoCameraSwitcher.toggleMode()
        // 根据 currentMode 值执行相应操作
        currentMediaActionState = when (currentMediaActionState) {
            MediaAction.ACTION_PHOTO -> {
                recordButton.displayVideoRecordStateReady()
                MediaAction.ACTION_VIDEO

            }
            MediaAction.ACTION_VIDEO -> {
                recordButton.displayPhotoState()
                MediaAction.ACTION_PHOTO
            }
        }
    }


    /**
     * 拍摄照片
     * */
    private fun takePhoto() {
        //获取对 ImageCapture 用例的引用
        val imageCapture = imageCapture ?: return

        //创建用于保存图片的 MediaStore 内容值。请使用时间戳，确保 MediaStore 中的显示名是唯一的
        val name =
            SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MagicCamera")
            }
        }

        //输出保存在 MediaStore 中
       /* val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()*/

        //传入 outputOptions、执行器和保存图片时使用的回调
        /*imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                //如果图片拍摄失败或保存图片失败，请添加错误情况以记录失败
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                //将照片保存到我们之前创建的文件中，显示消息框
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })*/

        // 创建名为 "temp" 的子目录
        val tempDir = File(filesDir, "temp")
        // 如果该目录不存在，则创建名为 "temp" 的子目录。
        if (!tempDir.exists()) {
            tempDir.mkdir()
        }

        val imageFile = File(tempDir, "$name.jpeg")
        val outputOptions = contentResolver.let {
            ImageCapture.OutputFileOptions.Builder(imageFile).build()
        }
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                //如果图片拍摄失败或保存图片失败，请添加错误情况以记录失败
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                //将照片保存到我们之前创建的文件中，显示消息框
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val intent = Intent(this@CameraActivity, PreviewActivity::class.java)
                    val uriString = output.savedUri.toString()
                    val bundle = Bundle().apply {
                        putString("uri_string", uriString)
                        putFloat("rotation", rotateMode)
                    }
                    intent.putExtras(bundle)
                    startActivityForResult(intent, REQUEST_FINISH_ACTIVITY);
                    //startActivity(intent)

                }
            })

    }

    /**
     * 拍摄视频
     * */
    private fun captureVideo() {

        //检查是否已创建 VideoCapture 用例
        val videoCapture = this.videoCapture ?: return

        //停用界面
        recordButton.isEnabled = false

        val curRecording = recording
        //如果有正在进行的录制操作，将其停止
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // 创建一个新的录制会话，将系统时间戳作为显示名（以便我们可以捕获多个视频）
        val name =
            SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MagicCamera-Video")
            }
        }

        //使用外部内容选项创建 MediaStoreOutputOptions.Builder
        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        //将输出选项配置为 VideoCapture<Recorder> 的 Recorder 并启用录音
        recording = videoCapture.output.prepareRecording(this, mediaStoreOutputOptions)
            //在此录音中启用音频
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        this@CameraActivity, Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            //启动这项新录制内容，并注册一个 lambda VideoRecordEvent 监听器
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        recordButton.displayVideoRecordStateInProgress()
                        recordButton.isEnabled = true
                    }
                    //完成录制
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg =
                                "Video capture succeeded: " + "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG, "Video capture ends with error: " + "${recordEvent.error}"
                            )
                        }

                        recordButton.displayVideoRecordFinish()
                        recordButton.isEnabled = true
                    }
                }
            }
    }

    /**
     * 启动Camera
     * */
    private fun startCamera() {

        //创建 ProcessCameraProvider 的实例
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 用于将相机的生命周期绑定到应用进程中的 LifecycleOwner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 初始化 Preview 对象，在其上调用 build，从取景器中获取 Surface 提供程序，然后在预览上进行设置
            val preview = Preview.Builder()
                .build()
                .also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val recorder = Recorder.Builder().
            setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            ).build()
            videoCapture = VideoCapture.withOutput(recorder)


            try {
                // 创建一个 try 代码块。在此块内，确保没有任何内容绑定到 cameraProvider，然后将 cameraSelector 和预览对象绑定到 cameraProvider
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, lensFacing, preview, imageCapture, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 闪光灯
     * */
    private fun switchFlash() {

        if (camera.cameraInfo.hasFlashUnit()) {

            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    flashSwitchView.displayFlashAuto()
                }
                ImageCapture.FLASH_MODE_ON -> {
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    flashSwitchView.displayFlashOff()
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    flashMode = ImageCapture.FLASH_MODE_ON
                    flashSwitchView.displayFlashOn()
                }
            }
            changeFlashState()
        }
    }

    /**
     * 修改实例状态
     * */
    private fun changeFlashState() {
        imageCapture?.flashMode = flashMode
    }

    /**
     * 显示焦点
     * */
    private fun showFocusPoint(view: View, x: Float, y: Float) {
        val drawable = FocusPointDrawable()
        val strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            3f,
            context.resources.displayMetrics
        )
        drawable.setStrokeWidth(strokeWidth)

        val alphaAnimation = SpringAnimation(view, DynamicAnimation.ALPHA, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO

            addEndListener { _, _, _, _ ->
                SpringAnimation(view, DynamicAnimation.ALPHA, 0f)
                    .apply {
                        spring.stiffness = SPRING_STIFFNESS_ALPHA_OUT
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    }
                    .start()
            }
        }
        val scaleAnimationX = SpringAnimation(view, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
        }
        val scaleAnimationY = SpringAnimation(view, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
        }

        view.apply {
            background = drawable
            isVisible = true
            translationX = x - width / 2f
            translationY = y - height / 2f
            alpha = 0f
            scaleX = 1.5f
            scaleY = 1.5f
        }

        alphaAnimation.start()
        scaleAnimationX.start()
        scaleAnimationY.start()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //检查请求代码是否正确
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                //如果未授予权限，系统会显示一个消息框，通知用户未授予权限
                Toast.makeText(
                    this, "Permissions not granted by the user.", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                rotateMode = when (orientation) {
                    in 45 until 135 -> 90f
                    in 135 until 225 -> 180f
                    in 225 until 315 -> 270f //left
                    else -> 0f
                }
            }
        }
    }

    // 接收从OtherActivity中返回来的result信息，进而决定是否finish掉PreviewActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FINISH_ACTIVITY && resultCode == RESULT_OK) {
            //表示OtherActivity已经finish了且成功处理了任务
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_FINISH_ACTIVITY = 1
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        // animation constants for focus point
        private const val SPRING_STIFFNESS_ALPHA_OUT = 100f
        private const val SPRING_STIFFNESS = 800f
        private const val SPRING_DAMPING_RATIO = 0.35f
    }

}