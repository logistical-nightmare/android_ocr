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

fun calculateMatchPercentage(str1: String, str2: String): Int {
    val maxLength = maxOf(str1.length, str2.length)
    val matchLength = str1.zip(str2).count { it.first == it.second }
    return (matchLength.toDouble() / maxLength * 100).toInt()
}

fun modifyText(originalText: String): String {
    val lines = originalText.lines()
    val batchRegex = "(?i).*batch.*([\\w\\d]{10,}).*".toRegex()
    val vendBatchRegex = "(?i).*vend.*([\\w\\d]{10,}).*".toRegex()

    for (line in lines) {
        if (decide) {
            val batchMatch = batchRegex.find(line)
            if (batchMatch != null) {
                decide = false
                vendor = batchMatch.groups[1]?.value ?: "No Match"
                Log.d("vendor", vendor)
                return vendor
            }
        } else {
            val vendBatchMatch = vendBatchRegex.find(line)
            if (vendBatchMatch != null) {
                decide = true
                inhouse = vendBatchMatch.groups[1]?.value ?: "No Match"
                Log.d("inhouse", inhouse)
                return inhouse
            }
        }
    }

    Log.d("no match", "no match")
    return "No Match"
}


@Composable
fun ImageTextList(bitmaps: List<Bitmap>, viewModel: MainViewModel) {
    var extractedText by remember { mutableStateOf("") }

    LaunchedEffect(bitmaps.lastOrNull()) {
        val lastBitmap = bitmaps.lastOrNull()
        if (lastBitmap != null) {
            viewModel.extractTextFromImage(lastBitmap) { text ->
                extractedText = modifyText(text)
            }
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

        // Calculate match percentage
        var matchPercentage by remember { mutableStateOf(0) }
        var backgroundColor by remember { mutableStateOf(Color.LightGray) }

        if (inhouse.isNotEmpty() && vendor.isNotEmpty()) {
            matchPercentage = calculateMatchPercentage(inhouse, vendor)
            backgroundColor = when {
                matchPercentage == 100 -> Color.Green
                matchPercentage in 80 until 100 -> Color.Yellow
                else -> Color.Red
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp), // Increased vertical padding for better spacing
            verticalArrangement = Arrangement.SpaceAround, // Adjust vertical arrangement for better distribution
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title for OCR
            Text(
                text = "OCR",
                style = TextStyle(fontSize = 24.sp),
                modifier = Modifier.padding(bottom = 40.dp) // Increased bottom padding for separation
            )

            // Box to display camera feed
            Box(
                modifier = Modifier
                    .size(300.dp) // Adjust size as needed
                    .background(Color.Gray) // Placeholder background color
            ) {
                CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())
            }

            // Spacer to add more space between camera feed and match results
            Spacer(modifier = Modifier.height(32.dp))

            // Match Results Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // Adjust height as needed
                    .background(backgroundColor) // Set background color based on match percentage
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Match Results")
                    if (inhouse.isNotEmpty() && vendor.isNotEmpty()) {
                        Text(text = "Match Percentage: $matchPercentage%")
                    }
                }
            }

            // Spacer to add more space between match results and extracted text boxes
            Spacer(modifier = Modifier.height(24.dp))

            // Row to show extracted text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Box for extracted text 1 (Vendor)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp) // Adjust height as needed
                        .background(Color.LightGray) // Placeholder background color
                ) {
                    Column {
                        Text(text = "Vendor")
                        if (decide) {
                            ImageTextList(bitmaps, viewModel)
                        }
                        Text(text =vendor)
                    }
                }

                // Spacer to add some space between text boxes
                Spacer(modifier = Modifier.width(16.dp))

                // Box for extracted text 2 (Inhouse)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp) // Adjust height as needed
                        .background(Color.LightGray) // Placeholder background color]
                ) {
                    Column{
                        Text(text = "Inhouse")
                        if (!decide) {
                            ImageTextList(bitmaps, viewModel)
                        }
                        Text(text =inhouse)
                    }
                }
            }

            // Spacer to add some space between text boxes and button
            Spacer(modifier = Modifier.height(24.dp))

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