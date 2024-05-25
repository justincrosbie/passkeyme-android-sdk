package com.passkeyme;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.exceptions.CreateCredentialCancellationException;
import androidx.credentials.exceptions.CreateCredentialCustomException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialInterruptedException;
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException;
import androidx.credentials.exceptions.CreateCredentialUnknownException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PasskeymeSDK {
    private final CredentialManager credentialManager;

    private final Executor executor;
    private Context ctx;

    // Nested class for the parsed publicKey object
    static class RegPublicKeyChallenge {
        Map<String, Object> rp;
        Map<String, Object> user;
        String challenge;

        Set<Map<String, Object>> pubKeyCredParams;

        Integer timeout;

        String attestation;

        Set<Map<String, Object>> excludeCredentials;

        Map<String, Object> authenticatorSelection;
        Map<String, Object> extensions;

    }

    static class RegChallenge {
        RegPublicKeyChallenge publicKey;
    }
    static class AuthPublicKeyChallenge {
        String rpId;
        String challenge;

        Integer timeout;

        String userVerification;

        Set<Map<String, Object>> allowCredentials;

    }
    static class AuthChallenge {
        AuthPublicKeyChallenge publicKey;
    }

    static class RegResponse {
        String clientDataJSON;
        String attestationObject;

    }

    static class RegCredentialResponse {
        String id;
        String rawId;
        String type;

        RegResponse response;
    }

    public PasskeymeSDK(Context context) {
        this.ctx = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.credentialManager = CredentialManager.create(context);
    }

    public interface Callback {
        void onSuccess(String credential);
        void onError(String error);
    }

    public void registerPasskey(String requestJson, Callback callback) {
        Gson gson = new Gson();
        RegChallenge challenge;

        try {
            challenge = gson.fromJson(requestJson, RegChallenge.class);
        } catch (Exception e) {
            callback.onError("Failed to parse publicKey JSON" + e.getLocalizedMessage());
            return;
        }

        challenge.publicKey.authenticatorSelection.put("requireResidentKey", true);
        challenge.publicKey.authenticatorSelection.put("residentKey", "preferred");

        String publicKey = gson.toJson(challenge.publicKey);

        String relyingPartyID = (String) challenge.publicKey.rp.get("id");
        String challengeInner = challenge.publicKey.challenge;
        String userID = (String) challenge.publicKey.user.get("id");

        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                // `requestJson` contains the request in JSON format. Uses the standard
                // WebAuthn web JSON spec.
                // `preferImmediatelyAvailableCredentials` defines whether you prefer
                // to only use immediately available credentials, not  hybrid credentials,
                // to fulfill this request. This value is false by default.
                new CreatePublicKeyCredentialRequest(publicKey);

        CancellationSignal cancelSignal = new CancellationSignal();

        // Execute CreateCredentialRequest asynchronously to register credentials
        // for a user account. Handle success and failure cases with the result and
        // exceptions, respectively.
        credentialManager.createCredentialAsync(
                // Use an activity-based context to avoid undefined system
                // UI launching behavior
                ctx,
                createPublicKeyCredentialRequest,
                cancelSignal,
                executor,
                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                    @Override
                    public void onResult(CreateCredentialResponse result) {
                        String credential = null;
                        try {
                            credential = handleSuccessfulCreatePasskeyResult(result);
                            callback.onSuccess(credential);
                        } catch (Exception e) {
                            callback.onError(e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onError(CreateCredentialException e) {
                        callback.onError(e.getLocalizedMessage());
                    }
                }
        );
    }

    public boolean authenticatePasskey(String requestJson, Callback callback) {
        Gson gson = new Gson();
        AuthChallenge challenge;

        try {
            challenge = gson.fromJson(requestJson, AuthChallenge.class);
        } catch (Exception e) {
            callback.onError("Failed to parse publicKey JSON" + e.getLocalizedMessage());
            return false;
        }

        String publicKey = gson.toJson(challenge.publicKey);

// Get passkey from the user's public key credential provider.
        GetPublicKeyCredentialOption getPublicKeyCredentialOption =
                new GetPublicKeyCredentialOption(publicKey);

        GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(getPublicKeyCredentialOption)
                .build();

        CancellationSignal cancelSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(
                // Use activity based context to avoid undefined
                // system UI launching behavior
                ctx,
                getCredRequest,
                cancelSignal,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        String credential = null;
                        try {
                            credential = handleSignIn(result);
                            callback.onSuccess(credential);
                        } catch (Exception e) {
                            callback.onError(e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        try {
                            handleAuthError(e);
                        } catch (Exception ex) {
                            callback.onError(e.getLocalizedMessage());
                        }
                    }
                }
        );

        return true;
    }

    public String handleSignIn(GetCredentialResponse result) throws Exception {

        // Handle the successfully returned credential.
        Credential credential = result.getCredential();
        if (credential instanceof PublicKeyCredential) {
            String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
            // Share responseJson i.e. a GetCredentialResponse on your server to validate and authenticate
            return responseJson;
        } else if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            // Use id and password to send to your server to validate and authenticate
            throw new Exception("PasswordCredential not supported");
        } else if (credential instanceof CustomCredential) {
//                if (ExampleCustomCredential.TYPE.equals(credential.getType())) {
//                    try {
//                        ExampleCustomCredential customCred = ExampleCustomCredential.createFrom(customCredential.getData());
//                        // Extract the required credentials and complete the
//                        // authentication as per the federated sign in or any external
//                        // sign in library flow
//                    } catch (ExampleCustomCredential.ExampleCustomCredentialParsingException e) {
//                        // Unlikely to happen. If it does, you likely need to update the
//                        // dependency version of your external sign-in library.
//                        Log.e(TAG, "passkeyme: Failed to parse an ExampleCustomCredential", e);
//                    }
//                } else {
//                    // Catch any unrecognized custom credential type here.
//                    Log.e(TAG, "passkeyme: Unexpected type of credential");
//                }
            throw new Exception("CustomCredential not supported");
        } else {
            // Catch any unrecognized credential type here.
            throw new Exception("Unexoected credential type not supported");
        }
    }


    public String handleSuccessfulCreatePasskeyResult(CreateCredentialResponse result) throws Exception {

        // Extract data from the response
        try {

            if ( result instanceof CreatePublicKeyCredentialResponse ) {

                String responseJson = ((CreatePublicKeyCredentialResponse) result).getRegistrationResponseJson();

                return responseJson;
            } else {
                throw new Exception("unexpected credential: " + result);
            }
        } catch (Exception e) {
            throw new Exception(e.getLocalizedMessage());
        }
    }

    public void handleAuthError(Exception e) throws Exception {
        if (e instanceof CreatePublicKeyCredentialDomException) {
            // Handle the passkey DOM errors thrown according to the
            // WebAuthn spec.

            // androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException: One of the excluded credentials exists on the local device
            // errorMessage: One of the excluded credentials exists on the local device
            // type: androidx.credentials.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION/androidx.credentials.TYPE_INVALID_STATE_ERROR

            throw new Exception("Registration Dom failure: " + ((CreatePublicKeyCredentialDomException)e).getDomError());
        } else if (e instanceof CreateCredentialCancellationException) {
            // The user intentionally canceled the operation and chose not
            // to register the credential.
            throw new Exception("Registration Cancelled: " + e.getLocalizedMessage());
        } else if (e instanceof CreateCredentialInterruptedException) {
            // Retry-able error. Consider retrying the call.
            throw new Exception("Registration Interrupted: " + e.getLocalizedMessage());
        } else if (e instanceof CreateCredentialProviderConfigurationException) {
            // Your app is missing the provider configuration dependency.
            // Most likely, you're missing the
            // "credentials-play-services-auth" module.
            throw new Exception("Registration Provider Config Error: " + e.getLocalizedMessage());
        } else if (e instanceof CreateCredentialUnknownException) {
            throw new Exception("Registration Unknown Error: " + e.getLocalizedMessage());
        } else if (e instanceof CreateCredentialCustomException) {
            // You have encountered an error from a 3rd-party SDK. If
            // you make the API call with a request object that's a
            // subclass of
            // CreateCustomCredentialRequest using a 3rd-party SDK,
            // then you should check for any custom exception type
            // constants within that SDK to match with e.type.
            // Otherwise, drop or log the exception.
            throw new Exception("Registration Custom Error: " + e.getLocalizedMessage());
        } else {
            throw new Exception("Unexpected exception type: " + e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
    }
}
