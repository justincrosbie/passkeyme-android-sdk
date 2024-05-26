package com.passkeyme.passkeymedemoapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.passkeyme.PasskeymeSDK
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.IOException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.RequestBody.Companion.toRequestBody

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

data class RegStartRequest(
    val username: String,
    val displayName: String
)
data class AuthStartRequest(
    val username: String,
)
data class RegCompleteRequest(
    val username: String,
    val credential: String
)
data class AuthCompleteRequest(
    val credential: String
)
data class StartResponse(
    val challenge: String,
)
data class CompleteResponse(
    val credential: String,
)

class MainActivity : Activity() {

    val appId = "90316f5a-c6d2-4d60-841b-76c74881eaf7"
    val apiKey = "QGXPWRTMLfbP10LmTyYzUlkQPc9tTE53"

    val mediaType = "application/json; charset=utf-8".toMediaType()

    private val passkeymeSDK = PasskeymeSDK(this)
    private val backendURL = "https://passkeyme.com/webauthn/$appId"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registerButton: Button = findViewById(R.id.button_register)
        val authenticateButton: Button = findViewById(R.id.button_authenticate)

        registerButton.setOnClickListener { startRegistration() }
        authenticateButton.setOnClickListener { startAuthentication() }
    }

    private fun startRegistration() {

        val jsonAdapter = moshi.adapter(RegStartRequest::class.java)
        val postData = RegStartRequest("testuser", "Test User")
        val json = jsonAdapter.toJson(postData)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$backendURL/start_registration")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error getting registration challenge", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error getting registration challenge", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response = response.body?.string() ?: ""

                    val jsonAdapter = moshi.adapter(StartResponse::class.java)
                    val challenge = jsonAdapter.fromJson(response)
                    runOnUiThread {
                        if (challenge != null) {
                            registerPasskey(challenge.challenge)
                        }
                    }
                } else {
                    Log.e(TAG, "Error getting registration challenge: ${response}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error getting registration challenge", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun registerPasskey(challenge: String) {
        passkeymeSDK.passkeyRegister(challenge, object : PasskeymeSDK.Callback {
            override fun onSuccess(credential: String) {
                completeRegistration(credential)
            }

            override fun onError(error: String) {
                Log.e(TAG, "Registration error: $error")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Registration error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun completeRegistration(credential: String) {
        val jsonAdapter = moshi.adapter(RegCompleteRequest::class.java)
        val postData = RegCompleteRequest("testuser", credential)
        val json = jsonAdapter.toJson(postData)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$backendURL/complete_registration")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error completing registration", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error completing registration", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Registration completed")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Registration completed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error completing registration: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error completing registration", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun startAuthentication() {
        val jsonAdapter = moshi.adapter(AuthStartRequest::class.java)
        val postData = AuthStartRequest("testuser")
        val json = jsonAdapter.toJson(postData)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$backendURL/start_authentication")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error getting authentication challenge", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error getting authentication challenge", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response = response.body?.string() ?: ""

                    val jsonAdapter = moshi.adapter(StartResponse::class.java)
                    val challenge = jsonAdapter.fromJson(response)

                    runOnUiThread {
                        if (challenge != null) {
                            authenticatePasskey(challenge.challenge)
                        }
                    }
                } else {
                    Log.e(TAG, "Error getting authentication challenge: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error getting authentication challenge", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun authenticatePasskey(challenge: String) {
        passkeymeSDK.passkeyAuthenticate(challenge, object : PasskeymeSDK.Callback {
            override fun onSuccess(credential: String) {
                completeAuthentication(credential)
            }

            override fun onError(error: String) {
                Log.e(TAG, "Authentication error: $error")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun completeAuthentication(credential: String) {
        val jsonAdapter = moshi.adapter(AuthCompleteRequest::class.java)
        val postData = AuthCompleteRequest(credential)
        val json = jsonAdapter.toJson(postData)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$backendURL/complete_authentication")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error completing authentication", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error completing authentication", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Authentication completed")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Authentication completed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error completing authentication: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error completing authentication", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}