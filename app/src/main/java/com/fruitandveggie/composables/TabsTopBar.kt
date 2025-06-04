package com.fruitandveggie.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.fruitandveggie.R
import androidx.compose.material3.TabRowDefaults

// This composable is used in Home screen to navigate between camera view and gallery view

// It takes an index to display an indication of which tab we're currently in, and a function
// to set that index when a tab is tapped

@Composable
fun TabsTopBar(
    selectedTabIndex: Int,
    setSelectedTabIndex: (Int) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        indicator = { tabPositions -> },
        divider = {}
    ) {
        Tab(
            unselectedContentColor = Color.Gray,
            selected = selectedTabIndex == 0,
            onClick = {
                setSelectedTabIndex(0)
            },
            text = { Text("Camera", color = Color.White) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_photo_camera_24),
                    contentDescription = "Camera icon"
                )
            },
        )
        Tab(
            unselectedContentColor = Color.Gray,
            selected = selectedTabIndex == 1,
            onClick = {
                setSelectedTabIndex(1)
            },
            text = { Text("Gallery", color = Color.White) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_photo_library_24),
                    contentDescription = "Gallery icon"
                )
            },
        )
    }
}