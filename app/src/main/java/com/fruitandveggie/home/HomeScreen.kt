package com.fruitandveggie.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fruitandveggie.R
import com.fruitandveggie.composables.FruitsAndVegetableDetectionBanner
import com.fruitandveggie.composables.TabsTopBar
import com.fruitandveggie.home.camera.CameraView
import com.fruitandveggie.home.galery.GalleryView

// The Home screen contains the camera view and the gallery view
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun HomeScreen(
    onOptionsButtonClick: () -> Unit,
    threshold: Float,
    maxResults: Int,
    delegate: Int,
    mlModel: Int,
) {
    // We declare a state to control which view we're displaying: camera or gallery
    var selectedTabIndex by rememberSaveable {
        mutableStateOf(0)
    }

    // This state stores the inference time of the latest object detection process
    // to be displayed at the bottom of the screen
    var inferenceTime by rememberSaveable {
        mutableStateOf(0)
    }

    Box {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Column {
            // MediaPipe banner showing an options button to navigate to Options screen with
            FruitsAndVegetableDetectionBanner(
                onOptionsButtonClick = onOptionsButtonClick,
            )
            // The tabs at the top to switch between camera and gallery views
            TabsTopBar(
                selectedTabIndex = selectedTabIndex,
                setSelectedTabIndex = {
                    selectedTabIndex = it
                    inferenceTime = 0
                }
            )
            // Here we display the camera view or the gallery view based on the selected tab, both
            // of which need to be provided with the object detector options as well as a function
            // to update the inference value when running an object detection process
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (selectedTabIndex == 0) {
                    CameraView(
                        threshold = threshold,
                        maxResults = maxResults,
                        delegate = delegate,
                        mlModel = mlModel,
                        setInferenceTime = { inferenceTime = it },
                        inferenceTime = inferenceTime,
                    )
                } else {
                    GalleryView(
                        threshold = threshold,
                        maxResults = maxResults,
                        delegate = delegate,
                        mlModel = mlModel,
                        setInferenceTime = { inferenceTime = it },
                    )
                }
            }
        }
    }
}