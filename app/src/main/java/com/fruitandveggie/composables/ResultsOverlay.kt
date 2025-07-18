package com.fruitandveggie.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitandveggie.ui.theme.MintAqua
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult

// This composable is used to display the results of the object detection

// Beside results, it also needs to know the dimensions of the media (video, image, etc.)
// on which the object detection was performed so that it can draw the results properly.

// This information is needed because each result bounds are calculated based on the
// dimensions of the original media that the object detection was performed on. But for
// us to draw the bounds correctly, we need to draw the bounds based on the dimensions of
// the UI space that the media is being displayed in.

// An important note is that this composable should have the exact same UI dimensions of the
// media being displayed, and it should be placed exactly on the top of the displayed media.
// For example, if an image is being displayed in a Box composable, the overlay should be placed
// on top of the image and it should fill the Box composable.
// This is a must because it scales the result bounds according to the provided frame dimensions
// as well as the max available UI width and height


@Composable
fun ResultsOverlay(
    results: ObjectDetectionResult,
    frameWidth: Int,
    frameHeight: Int,
) {
    val detections = results.detections()
    if (detections != null) {
        for (detection in detections) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
            ) {
                // calculating the UI dimensions of the detection bounds
                val resultBounds = detection.boundingBox()
                val boxWidth = (resultBounds.width() / frameWidth) * this.maxWidth.value
                val boxHeight = (resultBounds.height() / frameHeight) * this.maxHeight.value
                val boxLeftOffset = (resultBounds.left / frameWidth) * this.maxWidth.value
                val boxTopOffset = (resultBounds.top / frameHeight) * this.maxHeight.value

                Box(
                    Modifier
                        .fillMaxSize()
                        .offset(
                            boxLeftOffset.dp,
                            boxTopOffset.dp,
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .border(3.dp, MintAqua)
                            .width(boxWidth.dp)
                            .height(boxHeight.dp)
                    )
                    Box(modifier = Modifier.padding(3.dp)) {
                        Text(
                            text = "${
                                detection.categories().first().categoryName()
                            } ${detection.categories().first().score().toString().take(4)}",
                            modifier = Modifier
                                .background(Color.Black)
                                .padding(5.dp, 0.dp),
                            color = MintAqua,
                        )
                    }
                }
            }
        }
    }
}