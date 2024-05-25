package com.passkeyme.passkeymedemoapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.passkeyssdk.PasskeysSDK
import okhttp3.*

class MainActivity : AppCompatActivity() {

    private val passkeysSDK = PasskeysSDK(this)
    private val backendURL = "https://your-backend-url.com"
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
        val request = Request.Builder()
            .url("$backendURL/start_registration")
            .post(RequestBody.create(null, ""))
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
                    val challenge = response.body()?.string() ?: ""
                    runOnUiThread { registerPasskey(challenge) }
                } else {
                    Log.e(TAG, "Error getting registration challenge: ${response.message()}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error getting registration challenge", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun registerPasskey(challenge: String) {
        passkeysSDK.passkeyRegister(challenge, object : PasskeysSDK.Callback {
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
        val body = RequestBody.create(MediaType.parse("application/json"), "{\"credential\":\"$credential\"}")
        val request = Request.Builder()
            .url("$backendURL/complete_registration")
            .post(body)
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
                    Log.e(TAG, "Error completing registration: ${response.message()}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error completing registration", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun startAuthentication() {
        val request = Request.Builder()
            .url("$backendURL/start_authentication")
            .post(RequestBody.create(null, ""))
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
                    val challenge = response.body()?.string() ?: ""
                    runOnUiThread { authenticatePasskey(challenge) }
                } else {
                    Log.e(TAG, "Error getting authentication challenge: ${response.message()}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error getting authentication challenge", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun authenticatePasskey(challenge: String) {
        passkeysSDK.passkeyAuthenticate(challenge, object : PasskeysSDK.Callback {
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
        val body = RequestBody.create(MediaType.parse("application/json"), "{\"credential\":\"$credential\"}")
        val request = Request.Builder()
            .url("$backendURL/complete_authentication")
            .post(body)
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
                    Log.e(TAG, "Error completing authentication: ${response.message()}")
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