package com.example.ocr_final

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ocr_final.ui.theme.Ocr_finalTheme
import kotlinx.coroutines.launch

var decide = true
var inhouse = ""
var vendor = ""

fun modifyText(originalText: String): String {
    val lines = originalText.lines()
    val batchRegex = "Batch: ([\\w\\-_]{6,})".toRegex()
    val vendBatchRegex = "Vend\\.Batch: ([\\w\\-_]{6,})".toRegex()

    for (line in lines) {
        if (decide) {
            val matchResult = batchRegex.find(line)
            if (matchResult != null) {
                decide = false
                return matchResult.groups[1]?.value ?: ""
            }
        } else {
            val matchResult = vendBatchRegex.find(line)
            if (matchResult != null) {
                decide = true
                return matchResult.groups[1]?.value ?: ""
            }
        }
    }

    // If no match is found according to the rules, return the uppercase version of the original text
    decide = !decide
    return originalText.uppercase()
}


@Composable
fun ImageTextList(bitmaps: List<Bitmap>, viewModel: MainViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(bitmaps) { bitmap ->
            var extractedText by remember { mutableStateOf("") }
            LaunchedEffect(bitmap) {
                viewModel.extractTextFromImage(bitmap) { text ->
                    extractedText = modifyText(text)
                }
            }
            Text(
                text = extractedText,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, CAMERAX_PERMISSIONS, 0)
        }
        setContent {
            Ocr_finalTheme {
                TextExtraction()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TextExtraction() {
        val scope = rememberCoroutineScope()
        val controller = remember {
            LifecycleCameraController(applicationContext).apply {
                setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            }
        }
        val viewModel = viewModel<MainViewModel>()
        val bitmaps by viewModel.bitmaps.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding for better spacing
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title for OCR
            Text(
                text = "OCR Results",
                style = TextStyle(fontSize = 24.sp),
                modifier = Modifier
                    .padding(bottom = 100.dp)
            )

            // Box to display camera feed
            Box(
                modifier = Modifier
                    .size(300.dp) // Adjust size as needed
                    .background(Color.Gray) // Placeholder background color
            ) {
                CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())
            }

            // Spacer to add more space between camera feed and text boxes
            Spacer(modifier = Modifier.height(100.dp))

            // Row to show extracted text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Box for extracted text 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp) // Adjust height as needed
                        .background(Color.LightGray) // Placeholder background color
                ) {
                    Text(text = "Vendor")
                    if (decide) {
                        ImageTextList(bitmaps, viewModel)
                    }
                }

                // Spacer to add some space between text boxes
                Spacer(modifier = Modifier.width(16.dp))

                // Box for extracted text 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp) // Adjust height as needed
                        .background(Color.LightGray) // Placeholder background color
                ) {
                    Text(text = "Inhouse")
                    if (!decide) {
                        ImageTextList(bitmaps, viewModel)
                    }
                }
            }

            // Spacer to add some space between text boxes and button
            Spacer(modifier = Modifier.height(16.dp))

            // Button to capture image from camera
            Button(
                onClick = {
                    takePhoto(controller = controller, onPhotoTaken = { bitmap ->
                        viewModel.onTakePhoto(bitmap)
                    })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Capture Image")
            }
        }
    }


    private fun takePhoto(controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }

}