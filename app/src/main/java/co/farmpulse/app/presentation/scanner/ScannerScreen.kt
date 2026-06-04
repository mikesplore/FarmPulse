package co.farmpulse.app.presentation.scanner

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage

@Composable
fun ScannerScreen(viewModel: ScannerViewModel, onNavigateToResult: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    
    // Local state for optional fields
    var county by remember { mutableStateOf("") }
    var acres by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Farm scanner",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceCharcoal
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QuotaCard(state.quotaUsed, state.quotaLimit)
        
        UploadZone(state.selectedImageUri)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.PhotoCamera,
                label = "Camera"
            )
            SourceChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.PhotoLibrary,
                label = "Gallery"
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        FieldLabel("County (optional)")
        AppTextField(value = county, onValueChange = { county = it }, placeholder = "e.g. Kiambu")
        
        FieldLabel("Farm size (acres)")
        AppTextField(value = acres, onValueChange = { acres = it }, placeholder = "e.g. 2.5")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { /* TODO: Trigger analysis */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            enabled = state.selectedImageUri != null && !state.isAnalyzing
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Analyze farm", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun QuotaCard(used: Int, limit: Int) {
    val progress = used.toFloat() / limit.toFloat()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly analyses", fontSize = 13.sp, color = OnSurfaceCharcoal)
                Text("$used of $limit used", fontSize = 13.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(SurfaceVariant, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(ForestGreen, RoundedCornerShape(3.dp))
                )
            }
            Text("${limit - used} remaining · resets Jul 1", fontSize = 11.sp, color = SecondaryText)
        }
    }
}

@Composable
fun UploadZone(imageUri: Uri?) {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4FAF6))
            .border(1.5.dp, Color(0xFFB8DAC5), RoundedCornerShape(14.dp)), // Simplified dashed border
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = null,
                    tint = LightGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Upload farm image", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurfaceCharcoal)
                Text("Drone or satellite photo works best", fontSize = 12.sp, color = SecondaryText)
            }
        }
    }
}

@Composable
fun SourceChip(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = OnSurfaceCharcoal)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnSurfaceCharcoal)
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            color = SecondaryText,
            letterSpacing = 0.05.sp
        ),
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderGrey, RoundedCornerShape(8.dp)),
        placeholder = { Text(placeholder, fontSize = 13.sp, color = SecondaryText) },
        // Use default colors; container background is set via modifier
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}
