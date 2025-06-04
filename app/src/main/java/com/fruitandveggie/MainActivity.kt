package com.fruitandveggie


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fruitandveggie.home.HomeScreen
import com.fruitandveggie.home.camera.CameraView
import com.fruitandveggie.objectdetector.ObjectDetectorHelper
import com.fruitandveggie.options.OptionsScreen
import com.fruitandveggie.ui.theme.FruitAndVeggieTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObjectDetectionExampleApp()
//            FruitAndVeggieTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    ObjectDetectionExampleApp(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
        }
    }
}

@Composable
fun HomeScreenSimple() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home Page")
    }
}

@Composable
fun CameraScreen(
    threshold: Float,
    maxResults: Int,
    delegate: Int,
    mlModel: Int,
) {
    var inferenceTime by rememberSaveable { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        CameraView(
            threshold = threshold,
            maxResults = maxResults,
            delegate = delegate,
            mlModel = mlModel,
            setInferenceTime = { inferenceTime = it },
            inferenceTime = inferenceTime
        )
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Profile Page")
    }
}

@Composable
fun ObjectDetectionExampleApp() {
    var threshold by rememberSaveable { mutableStateOf(0.4f) }
    var maxResults by rememberSaveable { mutableStateOf(5) }
    var delegate by rememberSaveable { mutableStateOf(ObjectDetectorHelper.DELEGATE_CPU) }
    var mlModel by rememberSaveable { mutableStateOf(ObjectDetectorHelper.MODEL_EFFICIENTDETV0) }

    FruitAndVeggieTheme(darkTheme = false) {
        val windowInsets = WindowInsets(0, 67, 0, 0)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var selectedTab by rememberSaveable { mutableStateOf(0) }
            val navItems = listOf(
                Triple("Home", Icons.Filled.Home, { HomeScreenSimple() }),
                Triple("Camera", Icons.Filled.CameraAlt, {
                    CameraScreen(
                        threshold = threshold,
                        maxResults = maxResults,
                        delegate = delegate,
                        mlModel = mlModel
                    )
                }),
                Triple("Profile", Icons.Filled.Person, { ProfileScreen() })
            )
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = windowInsets,
                bottomBar = {
                    NavigationBar {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { Icon(item.second, contentDescription = item.first) },
                                label = { Text(item.first) },
                                selected = selectedTab == index,
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    navItems[selectedTab].third.invoke()
                }
            }
        }
    }
}