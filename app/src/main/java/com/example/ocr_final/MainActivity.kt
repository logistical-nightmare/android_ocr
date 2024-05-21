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
import androidx.compose.foundation.border
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

var state = 4
var inhouse = ""
var vendor = ""

fun calculateMatchPercentage(str1: String, str2: String): Int {
    val maxLength = maxOf(str1.length, str2.length)
    val matchLength = str1.zip(str2).count { it.first == it.second }
    return (matchLength.toDouble() / maxLength * 100).toInt()
}

fun modifyText(originalText: String): String {
    val lines = originalText.lines()
    val codeRegex = "\\b(?=.*\\d)[:\\w\\d]{10,}\\b".toRegex()
    var highestMatchPercentage = 0.0
    var bestMatch = ""

    val keyword = when (state) {
        1 -> "batch"
        2 -> "vend"
        else -> ""
    }

    for (i in lines.indices) {
        val line = lines[i].trim()

        val matches = codeRegex.findAll(line)
        Log.d("matches", matches.toString())
        for (match in matches) {
            val code = match.value
            val matchPercentage2 = calculateMatchPercentage2(line, keyword)
            Log.d("percentage", "$line $keyword $matchPercentage2")

            if (matchPercentage2 >= highestMatchPercentage) {
                highestMatchPercentage = matchPercentage2
                bestMatch = code
                if(state == 1){
                    vendor = bestMatch
                }
                else if (state == 2) {
                    inhouse = bestMatch
                }
            }
        }
    }

    Log.d("BestMatch", bestMatch)
    return bestMatch
}

fun calculateMatchPercentage2(code: String, keyword: String): Double {
    if (keyword.isEmpty()) return 0.0

    val keywordChars = keyword.toCharArray().toSet()
    val codeChars = code.toCharArray().toSet()

    val intersection = keywordChars.intersect(codeChars).size
    val union = keywordChars.union(codeChars).size

    return if (union == 0) 0.0 else (intersection.toDouble() / union) * 100
}




@Composable
fun ImageTextList(bitmaps: List<Bitmap>, viewModel: MainViewModel) {
    var extractedText by remember { mutableStateOf("") }
    if (state == 1 || state == 2) {
        LaunchedEffect(bitmaps.lastOrNull()) {
            val lastBitmap = bitmaps.lastOrNull()
            if (lastBitmap != null) {
                viewModel.extractTextFromImage(lastBitmap) { text ->
                    extractedText = modifyText(text)
                }
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
        var matchPercentage by remember { mutableStateOf<Int?>(null) }
        var backgroundColor by remember { mutableStateOf(Color.LightGray) }

        if (state == 4) {
            inhouse = ""
            vendor = ""
            matchPercentage = null
            backgroundColor = Color.LightGray
        }

        if (state == 3) {
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
                    .border(if (state == 2) 2.dp else 0.dp, if (state == 2) Color.Blue else Color.Transparent)
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
                        .border(if (state == 4) 2.dp else 0.dp, if (state == 4) Color.Blue else Color.Transparent)
                ) {
                    Column {
                        Text(text = "Vendor")

                        ImageTextList(bitmaps, viewModel)

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
                        .border(if (state == 1) 2.dp else 0.dp, if (state == 1) Color.Blue else Color.Transparent)
                ) {
                    Column{
                        Text(text = "Inhouse")

                        ImageTextList(bitmaps, viewModel)

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
                    if (state == 4) {
                        state = 1
                    }
                    else {
                        state += 1
                    }
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