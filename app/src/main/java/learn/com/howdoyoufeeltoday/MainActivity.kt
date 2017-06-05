package learn.com.howdoyoufeeltoday

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.AsyncTask
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
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.microsoft.projectoxford.emotion.EmotionServiceClient
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient
import com.microsoft.projectoxford.emotion.contract.FaceRectangle
import com.microsoft.projectoxford.emotion.contract.Order
import com.microsoft.projectoxford.emotion.contract.RecognizeResult
import com.microsoft.projectoxford.face.FaceServiceRestClient
import kotlinx.android.synthetic.main.activity_main.*
import learn.com.howdoyoufeeltoday.helper.ImageHelper
import learn.com.howdoyoufeeltoday.helper.SelectImageActivity
import learn.com.howdoyoufeeltoday.helper.ViewImageActivity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
    var client: EmotionServiceClient? = null
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
        //Add Client for recognize emotion
        if (client == null) {
            client = EmotionServiceRestClient(getString(R.string.subscription_key))
        }
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
                        newIntent.putExtra("URI",mImageUri.toString())
                        startActivityForResult(newIntent,viewImageRequestCode)
                    }
                }
            }
            viewImageRequestCode->{
                if (resultCode== Activity.RESULT_OK) {
                    val okOrNot = data?.getIntExtra("OK_OR_NOT", 1)
                    if (okOrNot == 0) {
                        val intent = Intent(this, SelectImageActivity::class.java)
                        startActivityForResult(intent, selectImageRequestCode)
                    } else {
                        doRecognize()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun doRecognize() {
        try {
            doRequest(false).execute()
        } catch (e: Exception) {
            processResponse("Error encountered. Exception is: " + e.toString())
        }


        /*val faceSubscriptionKey = getString(R.string.faceSubscription_key)
        if (faceSubscriptionKey.equals("Please_add_the_face_subscription_key_here", ignoreCase = true)) {
            processResponse("\n\nThere is no face subscription key in res/values/strings.xml. Skip the sample for detecting emotions using face rectangles\n")
        } else {
            // Do emotion detection using face rectangles provided by Face API.
            try {
                doRequest(true).execute()
            } catch (e: Exception) {
                processResponse("Error encountered. Exception is: " + e.toString())
            }

        }*/
    }

    //AsyncTask helps push request to Microsoft server
    inner class doRequest(useFaceRectangle: Boolean) : AsyncTask<String, String, List<RecognizeResult>>() {
        var useFaceRectangles=useFaceRectangle
        override fun doInBackground(vararg params: String?): List<RecognizeResult>? {
            /*if (!useFaceRectangles) {*/
                try {
                    return processWithAutoFaceDetection()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }

            /*} else {
                try {
                    return processWithFaceRectangles()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }*/

        }

        override fun onPostExecute(result: List<RecognizeResult>?) {
            super.onPostExecute(result)
            if(result?.size==0){
                processResponse("No emotion detected")
            }else{
                val bitmapCopy = mBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                val faceCanvas = Canvas(bitmapCopy)
                faceCanvas.drawBitmap(mBitmap, 0f, 0f, null)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                paint.color = Color.RED
                //Only choose the first face
                val r=result?.get(0)
                val collection : List<Map.Entry<String, Double>>? = r?.scores?.ToRankedList(Order.DESCENDING)
                val emotion=collection?.get(0)?.key.toString()

                processResponse(emotion)
            }
        }
    }

    private fun processWithFaceRectangles(): List<RecognizeResult>? {
        val gson = Gson()

        // Put the image into an input stream for detection.
        val output = ByteArrayOutputStream()
        mBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, output)
        val inputStream = ByteArrayInputStream(output.toByteArray())

        var timeMark = System.currentTimeMillis()
        Log.d("emotion", "Start face detection using Face API")
        var faceRectangles:Array<FaceRectangle?> = arrayOfNulls<FaceRectangle>(0)
        val faceSubscriptionKey = getString(R.string.faceSubscription_key)
        val faceClient = FaceServiceRestClient(faceSubscriptionKey)
        val faces = faceClient.detect(inputStream, false, false, null)
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", System.currentTimeMillis() - timeMark))

        if (faces != null) {
            faceRectangles= arrayOfNulls<FaceRectangle>(faces.size)

            for (i in faceRectangles.indices) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                val rect = faces[i].faceRectangle
                faceRectangles[i] = com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height)
            }
        }

        var result: List<RecognizeResult>? = null
        if (faceRectangles != null) {
            inputStream.reset()

            timeMark = System.currentTimeMillis()
            Log.d("emotion", "Start emotion detection using Emotion API")
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.client?.recognizeImage(inputStream, faceRectangles)

            val json = gson.toJson(result)
            Log.d("result", json)
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", System.currentTimeMillis() - timeMark))
        }
        return result
    }

    private fun processWithAutoFaceDetection(): List<RecognizeResult>?{
        Log.d("emotion", "Start emotion detection with auto-face detection")
        val gson = Gson()

        // Put the image into an input stream for detection.
        val output = ByteArrayOutputStream()
        mBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, output)
        val inputStream = ByteArrayInputStream(output.toByteArray())

        val startTime = System.currentTimeMillis()
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        var result: List<RecognizeResult>? = null
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client?.recognizeImage(inputStream)

        val json = gson.toJson(result)
        Log.d("result", json)
        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", System.currentTimeMillis() - startTime))
        return result
    }
}
