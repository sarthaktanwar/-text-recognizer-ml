package com.example.textrecognisation

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.ProgressDialog.show
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.jar.Manifest


class  MainActivity : AppCompatActivity() {
    private lateinit var inputImageButton : MaterialButton
    private lateinit var textRecogniseButton : MaterialButton
    private lateinit var imageIv : ImageView
    private lateinit var recogniseTextEt : EditText

    private companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE =101
    }

    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var progressDialog: ProgressDialog

    private lateinit var textRecognizer: TextRecognizer



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        inputImageButton.findViewById<MaterialButton>(R.id.inputImageButton)
        textRecogniseButton.findViewById<MaterialButton>(R.id.textRecogniseButton)
        imageIv.findViewById<ImageView>(R.id.imageIv)
        recogniseTextEt.findViewById<EditText>(R.id.recogniseTextEt)

        cameraPermission = arrayOf(android.Manifest.permission.CAMERA , android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)



        inputImageButton.setOnClickListener {

            showInputImageDialog()
        }
        textRecogniseButton.setOnClickListener{
            if(imageUri == null){
                showToast("pick Image first")
            }
            else
            {
                recogniseTextFromImage()
            }
        }

    }

    private fun recogniseTextFromImage() {
        progressDialog.setMessage("preparing image")
        progressDialog.show()


        try{
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setMessage("recognizing text ...")
            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener { text->

                    progressDialog.dismiss()
                    val recognisedText = text.text
                    recogniseTextEt.setText(recognisedText)

                }
                .addOnFailureListener{e->
                    progressDialog.dismiss()
                    showToast("failed to recognize due to ${e.message}")
                }

        }
        catch (e: Exception){
            progressDialog.dismiss()
            showToast("failed to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(this,inputImageButton)
        popupMenu.menu.add(Menu.NONE,1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE,2,2,"GALLERY")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem->

            val id = menuItem.itemId

            if(id ==1){
                if(checkCameraPermission())
                {
                    pickImageCamera()
                }
                else
                {
                    requestcameraPermission()
                }
            }
            if(id ==2){
                if(checkStoragePermission())
                {
                    pickImageGallery()
                }
                else
                {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true


        }

    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type ="image/*"

    }
    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if(result.resultCode == Activity.RESULT_OK){
                 val data = result.data
                imageUri =data!!.data
                imageIv.setImageURI(imageUri)


            }
            else{
                     showToast("cancelled...")
            }

        }
    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"SAMPLE TITLE")
        values.put(MediaStore.Images.Media.DESCRIPTION,"SAMPLE TITLE")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLauncher.launch(intent)


    }
    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
             if(result.resultCode == Activity.RESULT_OK){


                   imageIv.setImageURI(imageUri)
             }
            else{
                   showToast("cancelled")
             }

        }
    private fun checkStoragePermission() : Boolean{
         return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkCameraPermission(): Boolean{
           val cameraResult =  ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
           val storageResult = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
         return cameraResult && storageResult
    }
    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestcameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
             CAMERA_REQUEST_CODE->{
                 if (grantResults.isNotEmpty()){
                     val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                     val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                     if(cameraAccepted && storageAccepted){
                         pickImageCamera()
                     }
                     else{
                         showToast("Camera and Storage permission required")
                     }
                 }
             }
            STORAGE_REQUEST_CODE->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                     if(storageAccepted){
                         pickImageGallery()
                     }
                    else{
                        showToast("storage permission required")
                     }
                }

            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
}