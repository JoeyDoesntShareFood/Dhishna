package com.example.jyothisp.dhishna;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private TextView mTextView;
    private Button mFacebookButton, mGoogleButton;

    private FirebaseAuth mAuth;


    private int RC_GOOGLE_SIGN_IN = 1;
    private CallbackManager mCallbackManager;

    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";
    private static final String LOG_TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mTextView = (TextView) findViewById(R.id.textView);
        mGoogleButton = (Button) findViewById(R.id.google_sign_in_btn);
        mFacebookButton = (Button) findViewById(R.id.fb_sign_in_btn);


        //================================Google====================================================
        /**
         * Setting up Google sign in.
         */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        final GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
            }
        });

        //==========================================================================================


        /**
         * Setting up Facebook sign in.
         */
        FacebookSdk.sdkInitialize(getApplicationContext());  //Initializing the SDK.
        AppEventsLogger.activateApp(this);      //for logging purposes.

        mCallbackManager = CallbackManager.Factory.create();    //setting up the callback manager.

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        //Facebook login success.
                        firebaseAuthWithFacebook(loginResult.getAccessToken());     //Integrating with FirebaseAuth.


                        //Code for what to do when user logged in goes here.
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });


        final Activity activity = this;
        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList(
                        PUBLIC_PROFILE, EMAIL));
            }
        });


    }


    /**
     * Function to update the UI (duh!) with the details of the user.
     * TODO: replace function logic such that the app goes to the registration field and auto-fills the data that you've got.
     */
    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            String photoUrl = user.getPhotoUrl().toString();
            String displayString = "Name: " + name + "\n"
                    + "Email ID: " + email + "\n";
            Log.v(LOG_TAG, name);
            Log.v(LOG_TAG, email);
            Log.v(LOG_TAG, photoUrl);

            mGoogleButton.setVisibility(View.GONE);
            mFacebookButton.setVisibility(View.GONE);
            mTextView.setText(displayString);
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /**
         * For the facebook Login callback.
         */
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        /**
         * For the Google sign in result.
         */
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /**
         * Loads the user from the app cache.
         */
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null)
            updateUI();
    }



    /**
     * Function to integrate the Facebook login with FirebaseAuth.
     *
     * @param token The accessToken obtained from the Facebook LoginResult.
     */
    private void firebaseAuthWithFacebook(AccessToken token) {
        Log.d(LOG_TAG, "firebaseAuthWithFacebook:" + token);
        mAuth = FirebaseAuth.getInstance();     //Creates FirebaseAuth instance.

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());   //Gets the credentians from the Facebook login.

        mAuth.signInWithCredential(credential)  //Uses those credentials to Authenticate via Firebase.
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG_TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        //Alert the user if the Login attempt was unsuccessful.
                        if (!task.isSuccessful()) {
                            Log.w(LOG_TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            updateUI();

                        }
                    }
                });
    }


    /**
     * Function to integrate the Facebook login with FirebaseAuth.
     *
     * @param acct . object containing details of the current user account.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(LOG_TAG, "firebaseAuthWithGoogle:" + acct.getId());
        mAuth = FirebaseAuth.getInstance();
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG_TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w(LOG_TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            updateUI();
                        }
                    }
                });
    }
}
