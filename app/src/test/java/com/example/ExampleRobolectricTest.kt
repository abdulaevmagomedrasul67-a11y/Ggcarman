package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.export.TrafficExporter
import com.example.core.model.TrafficEntry
import com.example.core.security.CertificateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("API Flow Inspector", appName)
  }

  @Test
  fun `certificate manager generates valid root CA`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val certManager = CertificateManager(context)
    val caCert = certManager.getCaCertificate()
    assertNotNull(caCert)
    val pem = certManager.getCaCertificatePem()
    assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----"))
    assertTrue(pem.trim().endsWith("-----END CERTIFICATE-----"))
  }

  @Test
  fun `traffic exporter produces valid curl command`() {
    val entry = TrafficEntry(
      id = 1L,
      method = "POST",
      url = "https://api.example.com/v1/auth",
      host = "api.example.com",
      path = "/v1/auth",
      requestHeadersJson = "{\"Authorization\":\"Bearer test_token\",\"Content-Type\":\"application/json\"}",
      requestBody = "{\"user\":\"audit_admin\"}"
    )

    val curl = TrafficExporter.toCurl(entry)
    assertTrue(curl.startsWith("curl -X POST 'https://api.example.com/v1/auth'"))
    assertTrue(curl.contains("-H 'Authorization: Bearer test_token'"))
    assertTrue(curl.contains("--data-raw '{\"user\":\"audit_admin\"}'"))
  }
}

