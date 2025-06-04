package com.fruitandveggie.home.camera

import android.Manifest
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fruitandveggie.composables.ResultsOverlay
import com.fruitandveggie.objectdetector.ObjectDetectorHelper
import com.fruitandveggie.objectdetector.ObjectDetectorListener
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import java.util.concurrent.Executors

// Here we have the camera view which is displayed in Home screen

// It's used to run object detection on live camera feed

// It takes as input the object detection options, and a function to update the inference time state

// You will notice we have a decorator that indicated we're using an experimental API for
// permissions, we're using it cause it's easy to check for permissions with it, and we need camera
// permission in this composable.
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView(
    threshold: Float,
    maxResults: Int,
    delegate: Int,
    mlModel: Int,
    setInferenceTime: (newInferenceTime: Int) -> Unit,
    inferenceTime: Int = 0,
) {
    // We first have to deal with the camera permission, so we declare a state for it
    val storagePermissionState: PermissionState =
        rememberPermissionState(Manifest.permission.CAMERA)

    // When using this composable, we wanna check the camera permission state, and ask for the
    // permission to use the phone camera in case we don't already have it
    LaunchedEffect(key1 = Unit) {
        if (!storagePermissionState.hasPermission) {
            storagePermissionState.launchPermissionRequest()
        }
    }


    // In case we don't have the permission to use a camera, we'll just display a text to let the
    // user know that that's the case, and we won't show anything else
    if (!storagePermissionState.hasPermission) {
        Text(text = "No Storage Permission!")
        return
    }

    // At this point we have our permission to use the camera. Now we define some states

    // This state holds the object detection results
    var results by remember {
        mutableStateOf<ObjectDetectionResult?>(null)
    }

    // These states hold the dimensions of the camera frames. We don't know their values yet so
    // we just set them initially to 1x1
    var frameHeight by remember {
        mutableStateOf(4)
    }

    var frameWidth by remember {
        mutableStateOf(3)
    }

    // This state is used to prevent further state updates when this camera view is being disposed
    // We check for it before updating states, and we set it to false when we dispose of the view
    var active by remember {
        mutableStateOf(true)
    }

    // We need the following objects setup the camera preview later
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }


    // Here we setup what will happen when the camera view is being disposed. We just need to set
    // "active" to false to stop any further state updates, and to close any currently open cameras
    DisposableEffect(Unit) {
        onDispose {
            active = false;
            cameraProviderFuture.get().unbindAll()
        }
    }

    // Next we describe the UI of this camera view.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val previewWidth = 340.dp
        val previewHeight = this.maxHeight * 0.75f // 75% of the available height
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(RoundedCornerShape(32.dp))
                    .background(color = Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        // We start by instantiating the camera preview view to be displayed
                        val previewView = PreviewView(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // We set a surface for the camera input feed to be displayed in, which is
                            // in the camera preview view we just instantiated
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // We specify what phone camera to use. In our case it's the back camera
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()

                            // We instantiate an image analyser to apply some transformations on the
                            // input frame before feeding it to the object detector
                            val imageAnalyzer =
                                ImageAnalysis.Builder()
                                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()

                            // Now we're ready to apply object detection. For a better performance, we
                            // execute the object detection process in a new thread.
                            val backgroundExecutor = Executors.newSingleThreadExecutor()

                            backgroundExecutor.execute {

                                // To apply object detection, we use our ObjectDetectorHelper class,
                                // which abstracts away the specifics of using MediaPipe  for object
                                // detection from the UI elements of the app
                                val objectDetectorHelper =
                                    ObjectDetectorHelper(
                                        context = ctx,
                                        threshold = threshold,
                                        currentDelegate = delegate,
                                        currentModel = mlModel,
                                        maxResults = maxResults,
                                        // Since we're detecting objects in a live camera feed, we need
                                        // to have a way to listen for the results
                                        objectDetectorListener = ObjectDetectorListener(
                                            onErrorCallback = { _, _ -> },
                                            onResultsCallback = {
                                                // On receiving results, we now have the exact camera
                                                // frame dimensions, so we set them here
                                                frameHeight = it.inputImageHeight
                                                frameWidth = it.inputImageWidth

                                                // Then we check if the camera view is still active,
                                                // if so, we set the state of the results and
                                                // inference time.
                                                if (active) {
                                                    results = it.results.first()
                                                    setInferenceTime(it.inferenceTime.toInt())
                                                }
                                            }
                                        ),
                                        runningMode = RunningMode.LIVE_STREAM
                                    )

                                // Now that we have our ObjectDetectorHelper instance, we set is as an
                                // analyzer and start detecting objects from the camera live stream
                                imageAnalyzer.setAnalyzer(
                                    backgroundExecutor,
                                    objectDetectorHelper::detectLivestreamFrame
                                )
                            }

                            // We close any currently open camera just in case, then open up
                            // our own to be display the live camera feed
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                imageAnalyzer,
                                preview
                            )
                        }, executor)
                        // We return our preview view from the AndroidView factory to display it
                        previewView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp)),
                )
                // Finally, we check for current results, if there's any, we display the results overlay
                results?.let {
                    ResultsOverlay(
                        results = it,
                        frameWidth = frameWidth,
                        frameHeight = frameHeight
                    )
                }
            }
            // Add inference time text just below the camera preview
            Text(
                text = "Inference Time: $inferenceTime ms",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

}