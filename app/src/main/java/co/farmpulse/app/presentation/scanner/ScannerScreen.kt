package co.farmpulse.app.presentation.scanner

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.presentation.components.*
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToResult: (TreeAnalysisResult) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Permission state for camera
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Media permission state
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    rememberPermissionState(mediaPermission)

    // Navigate when a result arrives and immediately clear it in the callback
    LaunchedEffect(state.result) {
        state.result?.let { result ->
            onNavigateToResult(result)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onCameraImageCaptured()
    }

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { viewModel.refreshQuota() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Screen title
                Text(
                    text = "Farm scanner",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Upload zone ───────────────────────────────────────────────────
                UploadZone(
                    imageUri = state.selectedImageUri,
                    onClick = { showBottomSheet = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Optional fields ───────────────────────────────────────────────
                SectionHeading(text = "County (optional)")
                AppTextField(
                    value = state.county,
                    onValueChange = { viewModel.onCountyChanged(it) },
                    placeholder = "e.g. Kiambu"
                )

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeading(text = "Farm size (acres)")
                AppTextField(
                    value = state.acres,
                    onValueChange = { viewModel.onAcresChanged(it) },
                    placeholder = "e.g. 2.5",
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ── Primary action button ─────────────────────────────────────────
                val quotaExhausted = state.quotaUsed >= state.quotaLimit
                Button(
                    onClick = { viewModel.analyzeImage(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        disabledContainerColor = ForestGreen.copy(alpha = 0.4f)
                    ),
                    enabled = state.selectedImageUri != null && !state.isAnalyzing && !quotaExhausted
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (quotaExhausted) "No analyses remaining" else "Analyze farm",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (quotaExhausted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Monthly limit reached. Resets ${state.quotaResetsAt}.",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                state.error?.let { err ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = err,
                        fontSize = 13.sp,
                        color = DangerTerracotta,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // ── Image Source Bottom Sheet ────────────────────────────────────────────────
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceWhite,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderGrey) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "Select image source",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )

                ListItem(
                    headlineContent = { Text("Take photo", fontWeight = FontWeight.Medium) },
                    leadingContent = { 
                        Icon(Icons.Outlined.PhotoCamera, null, tint = ForestGreen) 
                    },
                    modifier = Modifier.clickable {
                        if (cameraPermissionState.status.isGranted) {
                            val uri = viewModel.prepareCameraUri(context)
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    headlineContent = { Text("Choose from gallery", fontWeight = FontWeight.Medium) },
                    leadingContent = { 
                        Icon(Icons.Outlined.PhotoLibrary, null, tint = ForestGreen) 
                    },
                    modifier = Modifier.clickable {

                        galleryLauncher.launch("image/*")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun UploadZone(imageUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (imageUri != null) SurfaceVariant else Color(0xFFF4FAF6))
            .border(
                width = 1.dp,
                color = if (imageUri != null) BorderGrey else Color(0xFFB8DAC5),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Selected farm image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Upload farm image",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Drone or satellite photo works best",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, fontSize = 14.sp, color = SecondaryText)
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = OnSurfaceCharcoal
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = SurfaceWhite,
            focusedContainerColor = SurfaceWhite,
            unfocusedBorderColor = BorderGrey,
            focusedBorderColor = ForestGreen,
            cursorColor = ForestGreen
        )
    )
}
