package com.example.core.security

import android.content.Context
import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class CertificateManager(private val context: Context) {
    private val tag = "CertificateManager"

    private var caKeyPair: KeyPair? = null
    private var caCertificate: X509Certificate? = null
    private val sslContextCache = ConcurrentHashMap<String, SSLContext>()

    init {
        loadOrCreateCa()
    }

    @Synchronized
    fun loadOrCreateCa() {
        try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048, SecureRandom())
            val keyPair = keyGen.generateKeyPair()
            caKeyPair = keyPair

            val issuer = X500Name("CN=API Flow Inspector Root CA, O=Security Debugger, OU=Interception Engine, C=US")
            val serial = BigInteger(64, SecureRandom())
            val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
            val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000L) // 10 years

            val certBuilder = JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                issuer,
                keyPair.public
            )

            // Basic Constraints: IS CA = true
            certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            // Key Usage: Cert Sign, CRL Sign, Digital Signature
            certBuilder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign or KeyUsage.digitalSignature)
            )

            val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
            val certHolder = certBuilder.build(signer)
            val cert = JcaX509CertificateConverter().getCertificate(certHolder)
            caCertificate = cert

            Log.i(tag, "Generated API Flow Root CA Certificate successfully: ${cert.subjectX500Principal}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Root CA", e)
        }
    }

    fun getCaCertificate(): X509Certificate? = caCertificate

    fun getCaCertificatePem(): String {
        val cert = caCertificate ?: return ""
        val encoder = android.util.Base64.encodeToString(cert.encoded, android.util.Base64.NO_WRAP)
        val chunked = encoder.chunked(64).joinToString("\n")
        return "-----BEGIN CERTIFICATE-----\n$chunked\n-----END CERTIFICATE-----"
    }

    fun exportCaToFile(): File? {
        val pem = getCaCertificatePem()
        if (pem.isBlank()) return null
        return try {
            val exportDir = File(context.cacheDir, "certs").apply { mkdirs() }
            val file = File(exportDir, "api_flow_inspector_ca.crt")
            FileOutputStream(file).use { it.write(pem.toByteArray(Charsets.UTF_8)) }
            file
        } catch (e: Exception) {
            Log.e(tag, "Failed to export CA to file", e)
            null
        }
    }

    fun getOrCreateSslContextForHost(hostname: String): SSLContext {
        return sslContextCache.getOrPut(hostname) {
            createSslContextForHost(hostname)
        }
    }

    private fun createSslContextForHost(hostname: String): SSLContext {
        val caKey = caKeyPair ?: throw IllegalStateException("CA KeyPair not initialized")
        val caCert = caCertificate ?: throw IllegalStateException("CA Certificate not initialized")

        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        val hostKeyPair = keyGen.generateKeyPair()

        val subject = X500Name("CN=$hostname, O=API Flow Interceptor, C=US")
        val issuer = X500Name(caCert.subjectX500Principal.name)
        val serial = BigInteger(64, SecureRandom())
        val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val notAfter = Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L)

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            subject,
            hostKeyPair.public
        )

        // Subject Alternative Names (SAN) for both DNS and IP
        val san = GeneralNames(GeneralName(GeneralName.dNSName, hostname))
        certBuilder.addExtension(Extension.subjectAlternativeName, false, san)
        certBuilder.addExtension(Extension.basicConstraints, false, BasicConstraints(false))

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(caKey.private)
        val hostCert = JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        // Create in-memory KeyStore
        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(
                "key",
                hostKeyPair.private,
                "password".toCharArray(),
                arrayOf(hostCert, caCert)
            )
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, "password".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, SecureRandom())
        return sslContext
    }

    companion object {
        fun getNetworkSecurityConfigSample(): String {
            return """
<?xml version="1.0" encoding="utf-8"?>
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <!-- Allow trusting user-installed CA certificates during debug builds -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="user" />
            <certificates src="system" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
            """.trimIndent()
        }

        fun getAndroidManifestConfigSnippet(): String {
            return """
<!-- In AndroidManifest.xml under <application>: -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
            """.trimIndent()
        }

        fun getFridaBypassSnippet(): String {
            return """
/* Frida Universal SSL Pinning Bypass Script */
Java.perform(function () {
    var array_list = Java.use("java.util.ArrayList");
    var ApiTrustManager = Java.use('javax.net.ssl.X509TrustManager');
    var SSLContext = Java.use('javax.net.ssl.SSLContext');

    console.log("[*] API Flow: Hooking SSL TrustManager for debug session...");
    
    // Bypass TrustManagerImpl checkTrusted
    try {
        var TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
        TrustManagerImpl.verifyChain.implementation = function (untrustedChain, trustAnchorChain, host, clientAuth, ocspData, tlsSctData) {
            return untrustedChain;
        };
        console.log("[+] Conscrypt TrustManagerImpl bypassed");
    } catch (e) {
        console.log("[-] Conscrypt hook skipped: " + e);
    }
});
            """.trimIndent()
        }
    }
}
