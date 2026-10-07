package dev.cameronpak.muser1

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Camera
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local-only camera. Captures are held in memory until the user explicitly saves them. */
@Suppress("DEPRECATION")
class CameraActivity : Activity(), SurfaceHolder.Callback {
    private var camera: Camera? = null
    private lateinit var surface: PreviewSurface
    private lateinit var photo: ImageView
    private lateinit var capture: Button
    private lateinit var save: Button
    private lateinit var ask: Button
    private lateinit var translatePhoto: Button
    private lateinit var photoTools: LinearLayout
    private lateinit var retake: Button
    private lateinit var info: TextView
    private lateinit var switchCamera: Button
    private var selectedFacing = Camera.CameraInfo.CAMERA_FACING_BACK
    private var jpeg: ByteArray? = null
    private var busy = false
    private var resumed = false
    private var surfaceReady = false
    private var captureDown = -1L
    private var lastWheel = 0L
    private val wheelHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingFacing: Int? = null
    private val applyFacing = Runnable {
        val facing = pendingFacing
        pendingFacing = null
        if (facing != null && resumed && !busy && jpeg == null && (facing != selectedFacing || camera == null)) {
            lastWheel = SystemClock.uptimeMillis()
            releaseCamera()
            selectedFacing = facing
            openCamera()
        }
    }
    private fun t(ko: String, ja: String, en: String) = UiText.text(this, ko, ja, en)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun button(text: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 12f; isAllCaps = false
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        setPadding(dp(10), 0, dp(10), 0); elevation = 0f; stateListAnimator = null
        val orange = Color.rgb(255,139,66)
        fun shape(color: Int) = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(24).toFloat(); setColor(color)
            setStroke(dp(1), if (primary) color else Color.rgb(55,56,54))
        }
        val states = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), shape(Color.rgb(35,36,34)))
            addState(intArrayOf(android.R.attr.state_pressed), shape(if (primary) Color.rgb(225,115,45) else Color.rgb(48,49,46)))
            addState(intArrayOf(), shape(if (primary) orange else Color.rgb(23,24,23)))
        }
        background = android.graphics.drawable.InsetDrawable(states, dp(3), dp(8), dp(3), dp(8))
        setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(Color.GRAY, if (primary) Color.rgb(10,11,10) else Color.rgb(242,233,221))))
        setOnClickListener { action() }
    }
    private fun selectFacing(facing: Int) {
        if (!resumed || busy || jpeg != null) return
        wheelHandler.removeCallbacks(applyFacing)
        pendingFacing = null
        if (facing == selectedFacing && camera != null) return
        pendingFacing = facing
        val delay = (700L - (SystemClock.uptimeMillis() - lastWheel)).coerceAtLeast(0L)
        wheelHandler.postDelayed(applyFacing, delay)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10,11,10)) }
        val top = LinearLayout(this).apply { setPadding(dp(12), 0, dp(12), 0) }
        top.addView(button("‹ " + t("뒤로", "戻る", "Back")) { finish() }, LinearLayout.LayoutParams(dp(76), dp(48)))
        top.addView(TextView(this).apply { text = t("카메라", "カメラ", "Camera"); textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(48), 1f))
        switchCamera = button(t("전면 ↻", "前面 ↻", "Front ↻")) {
            selectFacing(if (selectedFacing == Camera.CameraInfo.CAMERA_FACING_BACK) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK)
        }
        top.addView(switchCamera, LinearLayout.LayoutParams(dp(96), dp(48)))
        root.addView(top)
        val preview = FrameLayout(this)
        surface = PreviewSurface().apply { holder.addCallback(this@CameraActivity) }
        photo = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; visibility = View.GONE }
        preview.addView(surface, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        preview.addView(photo, FrameLayout.LayoutParams(-1, -1))
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        info = TextView(this).apply { textSize = 11f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        root.addView(info, LinearLayout.LayoutParams(-1, dp(44)))
        photoTools = LinearLayout(this).apply { gravity = Gravity.CENTER }
        translatePhoto = button(t("사진 속 글자 번역", "写真の文字を翻訳", "Translate photo text")) { choosePhotoLanguage() }
        photoTools.addView(translatePhoto, LinearLayout.LayoutParams(dp(220), dp(48)))
        root.addView(photoTools)
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), dp(8)) }
        retake = button(t("다시 촬영", "撮り直す", "Retake")) { jpeg = null; photo.setImageDrawable(null); showPreview(); openCamera() }
        capture = button("● " + t("촬영", "撮影", "Capture"), primary = true) { shoot() }
        save = button(t("사진 저장", "写真を保存", "Save photo")) { savePhoto() }
        ask = button(t("Muse에 질문", "Museに質問", "Ask Muse"), primary = true) { askMuse() }
        bottom.addView(ask, LinearLayout.LayoutParams(dp(116), dp(48)))
        bottom.addView(retake, LinearLayout.LayoutParams(dp(100), dp(48)))
        bottom.addView(capture, LinearLayout.LayoutParams(dp(120), dp(48)))
        bottom.addView(save, LinearLayout.LayoutParams(dp(112), dp(48)))
        root.addView(bottom)
        setContentView(root)
        showPreview()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
    }

    override fun onWindowFocusChanged(focused: Boolean) {
        super.onWindowFocusChanged(focused)
        if (focused && android.os.Build.VERSION.SDK_INT >= 30) window.insetsController?.apply {
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsets.Type.systemBars())
        }
    }
    override fun onResume() { super.onResume(); resumed = true; foreground = this; openCamera() }
    override fun onPause() {
        resumed = false; if (foreground === this) foreground = null
        wheelHandler.removeCallbacks(applyFacing); pendingFacing = null
        captureDown = -1; releaseCamera(); super.onPause()
    }
    override fun onDestroy() { jpeg = null; photo.setImageDrawable(null); super.onDestroy() }
    override fun surfaceCreated(holder: SurfaceHolder) { surfaceReady = true; openCamera() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
    override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceReady = false; releaseCamera() }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 10 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) openCamera()
        else info.text = t("카메라 권한이 필요합니다.", "カメラの権限が必要です。", "Camera permission is required.")
    }
    private fun showPreview() {
        surface.visibility = View.VISIBLE; photo.visibility = View.GONE
        photoTools.visibility = View.GONE
        capture.visibility = View.VISIBLE; capture.isEnabled = camera != null
        retake.visibility = View.GONE; save.visibility = View.GONE; ask.visibility = View.GONE
        switchCamera.isEnabled = true
        info.text = t("휠: 전면·후면 전환 · 측면 버튼: 촬영", "ホイール: 前面・背面 · サイド: 撮影", "Wheel: front/back · Side button: capture")
    }
    private fun openCamera() {
        if (!resumed || !surfaceReady || camera != null || jpeg != null || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        var opened: Camera? = null
        try {
            val id = (0 until Camera.getNumberOfCameras()).firstOrNull {
                val c = Camera.CameraInfo(); Camera.getCameraInfo(it, c); c.facing == selectedFacing
            } ?: error("Requested facing unavailable")
            opened = Camera.open(id)
            val details = Camera.CameraInfo(); Camera.getCameraInfo(id, details)
            val parameters = opened.parameters
            val size = parameters.supportedPictureSizes.filter { it.width <= 1920 && it.height <= 1440 }.maxByOrNull { it.width * it.height }
                ?: parameters.supportedPictureSizes.minBy { it.width * it.height }
            parameters.setPictureSize(size.width, size.height)
            parameters.setRotation(details.orientation)
            parameters.jpegQuality = 90
            if (parameters.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) == true)
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            opened.parameters = parameters
            val previewSize = parameters.previewSize
            surface.ratio = if (details.orientation % 180 == 90) previewSize.height.toFloat() / previewSize.width else previewSize.width.toFloat() / previewSize.height
            surface.requestLayout()
            opened.setDisplayOrientation(if (details.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) (360 - details.orientation) % 360 else details.orientation)
            opened.setPreviewDisplay(surface.holder)
            opened.setOneShotPreviewCallback { _, _ -> android.util.Log.i("MuseCameraCheck", "preview_frame cameraId=$id facing=${details.facing}") }
            opened.startPreview(); camera = opened; capture.isEnabled = true; switchCamera.isEnabled = true; busy = false
            val front = details.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
            switchCamera.text = if (front) t("후면 ↻", "背面 ↻", "Back ↻") else t("전면 ↻", "前面 ↻", "Front ↻")
            switchCamera.contentDescription = if (front) t("후면 카메라로 전환", "背面カメラに切替", "Switch to back camera") else t("전면 카메라로 전환", "前面カメラに切替", "Switch to front camera")
            info.text = t("휠: 전면·후면 전환 · 측면 버튼: 촬영", "ホイール: 前面・背面 · サイド: 撮影", "Wheel: front/back · Side button: capture")
        } catch (_: Exception) {
            opened?.release(); camera = null; capture.isEnabled = false
            switchCamera.text = t("다른 카메라 ↻", "別のカメラ ↻", "Switch ↻")
            info.text = t("카메라를 열 수 없습니다. 다시 열어 주세요.", "カメラを開けません。開き直してください。", "Camera unavailable. Reopen this screen.")
        }
    }
    private fun releaseCamera() { camera?.release(); camera = null; busy = false }
    internal fun shoot() {
        val active = camera ?: return
        if (!resumed || busy || jpeg != null) return
        wheelHandler.removeCallbacks(applyFacing); pendingFacing = null
        busy = true; capture.isEnabled = false; switchCamera.isEnabled = false
        try {
            active.takePicture(null, null, Camera.PictureCallback { data, _ ->
                if (!resumed) return@PictureCallback
                busy = false
                if (data == null || data.size > 12_000_000) { info.text = t("촬영 실패", "撮影失敗", "Capture failed"); releaseCamera(); openCamera(); return@PictureCallback }
                jpeg = data; releaseCamera()
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = 2 })
                val orientation = runCatching { android.media.ExifInterface(java.io.ByteArrayInputStream(data)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
                val degrees = when (orientation) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
                photo.setImageBitmap(if (bitmap != null && degrees != 0f) android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, android.graphics.Matrix().apply { postRotate(degrees) }, true) else bitmap)
                photo.visibility = View.VISIBLE; surface.visibility = View.GONE
                capture.visibility = View.GONE; retake.visibility = View.VISIBLE; save.visibility = View.VISIBLE; save.isEnabled = true; ask.visibility = View.VISIBLE; ask.isEnabled = true
                photoTools.visibility = if(intent.hasExtra("pet_mission_token"))View.GONE else View.VISIBLE; translatePhoto.isEnabled = true
                if(intent.hasExtra("hunt_prompt")) {
                    val lang=InputLanguage.entries.firstOrNull{it.name==intent.getStringExtra("hunt_language") && it!=InputLanguage.AUTO} ?: InputLanguage.KOREAN
                    askMuse(intent.getStringExtra("hunt_prompt"),lang)
                } else if (intent.getBooleanExtra("translate_photo", false)) {
                    intent.removeExtra("translate_photo")
                    choosePhotoLanguage()
                }
                info.text = t("Muse에 사진을 보내 질문하거나 기기에 저장하세요.", "Museに写真で質問、または端末に保存。", "Ask Muse about this photo, or save it locally.")
            })
        } catch (_: Exception) { releaseCamera(); openCamera(); info.text = t("촬영 실패", "撮影失敗", "Capture failed") }
    }
    private fun choosePhotoLanguage() {
        if (busy || jpeg == null) return
        val languages = arrayOf(InputLanguage.KOREAN, InputLanguage.JAPANESE, InputLanguage.ENGLISH)
        android.app.AlertDialog.Builder(this).setTitle(t("번역할 언어", "翻訳先の言語", "Translate into"))
            .setItems(languages.map { it.label }.toTypedArray()) { _, index ->
                askMuse(PhotoPrompts.translate(languages[index]), languages[index])
            }.setNegativeButton(t("취소", "キャンセル", "Cancel"), null).show()
    }

    private fun askMuse(preset: String? = null, replyLanguage: InputLanguage = InputLanguage.KOREAN) {
        val data = jpeg ?: return
        if (busy) return
        val input = EditText(this).apply {
            setText(preset ?: t("이 사진에 무엇이 보이는지 한국어로 설명해 줘.", "この写真に何が写っているか日本語で説明して。", "Describe what you see in this photo in English."))
            filters = arrayOf(android.text.InputFilter.LengthFilter(1000))
            minLines = 2; maxLines = 4
            if(intent.hasExtra("pet_mission_token")){setText(PetActivityText.target(this@CameraActivity,intent.getIntExtra("pet_mission_target",0)));isEnabled=false}
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(t("사진으로 Muse에 질문", "写真でMuseに質問", "Ask Muse about photo"))
            .setMessage(t("사진과 질문이 Muse로 전송됩니다.", "写真と質問をMuseに送信します。", "The photo and question will be sent to Muse."))
            .setView(input)
            .setNegativeButton(t("취소", "キャンセル", "Cancel"), null)
            .setPositiveButton(t("보내기", "送信", "Send")) { _, _ ->
                val question = input.text.toString().trim()
                if (question.isBlank()) return@setPositiveButton
                busy = true; translatePhoto.isEnabled = false; ask.isEnabled = false; retake.isEnabled = false; save.isEnabled = false
                info.text = t("사진 준비 중…", "写真を準備中…", "Preparing photo…")
                Thread {
                    val compressed = runCatching {
                        val decoded = checkNotNull(BitmapFactory.decodeByteArray(data, 0, data.size))
                        val exif = android.media.ExifInterface(java.io.ByteArrayInputStream(data))
                        val rotation = when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
                        val scale = minOf(1f, 1024f / maxOf(decoded.width, decoded.height))
                        val bitmap = android.graphics.Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, android.graphics.Matrix().apply { postScale(scale, scale); postRotate(rotation) }, true)
                        val out = java.io.ByteArrayOutputStream()
                        for (quality in intArrayOf(80, 65, 50, 35, 20)) {
                            out.reset(); check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out))
                            if (out.size() <= 180000) break
                        }
                        if (bitmap !== decoded) bitmap.recycle()
                        decoded.recycle()
                        check(out.size() <= 180000)
                        out.toByteArray()
                    }.getOrNull()
                    runOnUiThread {
                        busy = false
                        if (isDestroyed || !resumed) return@runOnUiThread
                        if (compressed == null) {
                            translatePhoto.isEnabled = true; ask.isEnabled = true; retake.isEnabled = true; save.isEnabled = true
                            info.text = t("사진 준비 실패. 다시 촬영해 주세요.", "写真の準備に失敗しました。撮り直してください。", "Photo preparation failed. Retake the photo.")
                        } else {
                            PhotoQuestion.pending = PhotoQuestion.Request(compressed, question, replyLanguage, if(intent.hasExtra("pet_seed"))intent.getLongExtra("pet_seed",0) else null, intent.getStringExtra("pet_mission_token"), intent.getIntExtra("pet_mission_target",-1))
                            finish()
                        }
                    }
                }.start()
            }.show()
    }

    private fun savePhoto() {
        val data = jpeg ?: return
        if (busy) return
        busy = true; save.isEnabled = false
        Thread {
            var uri: android.net.Uri? = null
            val success = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Muse_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Muse")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Insert failed")
                checkNotNull(contentResolver.openOutputStream(uri!!)).use { it.write(data) }
                check(contentResolver.update(uri!!, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) > 0)
            }.isSuccess
            if (!success) uri?.let { runCatching { contentResolver.delete(it, null, null) } }
            runOnUiThread {
                busy = false
                if (!isDestroyed) { save.isEnabled = !success; info.text = if (success) t("사진 저장 완료", "写真を保存しました", "Photo saved") else t("저장 실패", "保存失敗", "Save failed") }
            }
        }.start()
    }
    internal fun cameraKey(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_PAIRING) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) captureDown = event.downTime
            if (event.action == KeyEvent.ACTION_UP) {
                if (captureDown == event.downTime && !event.isCanceled) shoot()
                captureDown = -1
            }
            return true
        }
        if (event.keyCode in intArrayOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) selectFacing(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK)
            return true
        }
        return false
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = cameraKey(event) || super.dispatchKeyEvent(event)
    private inner class PreviewSurface : SurfaceView(this@CameraActivity) {
        var ratio = 3f / 4f
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            if (width.toFloat() / height > ratio) setMeasuredDimension((height * ratio).toInt(), height)
            else setMeasuredDimension(width, (width / ratio).toInt())
        }
    }
    companion object { internal var foreground: CameraActivity? = null; private set }
}
