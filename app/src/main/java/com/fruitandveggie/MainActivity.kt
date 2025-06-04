package com.fruitandveggie


import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fruitandveggie.composables.ResultsOverlay
import com.fruitandveggie.home.camera.CameraView
import com.fruitandveggie.objectdetector.ObjectDetectorHelper
import com.fruitandveggie.ui.theme.Charcoal
import com.fruitandveggie.ui.theme.FruitAndVeggieTheme
import com.fruitandveggie.ui.theme.MintAqua
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = MintAqua)
        .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fruits and Vegetables Detection",
            color = Color(0xFF2e2e2e),
            fontSize = 24.sp
        )
        var searchText by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Input fruits or vegetables name", color = Color(0xFF7a7a7a)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Charcoal,
                unfocusedBorderColor = Charcoal
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    threshold: Float,
    maxResults: Int,
    delegate: Int,
    mlModel: Int,
) {
    var inferenceTime by rememberSaveable { mutableStateOf(0) }
    var selectedMediaUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedMediaType by rememberSaveable { mutableStateOf<String?>(null) }

    // Get the context in the composable scope
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            selectedMediaUri = uri
            // Use the context variable instead of LocalContext.current
            selectedMediaType = uri?.let { context.contentResolver.getType(it) }
        }
    )

    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    fun MediaDetectionView(
        mediaUri: Uri,
        mediaType: String,
        targetWidth: Dp,
        targetHeight: Dp,
        threshold: Float,
        maxResults: Int,
        delegate: Int,
        mlModel: Int,
        setInferenceTime: (Int) -> Unit,
    ) {
        // States for image detection
        var loadedImage by remember { mutableStateOf<Bitmap?>(null) }
        var imageResults by remember { mutableStateOf<ObjectDetectionResult?>(null) }

        // States for video detection
        var videoResults by remember { mutableStateOf<ObjectDetectionResult?>(null) }
        var videoWidth by remember { mutableStateOf(1) }
        var videoHeight by remember { mutableStateOf(1) }

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current // Keep this if needed for detection lifecycle

        // ExoPlayer instance, managed by remember and DisposableEffect
        val exoPlayer = remember(context) {
            ExoPlayer.Builder(context).build().also { player ->
                player.volume = 0f // Mute video by default
            }
        }

        LaunchedEffect(mediaUri, mediaType) {
            when {
                mediaType.startsWith("image") -> {
                    // Image loading and detection logic (adapted from StaticImageDetectionView)
                    try {
                        val source = ImageDecoder.createSource(context.contentResolver, mediaUri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        loadedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                        loadedImage?.let { img ->
                            Executors.newSingleThreadScheduledExecutor().execute {
                                val objectDetectorHelper = ObjectDetectorHelper(
                                    context = context,
                                    threshold = threshold,
                                    currentDelegate = delegate,
                                    currentModel = mlModel,
                                    maxResults = maxResults,
                                    runningMode = RunningMode.IMAGE,
                                )
                                val resultBundle = objectDetectorHelper.detectImage(img)
                                if (resultBundle != null) {
                                    setInferenceTime(resultBundle.inferenceTime.toInt())
                                    imageResults = resultBundle.results.first()
                                }
                                objectDetectorHelper.clearObjectDetector()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                mediaType.startsWith("video") -> {
                    // Prepare ExoPlayer with MediaItem and start detection
                    val mediaItem = MediaItem.Builder().setUri(mediaUri).build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()

                    exoPlayer.play() // Start playing immediately after preparation

                    // Video detection logic (can be adapted to observe player time)
                    val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
                    backgroundExecutor.execute {
                        val objectDetectorHelper = ObjectDetectorHelper(
                            context = context,
                            threshold = threshold,
                            currentDelegate = delegate,
                            currentModel = mlModel,
                            maxResults = maxResults,
                            runningMode = RunningMode.VIDEO,
                        )

                        val videoInterval = 300L // Detection interval in ms
                        val detectionResults = objectDetectorHelper.detectVideoFile(mediaUri, videoInterval)

                        if (detectionResults != null) {
                            val videoStartTimeMs = SystemClock.uptimeMillis()
                            backgroundExecutor.scheduleAtFixedRate(
                                {
                                    val videoElapsedTimeMs = SystemClock.uptimeMillis() - videoStartTimeMs
                                    val resultIndex = videoElapsedTimeMs.div(videoInterval).toInt()

                                    if (resultIndex >= detectionResults.results.size) {
                                        backgroundExecutor.shutdown()
                                    } else {
                                        videoResults = detectionResults.results[resultIndex]
                                        setInferenceTime(detectionResults.inferenceTime.toInt())
                                    }
                                },
                                0,
                                videoInterval,
                                TimeUnit.MILLISECONDS,
                            )
                        }
                        objectDetectorHelper.clearObjectDetector()
                    }
                }
            }
        }

        // Release ExoPlayer and detection executor on dispose
        DisposableEffect(Unit) { // Use Unit as key to dispose when composable leaves composition
            onDispose {
                exoPlayer.release()
                // The backgroundExecutor for video detection is managed locally within the LaunchedEffect
            }
        }

        // Display image or video with overlay
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                mediaType.startsWith("image") -> {
                    loadedImage?.let { img ->
                        Box(modifier = Modifier
                            .width(targetWidth)
                            .height(targetHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray) // Placeholder background
                        ) {
                            Image(
                                bitmap = img.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            imageResults?.let { res ->
                                ResultsOverlay(
                                    results = res,
                                    frameWidth = img.width,
                                    frameHeight = img.height,
                                )
                            }
                        }
                    }
                }
                mediaType.startsWith("video") -> {
                    // Display StyledPlayerView and overlay
                    val player = exoPlayer // Use the rememberd exoPlayer instance
                    val currentVideoWidth = videoWidth.takeIf { it > 1 } ?: player.videoSize.width.takeIf { it > 0 } ?: 1
                    val currentVideoHeight = videoHeight.takeIf { it > 1 } ?: player.videoSize.height.takeIf { it > 0 } ?: 1

                    // Calculate scaled size (simplified - you might need getFittedBoxSize if needed)
                    val displayWidth = this.maxWidth
                    val displayHeight = displayWidth * (currentVideoHeight.toFloat() / currentVideoWidth.toFloat())

                    Box(modifier = Modifier
                        .width(displayWidth)
                        .height(displayHeight)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                StyledPlayerView(ctx).apply {
                                    hideController()
                                    useController = false
                                    this.player = player // Set player in factory
                                }
                            },
                            update = { playerView ->
                                // Update video dimensions as they become available
                                playerView.player?.videoSize?.let { size ->
                                    if (size.width > 0 && size.height > 0) {
                                        videoWidth = size.width
                                        videoHeight = size.height
                                    }
                                }
                            }
                        )
                        videoResults?.let { res ->
                            ResultsOverlay(
                                results = res,
                                frameWidth = currentVideoWidth,
                                frameHeight = currentVideoHeight,
                            )
                        }
                    }
                }
                else -> {
                    // Handle other media types or error display if necessary
                }
            }
        }
    }

    // Private composable to handle the main content display based on media type
    @Composable
    fun CameraContent(
        modifier: Modifier,
        selectedMediaUri: Uri?,
        selectedMediaType: String?,
        threshold: Float,
        maxResults: Int,
        delegate: Int,
        mlModel: Int,
        inferenceTime: Int,
        setInferenceTime: (Int) -> Unit
    ) {
        BoxWithConstraints(modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val cameraDisplayWidth = this.maxWidth
            val cameraDisplayHeight = this.maxHeight * 0.75f // Assuming this is the target height from previous steps

            if (selectedMediaType?.startsWith("image") == true) {
                MediaDetectionView(
                    mediaUri = selectedMediaUri!!,
                    mediaType = selectedMediaType!!,
                    targetWidth = cameraDisplayWidth,
                    targetHeight = cameraDisplayHeight,
                    threshold = threshold,
                    maxResults = maxResults,
                    delegate = delegate,
                    mlModel = mlModel,
                    setInferenceTime = setInferenceTime
                )
            } else if (selectedMediaType?.startsWith("video") == true) {
                 MediaDetectionView(
                    mediaUri = selectedMediaUri!!,
                    mediaType = selectedMediaType!!,
                    targetWidth = cameraDisplayWidth,
                    targetHeight = cameraDisplayHeight,
                    threshold = threshold,
                    maxResults = maxResults,
                    delegate = delegate,
                    mlModel = mlModel,
                    setInferenceTime = setInferenceTime
                )
            } else {
                CameraView(
                    threshold = threshold,
                    maxResults = maxResults,
                    delegate = delegate,
                    mlModel = mlModel,
                    setInferenceTime = setInferenceTime,
                    inferenceTime = inferenceTime
                )
             }
         }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = MintAqua)) {
        // Custom Top Bar
        TopAppBar(
            title = { Text("") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MintAqua
            )
        )

        // Main Content Area (CameraView or Static Image)
        CameraContent(
            modifier = Modifier.weight(2f),
            selectedMediaUri = selectedMediaUri,
            selectedMediaType = selectedMediaType,
            threshold = threshold,
            maxResults = maxResults,
            delegate = delegate,
            mlModel = mlModel,
            inferenceTime = inferenceTime,
            setInferenceTime = { inferenceTime = it }
        )

        // Buttons (Capture Photo and Camera Roll)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { galleryLauncher.launch(arrayOf("image/*", "video/*")) }) {
                Text("Camera Roll")
            }
        }

        // Inference Time Display
        // ... existing code ...
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MintAqua), contentAlignment = Alignment.Center) {
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
            val navItems: List<Triple<String, @Composable () -> Unit, @Composable () -> Unit>> = listOf(
                Triple("Home", { Icon(Icons.Filled.Home, contentDescription = "Home") }, { HomeScreenSimple() }),
                Triple("Camera", { Icon(painterResource(id = R.drawable.ic_baseline_photo_camera_24), contentDescription = "Camera") }, {
                    CameraScreen(
                        threshold = threshold,
                        maxResults = maxResults,
                        delegate = delegate,
                        mlModel = mlModel
                    )
                }),
                Triple("Profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") }, { ProfileScreen() })
            )
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = windowInsets,
                bottomBar = {
                    NavigationBar {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { item.second() },
                                label = { Text(item.first) },
                                selected = selectedTab == index,
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    navItems[selectedTab].third()
                }
            }
        }
    }
}