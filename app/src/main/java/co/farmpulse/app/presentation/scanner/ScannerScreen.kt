package co.farmpulse.app.presentation.scanner

import android.net.Uri
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
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage

// ─────────────────────────────────────────────────────────────────────────────
// ScannerScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToResult: () -> Unit = {}
) {
    val state   by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigate when a result arrives
    LaunchedEffect(state.result) {
        if (state.result != null) onNavigateToResult()
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // Camera launcher (requires a FileProvider URI prepared in ViewModel)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onCameraImageCaptured()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)     // #F8F7F2
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Screen title — 17sp Medium Charcoal
        Text(
            text       = "Farm scanner",
            fontSize   = 17.sp,
            fontWeight = FontWeight.Medium,
            color      = OnSurfaceCharcoal
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Quota card ────────────────────────────────────────────────────
        QuotaCard(
            used         = state.quotaUsed,
            limit        = state.quotaLimit,
            resetsAt     = state.quotaResetsAt
        )

        // ── Upload zone ───────────────────────────────────────────────────
        UploadZone(imageUri = state.selectedImageUri)

        // ── Image source row ──────────────────────────────────────────────
        // FIX: original chips had no click handling; wire them up here
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceChip(
                modifier = Modifier.weight(1f),
                icon     = Icons.Outlined.PhotoCamera,
                label    = "Camera",
                onClick  = {
                    val uri = viewModel.prepareCameraUri(context)
                    cameraLauncher.launch(uri)
                }
            )
            SourceChip(
                modifier = Modifier.weight(1f),
                icon     = Icons.Outlined.PhotoLibrary,
                label    = "Gallery",
                onClick  = { galleryLauncher.launch("image/*") }
            )
        }

        // ── Optional fields ───────────────────────────────────────────────
        FieldLabel("County (optional)")
        AppTextField(
            value         = state.county,
            onValueChange = { viewModel.onCountyChanged(it) },
            placeholder   = "e.g. Kiambu"
        )

        FieldLabel("Farm size (acres)")
        AppTextField(
            value         = state.acres,
            onValueChange = { viewModel.onAcresChanged(it) },
            placeholder   = "e.g. 2.5",
            keyboardType  = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Primary action button ─────────────────────────────────────────
        // FIX: original had no onClick logic; wired to ViewModel.
        // Disabled when no image or quota is exhausted.
        val quotaExhausted = state.quotaUsed >= state.quotaLimit
        Button(
            onClick  = { viewModel.analyzeImage(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = ForestGreen,   // #2D6A4F
                disabledContainerColor = ForestGreen.copy(alpha = 0.4f)
            ),
            enabled  = state.selectedImageUri != null && !state.isAnalyzing && !quotaExhausted
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(
                    color       = Color.White,
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = if (quotaExhausted) "No analyses remaining" else "Analyze farm",
                    color      = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp
                )
            }
        }

        // Quota exhausted hint text
        if (quotaExhausted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text     = "You've used all analyses this month. Resets ${state.quotaResetsAt}.",
                fontSize = 12.sp,
                color    = SecondaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Error state
        state.error?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text     = err,
                fontSize = 13.sp,
                color    = DangerTerracotta   // #C45C26
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuotaCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuotaCard(used: Int, limit: Int, resetsAt: String) {
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

    // FIX: use border+background pattern instead of CardDefaults elevation
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp))
            .background(SurfaceWhite, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = "Monthly analyses",
                fontSize = 12.sp,
                color    = SecondaryText
            )
            Text(
                text       = "$used of $limit used",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = ForestGreen           // #2D6A4F
            )
        }

        // Progress bar — LightGreen fill on warm-grey track
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(SurfaceVariant, RoundedCornerShape(3.dp))   // #F0EFEA track
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(LightGreen, RoundedCornerShape(3.dp))   // #52B788 fill
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = "${limit - used} remaining · resets $resetsAt",
            fontSize = 11.sp,
            color    = SecondaryText
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UploadZone
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UploadZone(imageUri: Uri?) {
    // FIX: height bumped to 160dp to match mockup proportions.
    // Shows thumbnail after selection.
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4FAF6))                               // light green tint
            .border(1.5.dp, Color(0xFFB8DAC5), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            // Show selected image thumbnail — fills the zone
            AsyncImage(
                model              = imageUri,
                contentDescription = "Selected farm image",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector        = Icons.Outlined.UploadFile,
                    contentDescription = null,
                    tint               = LightGreen,        // #52B788
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = "Upload farm image",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = OnSurfaceCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "Drone or satellite photo works best",
                    fontSize = 12.sp,
                    color    = SecondaryText
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Source chips (Camera / Gallery)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SourceChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    // FIX: chips are now tappable with Forest Green icon+text — matching mockup
    Row(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(10.dp))   // #F0EFEA
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(15.dp),
            tint               = ForestGreen                          // #2D6A4F — matches mockup
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = ForestGreen
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Field label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FieldLabel(text: String) {
    // FIX: uppercase + letter spacing to match SectionHeading pattern
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Medium,
        color         = SecondaryText,
        letterSpacing = 0.7.sp,
        modifier      = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// AppTextField
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    // FIX: strip default TextField indicator/underline and match warm-grey input spec.
    // Original had conflicting background from both modifier and TextField's own colors.
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = {
            Text(placeholder, fontSize = 13.sp, color = SecondaryText)
        },
        textStyle     = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            color    = OnSurfaceCharcoal
        ),
        singleLine    = true,
        shape         = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor  = SurfaceVariant,    // #F0EFEA
            focusedContainerColor    = SurfaceVariant,
            unfocusedBorderColor     = BorderGrey,        // #E0DFD8
            focusedBorderColor       = ForestGreen,       // #2D6A4F on focus
            cursorColor              = ForestGreen
        )
    )
}