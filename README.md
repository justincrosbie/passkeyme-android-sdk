![alt text](https://passkeyme.com/docs/img/passkeyme-logo-removebg-preview.png)
# Passkeyme Android SDK

Passkeyme Android SDK is a convenience SDK for the Passkeyme platform Android that provides simple functions to handle passkey registration and authentication using the CredentialManager API. This library helps you integrate passkey-based authentication into your web applications with ease.

See [Passkeyme](https://passkeyme.com)

## Installation

You can install the Passkey SDK via Github Packages:

settings.gradle.kts:

```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            val gpr_userProvider = providers.gradleProperty("gpr.username")
            val gpr_keyProvider = providers.gradleProperty("gpr.key")

            url = uri("https://maven.pkg.github.com/justincrosbie/passkeyme-android-sdk")
            credentials {
                username = gpr_userProvider.getOrNull()
                password = gpr_keyProvider.getOrNull()
            }
        }
    }
}
```
build.gradle.kts:

```
    implementation("com.passkeyme:passkeyme-android-sdk:1.0.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("com.google.code.gson:gson:2.10.1")
```

then
```
./gradlew clean build
```

## Usage

Importing the SDK

```
import com.passkeyme.PasskeymeSDK
```

### Creating an Instance

Create an instance of the PasskeySDK:

```
let sdk = PasskeymeSDK()
```

### Registering a Passkey

To register a passkey, use the passkeyRegister method. This method takes a challenge string as input and returns a promise that resolves to an object containing the credential.

```
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
```

### Authenticating with a Passkey

To authenticate using a passkey, use the passkeyAuthenticate method. This method takes a challenge string as input and returns a promise that resolves to an object containing the credential.

```
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
```

## API

passkeyRegister(challenge: string): Promise<{ credential: string }>

	•	challenge: A string that represents the challenge provided by your server.
	•	Returns: A promise that resolves to an object containing the credential.

passkeyAuthenticate(challenge: string): Promise<{ credential: string }>

	•	challenge: A string that represents the challenge provided by your server.
	•	Returns: A promise that resolves to an object containing the credential.

## Example

Here is a full working example. 

To get it working, first, go to [Passkeyme](https://passkeyme.com), register, create an app and grab the AppID and API Key, and populate in your env as:
```
APP_ID=
API_KEY=
```

You'll need to run it behind an addressible domain for Passkeys to work. You can host it, or use ngrok to serve it.
You'll need to follow the instructions at https://passkeyme.com/docs/docs/SDKs/android-sdk

```
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

    // Add these values to your strings.xml
    val appId = getString(R.string.APP_ID);
    val apiKey = getString(R.string.API_KEY);

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
```
