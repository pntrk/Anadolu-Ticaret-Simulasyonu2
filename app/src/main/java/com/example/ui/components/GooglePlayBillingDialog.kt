package com.example.ui.components
import com.example.ui.theme.RobotoMonoFontFamily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ThemeNeonCyan

data class GooglePlayPackage(
    val id: String,
    val name: String,
    val gems: Int,
    val bonusGems: Int,
    val priceText: String,
    val priceTry: Double,
    val badge: String? = null,
    val isBestValue: Boolean = false
)

@Composable
fun GooglePlayBillingDialog(
    packageInfo: GooglePlayPackage,
    onDismiss: () -> Unit,
    onConfirmPurchase: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF101726),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Google Play Store",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = RobotoMonoFontFamily
                )
                Text(
                    text = "${packageInfo.name}\n(${packageInfo.gems + packageInfo.bonusGems} 💎 Elmas) satın alınıyor...",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = packageInfo.priceText,
                    color = ThemeNeonCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("İptal", color = Color.Gray)
                    }
                    AppButton(
                        onClick = onConfirmPurchase,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026))
                    ) {
                        Text("ONAYLA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
