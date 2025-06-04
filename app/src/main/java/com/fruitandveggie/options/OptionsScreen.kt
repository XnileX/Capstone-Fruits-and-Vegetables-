package com.fruitandveggie.options

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitandveggie.R
import com.fruitandveggie.composables.FruitsAndVegetableDetectionBanner
import com.fruitandveggie.objectdetector.ObjectDetectorHelper
import com.fruitandveggie.ui.theme.Turquoise
import kotlin.math.max
import kotlin.math.min


@Composable
fun OptionsScreen(
    threshold: Float, setThreshold: (Float) -> Unit,
    maxResults: Int, setMaxResults: (Int) -> Unit,
    delegate: Int, setDelegate: (Int) -> Unit,
    mlModel: Int, setMlModel: (Int) -> Unit,
    onBackButtonClick: () -> Unit,
) {
    var delegateDropdownExpanded by remember { mutableStateOf(false) }
    var mlModelDropdownExpanded by remember { mutableStateOf(false) }

    Column {
        FruitsAndVegetableDetectionBanner(
            onBackButtonClick = onBackButtonClick,
        )
        Box(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Options",
                fontSize = 25.sp,
            )
        }
        Divider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Threshold",
                modifier = Modifier
                    .weight(1f),
            )
            IconButton(
                onClick = {
                    val newThreshold = ((threshold * 10).toInt() - 1).toDouble() / 10

                    setThreshold(
                        max(
                            newThreshold.toFloat(),
                            0.0f,
                        )
                    )
                },
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_minus),
                    contentDescription = null,
                    tint = Turquoise
                )
            }
            Box(
                modifier = Modifier.width(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$threshold".substring(IntRange(0, 2)),
                )
            }
            IconButton(
                onClick = {
                    val newThreshold = ((threshold * 10).toInt() + 1).toDouble() / 10
                    setThreshold(
                        min(
                            newThreshold.toFloat(),
                            0.8f,
                        )
                    )
                },
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    tint = Turquoise
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Max Results",
                modifier = Modifier
                    .weight(1f),
            )
            IconButton(
                onClick = {
                    setMaxResults(
                        max(
                            maxResults - 1,
                            1,
                        )
                    )
                },
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_minus),
                    contentDescription = null,
                    tint = Turquoise
                )
            }
            Box(
                modifier = Modifier.width(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$maxResults",
                )
            }
            IconButton(
                onClick = {
                    setMaxResults(
                        min(
                            maxResults + 1,
                            5,
                        )
                    )
                },
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    tint = Turquoise
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Delegate",
                modifier = Modifier
                    .weight(1f),
            )
            Text(
                text = when (delegate) {
                    ObjectDetectorHelper.DELEGATE_CPU -> "CPU";
                    else -> "GPU";
                },
            )
            IconButton(
                onClick = {
                    delegateDropdownExpanded = true
                },
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Turquoise
                )
                DropdownMenu(
                    expanded = delegateDropdownExpanded,
                    onDismissRequest = { delegateDropdownExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "CPU") },
                        onClick = {
                            setDelegate(ObjectDetectorHelper.DELEGATE_CPU)
                            delegateDropdownExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "GPU") },
                        onClick = {
                            setDelegate(ObjectDetectorHelper.DELEGATE_GPU)
                            delegateDropdownExpanded = false
                        },
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "ML Model",
                modifier = Modifier
                    .weight(1f),
            )
            Text(
                text = when (mlModel) {
                    ObjectDetectorHelper.MODEL_EFFICIENTDETV0 -> "EfficientDet Lite0";
                    else -> "EfficientDet Lite2";
                },
            )
            IconButton(
                onClick = {
                    mlModelDropdownExpanded = true
                },
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Turquoise
                )
                DropdownMenu(
                    expanded = mlModelDropdownExpanded,
                    onDismissRequest = { mlModelDropdownExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "EfficientDet Lite0") },
                        onClick = {
                            setMlModel(ObjectDetectorHelper.MODEL_EFFICIENTDETV0)
                            mlModelDropdownExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "EfficientDet Lite2") },
                        onClick = {
                            setMlModel(ObjectDetectorHelper.MODEL_EFFICIENTDETV2)
                            mlModelDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}