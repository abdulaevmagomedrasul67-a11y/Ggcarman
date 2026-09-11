package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DisclaimerTopBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Shield",
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Authorized Inspection Only — Test own apps and local network endpoints",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DisclaimerBlockingDialog(
    isOpen: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    if (!isOpen) return

    var isChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Blocking dialog */ },
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = ElectricAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DISCLAIMER / ОТКАЗ ОТ ОТВЕТСТВЕННОСТИ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Данное программное обеспечение «API Flow Inspector» создано ИСКЛЮЧИТЕЛЬНО для:\n" +
                            "• тестирования безопасности и отладки СВОИХ собственных сетей, API и приложений;\n" +
                            "• образовательных целей и контроля качества (QA);\n" +
                            "• авторизованного аудита (при наличии письменного разрешения владельца).\n\n" +
                            "АВТОР НЕ НЕСЁТ ОТВЕТСТВЕННОСТИ за любой ущерб, причинённый прямо или косвенно в результате использования данного ПО.\n\n" +
                            "КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО использовать приложение:\n" +
                            "✗ против чужих сетей и устройств без разрешения;\n" +
                            "✗ для несанкционированного перехвата конфиденциальных данных;\n" +
                            "✗ для любых противоправных действий (включая ст. 272, 273 УК РФ и международные законы).\n\n" +
                            "Используя приложение, вы подтверждаете, что действуете в рамках закона и несёте ПОЛНУЮ личную ответственность за свои действия.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TerminalGreen,
                            uncheckedColor = TextMuted,
                            checkmarkColor = DarkSurface
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Я прочитал и согласен / I confirm authorized testing",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = isChecked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = DarkSurface,
                    disabledContainerColor = DarkBorder,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Принять и продолжить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CriticalRed.copy(alpha = 0.5f))),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Отказаться и выйти")
            }
        }
    )
}
