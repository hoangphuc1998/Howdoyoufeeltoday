package learn.com.howdoyoufeeltoday.helper

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_view_image.*

import learn.com.howdoyoufeeltoday.R

class ViewImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_image)
        val intent=intent
        val uriString=intent.getStringExtra("URI")
        val photoUri= Uri.parse(uriString)
        val bitmap:Bitmap=ImageHelper.loadSizeLimitedBitmapFromUri(
                photoUri, contentResolver)
        if (bitmap!=null){
            imgPhoto.setImageBitmap(bitmap)
        }
        btnOk.setOnClickListener {
            intent.putExtra("OK_OR_NOT",1)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
        btnCancel.setOnClickListener {
            intent.putExtra("OK_OR_NOT",0)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }
}
