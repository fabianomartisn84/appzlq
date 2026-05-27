package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.sensor.SensorTracker
import com.example.ui.theme.MinimalSkyBlue
import com.example.ui.theme.ContrastNavy
import com.example.ui.theme.SlateBlueGray
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.TextLightGray
import com.example.ui.theme.NeutralGrayMuted
import com.example.ui.theme.NeutralCardLight
import com.example.ui.theme.EdgeBorderLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.DangerRed
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val viewModel: ScaffoldHeightViewModel by viewModels()
    private lateinit var sensorTracker: SensorTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorTracker = SensorTracker(this)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    ScaffoldHeightMeterScreen(
                        viewModel = viewModel,
                        sensorTracker = sensorTracker,
                        modifier = Modifier
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sensorTracker.start()
    }

    override fun onStop() {
        super.onStop()
        sensorTracker.stop()
    }
}

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldHeightMeterScreen(
    viewModel: ScaffoldHeightViewModel,
    sensorTracker: SensorTracker,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Aesthetic fallback if permission is denied
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MinimalSkyBlue,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Acesso à Câmera Necessário",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextLightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "O Medidor de Altura de Andaime precisa da câmera traseira para exibir o andaime na tela e estimar as dimensões corretas baseadas em trigonometria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLightGray.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinimalSkyBlue,
                        contentColor = ContrastNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Conceder Permissão", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Main Screen Interface
        val pitch by sensorTracker.pitch.collectAsState()
        val roll by sensorTracker.roll.collectAsState()

        var screenHeight by remember { mutableStateOf<Float?>(null) }
        var isCalibratorOpen by remember { mutableStateOf(false) }

        // Touch dragging logic support states
        var draggingPoint by remember { mutableStateOf<String?>(null) }
        var tapCandidate by remember { mutableStateOf<Offset?>(null) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .onGloballyPositioned { coordinates ->
                    screenHeight = coordinates.size.height.toFloat()
                }
        ) {
            // 1. Fullscreen camera view behind drawing boards
            Box(modifier = Modifier.fillMaxSize()) {
                CameraXPreview()
            }

            // Dark subtle transparent overlay to enhance readability of active guides
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // Let's draw the lines and visual markers
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas_overlay")
                    .pointerInput(viewModel.measuringMode) {
                        if (viewModel.measuringMode == "TOUCH") {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    val position = change.position
                                    val isPressed = change.pressed
                                    val isPressedChanged = change.changedToDown()
                                    val isReleasedChanged = change.changedToUp()

                                    // On touch down
                                    if (isPressedChanged) {
                                        val distBase = viewModel.basePoint?.let { (it - position).getDistance() } ?: Float.MAX_VALUE
                                        val distTop = viewModel.topPoint?.let { (it - position).getDistance() } ?: Float.MAX_VALUE
                                        val threshold = 110f // ~36dp threshold

                                        if (distBase < threshold && distBase < distTop) {
                                            draggingPoint = "base"
                                        } else if (distTop < threshold) {
                                            draggingPoint = "top"
                                        } else {
                                            draggingPoint = null
                                            tapCandidate = position
                                        }
                                    }

                                    // On drag
                                    if (isPressed && draggingPoint != null) {
                                        change.consume()
                                        if (draggingPoint == "base") {
                                            viewModel.setBasePosition(position)
                                        } else if (draggingPoint == "top") {
                                            viewModel.setTopPosition(position)
                                        }
                                        tapCandidate = null
                                    }

                                    // On release
                                    if (isReleasedChanged) {
                                        if (draggingPoint == null && tapCandidate != null) {
                                            val offset = tapCandidate!!
                                            if (viewModel.basePoint == null) {
                                                viewModel.setBasePosition(offset)
                                            } else if (viewModel.topPoint == null) {
                                                viewModel.setTopPosition(offset)
                                            } else {
                                                // Relocate closest point
                                                val distBase = (viewModel.basePoint!! - offset).getDistance()
                                                val distTop = (viewModel.topPoint!! - offset).getDistance()
                                                if (distBase < distTop) {
                                                    viewModel.setBasePosition(offset)
                                                } else {
                                                    viewModel.setTopPosition(offset)
                                                }
                                            }
                                        }
                                        draggingPoint = null
                                        tapCandidate = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Compose Canvas representing guidelines and crosshairs
                val isLevel = abs(roll) < 3.0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // A. Draw level horizontal lines helper if not perfectly level
                    if (!isLevel) {
                        drawLine(
                            color = DangerRed.copy(alpha = 0.5f),
                            start = Offset(0f, canvasHeight / 2),
                            end = Offset(canvasWidth, canvasHeight / 2),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    } else {
                        // Level horizontal helper in beautiful green
                        drawLine(
                            color = SafeGreen.copy(alpha = 0.6f),
                            start = Offset(0f, canvasHeight / 2),
                            end = Offset(canvasWidth, canvasHeight / 2),
                            strokeWidth = 2.5f
                        )
                    }

                    // B. Draw crosshairs if in AIM mode
                    if (viewModel.measuringMode == "AIM") {
                        val strokeColor = if (isLevel) SafeGreen else MinimalSkyBlue
                        // Sights circle
                        drawCircle(
                            color = strokeColor,
                            radius = 60f,
                            center = Offset(canvasWidth / 2, canvasHeight / 2),
                            style = Stroke(width = 3.5f)
                        )
                        // Outer ticks
                        drawLine(
                            color = strokeColor,
                            start = Offset(canvasWidth / 2 - 100f, canvasHeight / 2),
                            end = Offset(canvasWidth / 2 - 30f, canvasHeight / 2),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(canvasWidth / 2 + 30f, canvasHeight / 2),
                            end = Offset(canvasWidth / 2 + 100f, canvasHeight / 2),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(canvasWidth / 2, canvasHeight / 2 - 100f),
                            end = Offset(canvasWidth / 2, canvasHeight / 2 - 30f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(canvasWidth / 2, canvasHeight / 2 + 30f),
                            end = Offset(canvasWidth / 2, canvasHeight / 2 + 100f),
                            strokeWidth = 3f
                        )
                        // Tiny center alignment dot
                        drawCircle(
                            color = strokeColor,
                            radius = 4f,
                            center = Offset(canvasWidth / 2, canvasHeight / 2)
                        )
                    }

                    // C. Draw connecting dashed vertical line between BASE and TOPO
                    if (viewModel.measuringMode == "TOUCH") {
                        val base = viewModel.basePoint
                        val top = viewModel.topPoint
                        if (base != null && top != null) {
                            // Connecting line
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = base,
                                end = top,
                                strokeWidth = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                    }
                }

                // D. Composable-overlaid points (so they supports touch interactions and text tags on drawing canvas)
                if (viewModel.measuringMode == "TOUCH") {
                    viewModel.basePoint?.let { offset ->
                        CircleTargetMarker(
                            label = "BASE",
                            offset = offset,
                            color = DangerRed,
                            isDragging = draggingPoint == "base"
                        )
                    }
                    viewModel.topPoint?.let { offset ->
                        CircleTargetMarker(
                            label = "TOPO",
                            offset = offset,
                            color = SafeGreen,
                            isDragging = draggingPoint == "top"
                        )
                    }
                }
            }

            // 2. HUD - Live Status Bars (Safe areas padding)
            val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding + 12.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                // A. Sockets for segmented controller (TOUCH vs AIM mode)
                SegmentedModeSelector(
                    activeMode = viewModel.measuringMode,
                    onModeChanged = { mode ->
                        viewModel.measuringMode = mode
                        viewModel.reset()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // B. Real-time pitch/angle meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tilt state Card
                    StatusBadge(
                        icon = Icons.Default.CompassCalibration,
                        label = "Tilt",
                        value = String.format(Locale.US, "%.1f°", pitch),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )

                    // Balance state Card
                    val balanceColor = if (abs(roll) < 3.0f) SafeGreen else DangerRed
                    StatusBadge(
                        icon = Icons.Default.MyLocation,
                        label = if (abs(roll) < 3.0f) "Nivelado" else "Inclinado",
                        value = String.format(Locale.US, "%.1f°", roll),
                        color = balanceColor.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // C. Active Instructions Banner
                InstructionBanner(
                    mode = viewModel.measuringMode,
                    hasBase = if (viewModel.measuringMode == "TOUCH") viewModel.basePoint != null else viewModel.baseAngle != null,
                    hasTop = if (viewModel.measuringMode == "TOUCH") viewModel.topPoint != null else viewModel.topAngle != null,
                    rollAngle = roll
                )
            }

            // 3. Absolute measurements captured summary (when in AIM Mode)
            if (viewModel.measuringMode == "AIM" && (viewModel.baseAngle != null || viewModel.topAngle != null)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, top = 160.dp)
                        .background(BackgroundDark.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .width(140.dp)
                ) {
                    Text("Mira Salva:", style = MaterialTheme.typography.labelSmall, color = TextLightGray.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Base: " + (viewModel.baseAngle?.let { String.format(Locale.US, "%.1f°", Math.toDegrees(it)) } ?: "Não Marcado"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.baseAngle != null) SafeGreen else TextLightGray.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Topo: " + (viewModel.topAngle?.let { String.format(Locale.US, "%.1f°", Math.toDegrees(it)) } ?: "Não Marcado"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.topAngle != null) SafeGreen else TextLightGray.copy(alpha = 0.4f)
                    )
                }
            }

            // 4. ACTION BAR (Bottom Control center)
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding + 16.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Optional Live coordinates labels for touch points
                if (viewModel.measuringMode == "TOUCH" && (viewModel.basePoint != null || viewModel.topPoint != null)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = BackgroundDark.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Base: " + (viewModel.basePoint?.let { String.format(Locale.US, "X:%.0f, Y:%.0f", it.x, it.y) } ?: "—"),
                                fontSize = 11.sp,
                                color = TextLightGray.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = BackgroundDark.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Topo: " + (viewModel.topPoint?.let { String.format(Locale.US, "X:%.0f, Y:%.0f", it.x, it.y) } ?: "—"),
                                fontSize = 11.sp,
                                color = TextLightGray.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // If in AIM mode, we show "capture targets" active row on top of command bar
                if (viewModel.measuringMode == "AIM" && !viewModel.calculationPerformed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.captureBaseAngle(pitch.toDouble()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.baseAngle != null) SafeGreen else SlateBlueGray,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (viewModel.baseAngle != null) Icons.Default.CheckCircle else Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (viewModel.baseAngle != null) "Base Salva" else "Marcar Base",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.captureTopAngle(pitch.toDouble()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.topAngle != null) SafeGreen else SlateBlueGray,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (viewModel.topAngle != null) Icons.Default.CheckCircle else Icons.Default.Height,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (viewModel.topAngle != null) "Topo Salvo" else "Marcar Topo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Main triggers ROW (MEDIR / RESETAR / CALIBRAR)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A. Calibrator button
                    IconButton(
                        onClick = { isCalibratorOpen = true },
                        modifier = Modifier
                            .size(54.dp)
                            .background(SlateBlueGray.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, EdgeBorderLight.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = MinimalSkyBlue
                        )
                    }

                    // B. RESET button
                    IconButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(SlateBlueGray.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, EdgeBorderLight.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar",
                            tint = Color.White
                        )
                    }

                    // C. MEASURE action trigger (Minimal Sky Blue & Contrast Navy)
                    val isReadyToMeasure = if (viewModel.measuringMode == "TOUCH") {
                        viewModel.basePoint != null && viewModel.topPoint != null
                    } else {
                        viewModel.baseAngle != null && viewModel.topAngle != null
                    }

                    Button(
                        onClick = {
                            viewModel.computeHeight(pitch.toDouble(), screenHeight?.toDouble())
                        },
                        enabled = isReadyToMeasure,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("measure_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MinimalSkyBlue,
                            contentColor = ContrastNavy,
                            disabledContainerColor = SlateBlueGray.copy(alpha = 0.5f),
                            disabledContentColor = TextLightGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(27.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = if (isReadyToMeasure) ContrastNavy else TextLightGray.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MEDIR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isReadyToMeasure) ContrastNavy else TextLightGray.copy(alpha = 0.3f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // 5. THE HERO CARD - Measurement computation results banner
            AnimatedVisibility(
                visible = viewModel.calculationPerformed,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                ResultCard(
                    height = viewModel.measuredHeight,
                    precision = viewModel.measuredPrecision,
                    distance = viewModel.measuredDistance,
                    isAutoMethod = viewModel.distanceMethod == "AUTO",
                    onDismiss = { viewModel.calculationPerformed = false }
                )
            }

            // 6. BOTTOM SHEET SETTINGS DRAWER
            if (isCalibratorOpen) {
                CalibrationDrawer(
                    viewModel = viewModel,
                    onDismiss = { isCalibratorOpen = false }
                )
            }
        }
    }
}

// CameraX component loader
@Composable
fun CameraXPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera lifecycle binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}

// Segmented controller overlay
@Composable
fun SegmentedModeSelector(
    activeMode: String,
    onModeChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (activeMode == "TOUCH") MinimalSkyBlue else Color.Transparent,
                    CircleShape
                )
                .clip(CircleShape)
                .clickable { onModeChanged("TOUCH") }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = if (activeMode == "TOUCH") ContrastNavy else TextLightGray.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tocar na Tela",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (activeMode == "TOUCH") ContrastNavy else TextLightGray.copy(alpha = 0.5f)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (activeMode == "AIM") MinimalSkyBlue else Color.Transparent,
                    CircleShape
                )
                .clip(CircleShape)
                .clickable { onModeChanged("AIM") }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = if (activeMode == "AIM") ContrastNavy else TextLightGray.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Modo Mira",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (activeMode == "AIM") ContrastNavy else TextLightGray.copy(alpha = 0.5f)
            )
        }
    }
}

// Beautiful HUD top items
@Composable
fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(150.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (color == SafeGreen.copy(alpha = 0.85f)) Color.White else MinimalSkyBlue,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label, 
                    fontSize = 9.sp, 
                    color = if (color == SafeGreen.copy(alpha = 0.85f)) Color.White.copy(alpha = 0.8f) else TextLightGray.copy(alpha = 0.5f), 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Black, 
                    color = if (color == SafeGreen.copy(alpha = 0.85f)) Color.White else TextLightGray
                )
            }
        }
    }
}

