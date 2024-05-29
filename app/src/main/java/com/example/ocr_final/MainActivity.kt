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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

fun calculateMatchPercentage(str1: String, str2: String): Int {
    val maxLength = maxOf(str1.length, str2.length)
    val matchLength = str1.zip(str2).count { it.first == it.second }
    return (matchLength.toDouble() / maxLength * 100).toInt()
}

@Composable
fun ClickableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFADD8E6),
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .padding(4.dp)
            .background(color = backgroundColor, shape = shape)
            .padding(8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.IosShare, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
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
        val noScanned by viewModel.noScanned.collectAsState()


        var matchPercentage by remember { mutableStateOf<Int>(0) }
        var backgroundColor by remember { mutableStateOf(Color.LightGray) }



        if (state == 3) {
            matchPercentage = calculateMatchPercentage(inhouse, vendor)
            backgroundColor = when {
                matchPercentage == 100 -> Color.Green
                matchPercentage in 80 until 100 -> Color.Yellow
                else -> Color.Red
            }
        }
        else {
            matchPercentage = 0
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
                text = "BLab OCR",
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
                        if (state == 3) 4.dp else 0.dp,
                        if (state == 3) Color.Blue else Color.Transparent
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Match Percentage: $matchPercentage%")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            if (state == 1) 4.dp else 0.dp,
                            if (state == 1 && tryAgain) Color.Red
                            else if (state == 1) Color.Blue
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(1.dp)
                    ){
                        Text(text = "Vendor")
                        if(tryAgain && state == 1) Text(text = "Try Again")
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
                            if (state == 2) 4.dp else 0.dp,
                            if (state == 2 && tryAgain) Color.Red
                            else if (state == 2) Color.Blue
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center

                ) {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(1.dp)
                    ){
                        Text(text = "Inhouse")
                        if(tryAgain && state == 2) Text(text = "Try Again")
                        Text(text = inhouse)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

                Row() {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(60.dp)
                            .padding(4.dp)
                            .background(color = Color(0xFFADD8E6), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Scanned: $noScanned",
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    ClickableBox(
                        onClick = {
                            viewModel.printMatchDataList()
                            viewModel.shareMatchDataList(context)
                        },
                        contentDescription = "Share"
                    )
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
                            if (matchPercentage >= 80) {
                                viewModel.addNoScanned()
                                viewModel.addToMatchDataList(vendor, inhouse)
                            }
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
