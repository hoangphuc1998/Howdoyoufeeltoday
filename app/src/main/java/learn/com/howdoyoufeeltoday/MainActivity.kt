package learn.com.howdoyoufeeltoday

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import learn.com.howdoyoufeeltoday.helper.ImageHelper
import learn.com.howdoyoufeeltoday.helper.SelectImageActivity
import learn.com.howdoyoufeeltoday.helper.ViewImageActivity
import java.util.*


class MainActivity : AppCompatActivity() {
    var conversationList=ArrayList<Conversation>()
    var conversationAdapter:ConversationAdapter=ConversationAdapter(this,conversationList)
    val audioRequestCode=111
    val voiceRecognitionRequestCode=1
    val selectImageRequestCode=2
    val viewImageRequestCode=3
    // The URI of the image selected to detect.
    var mImageUri: Uri? = null

    // The image selected to detect.
    var mBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addControls()
        addEvents()
    }

    private fun addEvents() {
        btnSpeech.setOnClickListener {
            accessGoogleSpeechRecognition()
        }
        txtInput.setOnEditorActionListener(object:TextView.OnEditorActionListener
        {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId==EditorInfo.IME_ACTION_DONE){
                    processResponse(v.text.toString())
                    val inputManager=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
                    v.text=""
                    return true
                }
                return false
            }

        })
    }
    //Handle the input text and talk back
    private fun  processResponse(text: String) {
        conversationList.add(Conversation(text,1))
        conversationAdapter.notifyDataSetChanged()
        rvConversation.smoothScrollToPosition(conversationList.size-1)
        if (text.contains("guess")){
            val intent=Intent(this,SelectImageActivity::class.java)
            startActivityForResult(intent,selectImageRequestCode)
        }
    }

    private fun accessGoogleSpeechRecognition() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
            showRecognitionIntent()
        }else
        {
            requestForAudioPermissions()
        }
    }

    private fun requestForAudioPermissions() {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "You must allow phone to record audio", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO), audioRequestCode)
    }

    private fun showRecognitionIntent() {
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.ACTION_RECOGNIZE_SPEECH,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Voice Recognition")
        try{
            startActivityForResult(intent,voiceRecognitionRequestCode)
        }catch(ex:ActivityNotFoundException){
            ex.printStackTrace()
            Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addControls() {
        rvConversation.adapter=conversationAdapter
        val layoutManager=LinearLayoutManager(this)
        rvConversation.layoutManager=layoutManager
        conversationList.add(Conversation("How do you feel today?",0))
        conversationAdapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            audioRequestCode->{
                if (grantResults.size>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    showRecognitionIntent()
                }else{
                    val listener= DialogInterface.OnClickListener { dialog, which -> return@OnClickListener }
                    val dialogBuilder=AlertDialog.Builder(this)
                    dialogBuilder.setTitle("Voice Recognition")
                            .setMessage("You need to allow to record audio to recognize voice")
                            .setPositiveButton("OK",listener)
                            .show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            voiceRecognitionRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    processResponse(result.get(0))
                } else {
                    Toast.makeText(this, "Voice is not recognized", Toast.LENGTH_SHORT).show()
                }
            }

            selectImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data?.data

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, contentResolver)
                    if (mBitmap != null) {
                        // Show the image on screen.
                        val newIntent=Intent(this,ViewImageActivity::class.java)
                        newIntent.putExtra("URI",mImageUri)
                        startActivityForResult(newIntent,viewImageRequestCode)
                    }
                }
            }
            viewImageRequestCode->{
                val okOrNot=data?.getIntExtra("OK_OR_NOT",1)
                if (okOrNot==0){
                    val intent=Intent(this,SelectImageActivity::class.java)
                    startActivityForResult(intent,selectImageRequestCode)
                }else {
                    doRecognize(mImageUri)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun doRecognize(mImageUri: Uri?) {

    }

}