// Active instruction bar
@Composable
fun InstructionBanner(
    mode: String,
    hasBase: Boolean,
    hasTop: Boolean,
    rollAngle: Float
) {
    val isLevel = abs(rollAngle) < 3.0f

    val text = when {
        !isLevel -> "Gire o celular lateralmente para alinhar o nível (0°)"
        mode == "TOUCH" && !hasBase -> "Toque no ponto da BASE do andaime"
        mode == "TOUCH" && !hasTop -> "Toque no ponto do TOPO do andaime"
        mode == "TOUCH" -> "Pontos posicionados! Toque em MEDIR"
        mode == "AIM" && !hasBase -> "Mire os retículos na BASE e marque o ponto"
        mode == "AIM" && !hasTop -> "Mire os retículos no TOPO e marque o ponto"
        else -> "Mira salva! Toque em MEDIR"
    }

    val icon = when {
        !isLevel -> Icons.Default.Info
        else -> Icons.Default.HelpOutline
    }

    val containerColor = when {
        !isLevel -> DangerRed.copy(alpha = 0.9f)
        hasBase && hasTop -> SafeGreen.copy(alpha = 0.9f)
        else -> MinimalSkyBlue.copy(alpha = 0.92f)
    }

    val contentColor = when {
        !isLevel -> Color.White
        hasBase && hasTop -> Color.White
        else -> ContrastNavy
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Touch points overlay composables
@Composable
fun CircleTargetMarker(
    label: String,
    offset: Offset,
    color: Color,
    isDragging: Boolean
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .padding(16.dp) // creates interaction touch surface padding
            // Center the composable frame directly on coordinates
            .offset(
                x = (offset.x - 36).dp,
                y = (offset.y - 36).dp
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = 24f
            
            // Outer pulse glow if dragging
            if (isDragging) {
                drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = baseRadius * 1.8f
                )
            }

            // Main targeting ring
            drawCircle(
                color = color,
                radius = baseRadius,
                style = Stroke(width = 4f)
            )

            // Inner solid alignment center
            drawCircle(
                color = Color.White,
                radius = 7f
            )

            // Crosshair lines
            drawLine(
                color = color,
                start = Offset(center.x - 35f, center.y),
                end = Offset(center.x + 35f, center.y),
                strokeWidth = 3f
            )
            drawLine(
                color = color,
                start = Offset(center.x, center.y - 35f),
                end = Offset(center.x, center.y + 35f),
                strokeWidth = 3f
            )
        }

        // Target Tag badge overlay
        Surface(
            color = color,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// Gorgeous Industrial results display board in Clean Minimalism Theme
@Composable
fun ResultCard(
    height: Double,
    precision: Double,
    distance: Double,
    isAutoMethod: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("measurement_result_card")
            .border(1.dp, EdgeBorderLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = NeutralCardLight),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header: Status title and compact precision badge side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALTURA ESTIMADA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeutralGrayMuted,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", height),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = BackgroundDark,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "m",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = BackgroundDark,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                }

                // Precision pill
                Surface(
                    color = MinimalSkyBlue,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, EdgeBorderLight.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = String.format(Locale.US, "PRECISÃO ±%.1fm", precision),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContrastNavy,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle contextual label
            Text(
                text = if (isAutoMethod) "Distância base calculada via inclinação vertical" else "Distância base inserida manualmente pelo operador",
                fontSize = 11.sp,
                color = NeutralGrayMuted,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Information Grid divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EdgeBorderLight)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary data displays row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DISTÂNCIA BASE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralGrayMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f m", distance),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = BackgroundDark
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(EdgeBorderLight.copy(alpha = 0.5f))
                        .align(Alignment.CenterVertically)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp)
                ) {
                    Text(
                        text = "MÉTODO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralGrayMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAutoMethod) "Automático" else "Manual",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = BackgroundDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Dismiss Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateBlueGray,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Aferir Nova Leitura",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// Gorgeous collapsible settings bottom panel - Styled in Clean Minimalism
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDrawer(
    viewModel: ScaffoldHeightViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Configurações e Calibração",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextLightGray
            )
            Text(
                text = "Ajuste os parâmetros para obter resultados perfeitamente precisos no canteiro.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightGray.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Method Selector: Manual vs Auto
            Text(
                text = "Método da Distância",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalSkyBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateBlueGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.distanceMethod = "MANUAL" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.distanceMethod == "MANUAL") MinimalSkyBlue else Color.Transparent,
                        contentColor = if (viewModel.distanceMethod == "MANUAL") ContrastNavy else TextLightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        "Distância Manual", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.distanceMethod = "AUTO" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.distanceMethod == "AUTO") MinimalSkyBlue else Color.Transparent,
                        contentColor = if (viewModel.distanceMethod == "AUTO") ContrastNavy else TextLightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        "Cálculo Do Solo", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sliders block depending on method
            if (viewModel.distanceMethod == "MANUAL") {
                // Manual Distance slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distância até o Andaime",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextLightGray
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f m", viewModel.manualDistance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MinimalSkyBlue
                    )
                }
                Slider(
                    value = viewModel.manualDistance.toFloat(),
                    onValueChange = { viewModel.manualDistance = it.toDouble() },
                    valueRange = 1.0f..30.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalSkyBlue,
                        activeTrackColor = MinimalSkyBlue,
                        inactiveTrackColor = SlateBlueGray.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Insira a distância horizontal entre a sua posição e o andaime.",
                    fontSize = 10.sp,
                    color = TextLightGray.copy(alpha = 0.6f)
                )
            } else {
                // Auto eyeLevel slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Altura do Celular (do Solo)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextLightGray
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f m", viewModel.eyeHeight),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MinimalSkyBlue
                    )
                }
                Slider(
                    value = viewModel.eyeHeight.toFloat(),
                    onValueChange = { viewModel.eyeHeight = it.toDouble() },
                    valueRange = 1.00f..2.20f,
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalSkyBlue,
                        activeTrackColor = MinimalSkyBlue,
                        inactiveTrackColor = SlateBlueGray.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Sua altura até os olhos estando em pé (geralmente cerca de 1.6m). O app calculará a distância inclinando o sensor até a base.",
                    fontSize = 10.sp,
                    color = TextLightGray.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FOV Slider for perfect device calibration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campo de Visão (FOV Vertical)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextLightGray
                )
                Text(
                    text = String.format(Locale.US, "%.1f°", viewModel.cameraFov),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MinimalSkyBlue
                )
            }
            Slider(
                value = viewModel.cameraFov.toFloat(),
                onValueChange = { viewModel.cameraFov = it.toDouble() },
                valueRange = 35.0f..75.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MinimalSkyBlue,
                    activeTrackColor = MinimalSkyBlue,
                    inactiveTrackColor = SlateBlueGray.copy(alpha = 0.5f)
                )
            )
            Text(
                text = "Calibra a sensibilidade angular do canhão da câmera. O padrão Android para lentes padrão é cerca de 54°.",
                fontSize = 10.sp,
                color = TextLightGray.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalSkyBlue,
                    contentColor = ContrastNavy
                )
            ) {
                Text("Confirmar Parâmetros", fontWeight = FontWeight.Bold)
            }
        }
    }
}
