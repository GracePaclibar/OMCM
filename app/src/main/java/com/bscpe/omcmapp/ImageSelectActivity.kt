package com.bscpe.omcmapp

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bscpe.omcmapp.Constants.LABELS_PATH
import com.bscpe.omcmapp.Constants.MODEL_PATH

class ImageSelectActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var selectImageButton: Button
    private lateinit var selectedImageView: ImageView
    private lateinit var overlayView: OverlayView

    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_select)

        selectImageButton = findViewById(R.id.selectImageButton)
        selectedImageView = findViewById(R.id.selectedImageView)
        overlayView = findViewById(R.id.overlayView)

        selectImageButton.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK)
        }

        detector = Detector(this, MODEL_PATH, LABELS_PATH, this)
        detector.setup()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedImageView.setImageBitmap(bitmap)

                detector.detect(bitmap)
            }
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            overlayView.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            overlayView.setResults(boundingBoxes)
            overlayView.invalidate()
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right, //Entrance animation
            R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }
}
