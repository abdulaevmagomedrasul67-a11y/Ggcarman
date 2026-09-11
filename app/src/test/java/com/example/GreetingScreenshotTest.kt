package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.core.model.TrafficEntry
import com.example.ui.components.TrafficItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleEntry = TrafficEntry(
      id = 1L,
      method = "GET",
      url = "https://api.example.com/v1/auth/status",
      host = "api.example.com",
      path = "/v1/auth/status",
      responseStatus = 200,
      responseStatusText = "OK",
      durationMs = 38,
      responseSizeBytes = 512,
      contentType = "application/json",
      isHttps = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TrafficItemCard(entry = sampleEntry, onClick = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

