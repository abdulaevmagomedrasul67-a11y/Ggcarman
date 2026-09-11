package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.TrafficEntry
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.MethodDeleteColor
import com.example.ui.theme.MethodGetColor
import com.example.ui.theme.MethodOtherColor
import com.example.ui.theme.MethodPatchColor
import com.example.ui.theme.MethodPostColor
import com.example.ui.theme.MethodPutColor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.Status2xxColor
import com.example.ui.theme.Status3xxColor
import com.example.ui.theme.Status4xxColor
import com.example.ui.theme.Status5xxColor
import com.example.ui.theme.StatusErrorColor
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (method.uppercase()) {
        "GET" -> MethodGetColor.copy(alpha = 0.15f) to MethodGetColor
        "POST" -> MethodPostColor.copy(alpha = 0.15f) to MethodPostColor
        "PUT" -> MethodPutColor.copy(alpha = 0.15f) to MethodPutColor
        "DELETE" -> MethodDeleteColor.copy(alpha = 0.15f) to MethodDeleteColor
        "PATCH" -> MethodPatchColor.copy(alpha = 0.15f) to MethodPatchColor
        "CONNECT" -> PurpleAccent.copy(alpha = 0.15f) to PurpleAccent
        else -> MethodOtherColor.copy(alpha = 0.15f) to MethodOtherColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = method.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StatusBadge(statusCode: Int, isSslError: Boolean = false, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when {
        isSslError || statusCode == -1 -> Triple(
            CriticalRed.copy(alpha = 0.2f),
            CriticalRed,
            "SSL ERR"
        )
        statusCode in 200..299 -> Triple(
            Status2xxColor.copy(alpha = 0.15f),
            Status2xxColor,
            statusCode.toString()
        )
        statusCode in 300..399 -> Triple(
            Status3xxColor.copy(alpha = 0.15f),
            Status3xxColor,
            statusCode.toString()
        )
        statusCode in 400..499 -> Triple(
            Status4xxColor.copy(alpha = 0.15f),
            Status4xxColor,
            statusCode.toString()
        )
        statusCode in 500..599 -> Triple(
            Status5xxColor.copy(alpha = 0.15f),
            Status5xxColor,
            statusCode.toString()
        )
        else -> Triple(
            DarkSurfaceElevated,
            TextSecondary,
            if (statusCode > 0) statusCode.toString() else "..."
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TrafficItemCard(
    entry: TrafficEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }

    val sizeStr = remember(entry.responseSizeBytes) {
        when {
            entry.responseSizeBytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", entry.responseSizeBytes / (1024f * 1024f))
            entry.responseSizeBytes > 1024 -> String.format(Locale.US, "%.1f KB", entry.responseSizeBytes / 1024f)
            else -> "${entry.responseSizeBytes} B"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (entry.isSslFailure) CriticalRed.copy(alpha = 0.4f) else DarkBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("traffic_entry_${entry.id}")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(method = entry.method)
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(statusCode = entry.responseStatus, isSslError = entry.isSslFailure)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (entry.isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (entry.isHttps) "HTTPS" else "HTTP",
                        tint = if (entry.isHttps) NeonCyan else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )

                    if (entry.mockRuleApplied != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricYellow.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "MOCKED",
                                color = ElectricYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (entry.isBreakpointHit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "BREAKPOINT",
                                color = ElectricAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Text(
                    text = timeStr,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Host & Path
            Column {
                Text(
                    text = entry.host,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.path.ifBlank { "/" },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer info: duration, size, content type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${entry.durationMs}ms  •  $sizeStr",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = entry.contentType.substringBefore(";"),
                    color = NeonCyan.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
