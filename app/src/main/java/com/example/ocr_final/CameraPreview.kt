package com.example.ocr_final

import androidx.camera.view.PreviewView
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A composable function that displays a camera preview using the given controller.
 *
 * @param controller The LifecycleCameraController to use for the camera preview.
 * @param modifier Modifier to be applied to the PreviewView.
 * @param lifecycleOwner The LifecycleOwner that controls the lifecycle of the camera.
 */
@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier
    )
}
