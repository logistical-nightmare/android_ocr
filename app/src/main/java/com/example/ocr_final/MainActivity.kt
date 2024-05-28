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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ocr_final.ui.theme.Ocr_finalTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.IconButton

fun calculateMatchPercentage(str1: String, str2: String): Int {
    val maxLength = maxOf(str1.length, str2.length)
    val matchLength = str1.zip(str2).count { it.first == it.second }
    return (matchLength.toDouble() / maxLength * 100).toInt()
}

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

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
    fun TextExtraction(viewModel: MainViewModel = viewModel()) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val controller = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.IMAGE_ANALYSIS)
            }
        }
        val bitmaps by viewModel.bitmaps.collectAsState()
        val vendor by viewModel.vendor.collectAsState()
        val inhouse by viewModel.inhouse.collectAsState()
        val tryAgain by viewModel.tryAgain.collectAsState()
        var isFlashlightOn by remember { mutableStateOf(false) }
        val state by viewModel.state.collectAsState()


        var matchPercentage by remember { mutableStateOf<Int?>(null) }
        var backgroundColor by remember { mutableStateOf(Color.LightGray) }



        if (vendor.isNotEmpty() && inhouse.isNotEmpty()) {
            matchPercentage = calculateMatchPercentage(inhouse, vendor)
            backgroundColor = when {
                matchPercentage == 100 -> Color.Green
                matchPercentage in 80 until 100 -> Color.Yellow
                else -> Color.Red
            }
        }
        else {
            matchPercentage = null
            backgroundColor = Color.LightGray
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Blab OCR",
                style = TextStyle(fontSize = 24.sp),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.Gray)
            ) {
                CameraPreview(controller = controller, modifier = Modifier.fillMaxSize(), lifecycleOwner = this@MainActivity)
            }

            Spacer(modifier = Modifier.height(50.dp))


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(backgroundColor)
                    .border(
                        if (state == 3) 2.dp else 0.dp,
                        if (state == 3) Color.Blue else Color.Transparent
                    )
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .background(Color.LightGray)
                        .border(
                            if (state == 1) 2.dp else 0.dp,
                            if (state == 1) Color.Blue else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(1.dp)
                    ){
                        Text(text = "Vendor")
                        Text(text = vendor)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .background(Color.LightGray)
                        .border(
                            if (state == 2) 2.dp else 0.dp,
                            if (state == 2) Color.Blue else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center

                ) {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(1.dp)
                    ){
                        Text(text = "Inhouse")
                        Text(text = inhouse)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (tryAgain) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(80.dp)
                        .padding(16.dp)
                        .background(color = Color.Red, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Try Again",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ){
                Button(
                    onClick = {
                        viewModel.undoState()
                    },
                    modifier = Modifier.width(80.dp)
                ) {
                    Icon(Icons.Rounded.Undo , contentDescription = "Undo")
                }

                Button(
                    onClick = {
                        if (state == 3) {
                            viewModel.resetVendorAndInhouse()
                        } else {
                            takePhoto(controller) { bitmap ->
                                viewModel.onTakePhoto(bitmap)
                                viewModel.extractTextFromImage(bitmap)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = "Camera")
                }

                Button(
                    onClick = {
                        isFlashlightOn = !isFlashlightOn
                        controller.enableTorch(isFlashlightOn)
                    },
                    modifier = Modifier.width(80.dp)
                ) {
                    if (isFlashlightOn) {
                        Icon(Icons.Rounded.FlashlightOn, contentDescription = "FlashLightOn")
                    }
                    else {
                        Icon(Icons.Rounded.FlashlightOff, contentDescription = "FlashLightOff")
                    }
                }
            }
        }
    }

    private fun takePhoto(controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(this),
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
