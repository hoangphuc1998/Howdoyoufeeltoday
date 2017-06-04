package learn.com.howdoyoufeeltoday.helper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import kotlinx.android.synthetic.main.activity_select_image.*

import learn.com.howdoyoufeeltoday.R
import java.io.File
import java.io.IOException

class SelectImageActivity : AppCompatActivity() {
    val takePhotoRequestCode=1
    val choosePhotoRequestCode=2
    var photoUri:Uri?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_image)
        btnTakePhoto.setOnClickListener {
            takePhoto()
        }
        btnChooseFromGallery.setOnClickListener {
            chooseFromGallery()
        }
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, choosePhotoRequestCode)
        }
    }

    private fun takePhoto() {
        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager)!=null){
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            try {
                val file = File.createTempFile("IMG_", ".jpg", storageDir)
                photoUri = Uri.fromFile(file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, takePhotoRequestCode)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState?.putParcelable("ImageUri", photoUri)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        photoUri = savedInstanceState?.getParcelable<Uri>("ImageUri")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode==takePhotoRequestCode || requestCode==choosePhotoRequestCode){
            if (resultCode== Activity.RESULT_OK){
                var imageUri: Uri?
                if (data == null || data.data == null) {
                    imageUri = photoUri
                } else {
                    imageUri = data.data
                }
                val intent = Intent()
                intent.data = imageUri
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }


}
