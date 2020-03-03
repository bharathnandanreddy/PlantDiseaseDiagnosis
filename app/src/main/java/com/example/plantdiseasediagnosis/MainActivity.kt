package com.example.plantdiseasediagnosis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alert_layout.view.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val READ_EXST=4
    private val mGalleryRequestCode=1
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap
    private val mInputSize = 299
    private val mModelPath = "plant_disease_model.tflite"
    private val mLabelPath = "plant_labels.txt"
    private val GALLERY = 1
    private val CAMERA = 2



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Glide.with(this).load("http://192.168.0.8/").placeholder(R.drawable.ic_launcher_background).into(live)
        toolbar.title="Plant Disease Diagnosis"
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)
        live.settings.loadWithOverviewMode=true
        live. settings.useWideViewPort=true
        live.rotation=90F
        image.rotation=90F
        live.loadUrl("http://192.168.43.10")
        live.setInitialScale(1)
        addImg.setOnClickListener {

            showPictureDialog()

        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
        askForPermission(Manifest.permission.CAMERA,CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,READ_EXST);
        capture.setOnClickListener {
            if(capture.text=="live"){

                live.visibility=View.VISIBLE
                image.visibility=View.INVISIBLE
                Log.e("captue","true")
                capture.text="capture"

            }
            else{
                live.stopLoading()

                Glide.with(this@MainActivity).load("http://192.168.43.10/capture")
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return true

                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Log.d("Tag", "OnResourceReady")
                            //do something when picture already loaded
                            mBitmap= scaleImage(drawableToBitmap(resource!!))
                            mBitmap = scaleImage(mBitmap)
                            val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
                            results?.let { showCustomDialog(it.title,it.confidence) }

                            Log.e("result",results?.title+"\n Confidence:"+results?.confidence)
                            image.visibility=View.VISIBLE
                            live.visibility=View.INVISIBLE
                            Log.e("resource",resource.toString())
                            live.loadUrl("http://192.168.43.10")
                            return false
                        }
                    })
                    .placeholder(R.drawable.ic_launcher_background)
                 .signature( ObjectKey (System.currentTimeMillis().toString()))
                 .into(image)


                capture.text="live"

            }

        }





    }

    private fun askForPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) { // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    permission
                )
            ) { //This is called if user has denied the permission before
//In this case I am just asking the permission again
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(permission),
                    requestCode
                )
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(permission),
                    requestCode
                )
            }
        } else {

        }
    }

    fun scaleImage(bitmap: Bitmap?): Bitmap {
        val orignalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / orignalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, orignalWidth, originalHeight, matrix, true)
    }
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        // We ask for the bounds if they have been set as they would be most
// correct, then we check we are  > 0
        val width =
            if (!drawable.bounds.isEmpty) drawable.bounds.width() else drawable.intrinsicWidth
        val height =
            if (!drawable.bounds.isEmpty) drawable.bounds.height() else drawable.intrinsicHeight
        // Now we check we are > 0
        val bitmap = Bitmap.createBitmap(
            if (width <= 0) 1 else width, if (height <= 0) 1 else height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bitmap
    }



    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    fun choosePhotoFromGallary() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takePhotoFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA)
    }

    private fun showCustomDialog(disease :String,confidence:Float) {
        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        var viewGroup = findViewById<ViewGroup>(android.R.id.content)

        //then we will inflate the custom alert dialog xml that we created
        var dialogView = LayoutInflater.from(this).inflate(R.layout.alert_layout, viewGroup, false);
        dialogView.disease.setText(disease)
       dialogView.confidence.setText("%.2f".format(confidence).toString())

        //Now we need an AlertDialog.Builder object
       var builder =  AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);


        //finally creating the alert dialog and displaying it
        var alertDialog = builder.create();
        alertDialog.show();
        dialogView.buttonOk.setOnClickListener {
alertDialog.hide()
        }
    }

    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        /* if (resultCode == this.RESULT_CANCELED)
         {
         return
         }*/
        if (requestCode == GALLERY)
        {

                if (data != null) {
                    val uri = data.data

                    try {
                        mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    Log.e("output",("Success!!!"))
                    mBitmap = scaleImage(mBitmap)
                    val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
                    results?.let { showCustomDialog(it.title,it.confidence) }
                    Log.e("result",results?.title+"\n Confidence:"+results?.confidence)

                }



        }
        else if (requestCode == CAMERA)
        {
            if (data != null) {
                val thumbnail = data!!.extras!!.get("data") as Bitmap
                mBitmap = scaleImage(thumbnail)
                val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
                results?.let { showCustomDialog(it.title,it.confidence) }

                Log.e("result", results?.title + "\n Confidence:" + results?.confidence)
            }
        }
    }






}
