package com.phenix.wirelessadb.pairing

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Trust manager that accepts all certificates.
 *
 * Used for ADB pairing where the device uses self-signed certificates.
 * This is secure because the pairing code provides authentication.
 *
 * WARNING: Only use for ADB pairing connections, not for general HTTPS.
 */
@SuppressLint("CustomX509TrustManager")
class TrustAllManager : X509TrustManager {

  @SuppressLint("TrustAllX509TrustManager")
  override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    // Accept all client certificates (we're the client, not server)
  }

  @SuppressLint("TrustAllX509TrustManager")
  override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    // Accept all server certificates (device uses self-signed cert)
  }

  override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
