package com.example.jyothisp.dhishna;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.Animatable2Compat;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {


    private ImageView mProgressView;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private TextView mProgressTextView;
    private View mLoginFormView;


    private FirebaseAuth mAuth;


    private int RC_GOOGLE_SIGN_IN = 1;
    private int RC_REGISTER = 3;
    private int RC_EMAIL_SIGN_IN = 2;
    private CallbackManager mCallbackManager;

    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";
    private static final String LOG_TAG = "LoginActivity";
    private static final String ACTIVITY_MODE = "ACTIVITY_MODE";
    private static final boolean ACTIVITY_EMAIL_CREATE = true;
    private static final boolean ACTIVITY_REGISTER = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = (ImageView) findViewById(R.id.login_progress);
        mProgressTextView = (TextView) findViewById(R.id.text_progress);
        mEmailEditText = (EditText) findViewById(R.id.email);
        mPasswordEditText = (EditText) findViewById(R.id.password);

        Button mGoogleButton = (Button) findViewById(R.id.google_sign_in_btn);
        Button mFacebookButton = (Button) findViewById(R.id.fb_sign_in_btn);
        Button mRegisterButton = (Button) findViewById(R.id.email_sign_up_btn);
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_btn);

        mAuth = FirebaseAuth.getInstance();

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
                mProgressTextView.setText("Logging in with Google");
                showProgress(true);
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
            }
        });


        /**
         * Setting up Sign In with Email.
         */
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = mEmailEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();
                signInWithEmail(email, password);
                FirebaseUser user = mAuth.getCurrentUser();
                if (!user.isEmailVerified()){
                    //TODO this case arises when the registration of the particular user was dropped halfway.
                    Log.d(LOG_TAG, "SignInWithEmail: Email not verified.");
                    Toast.makeText(LoginActivity.this, "Your registration met with an error.", Toast.LENGTH_SHORT).show();
                    user.delete()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Toast.makeText(LoginActivity.this, "Please register again.", Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                }
            }
        });

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
                        mProgressTextView.setText("Integrating with Firebase");
                        firebaseAuthWithFacebook(loginResult.getAccessToken());     //Integrating with FirebaseAuth.

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
                mProgressTextView.setText("Logging in with FB");
                showProgress(true);
                LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList(
                        PUBLIC_PROFILE, EMAIL));
            }
        });


        /**
         * Setting up new Email Registration.
         */
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                intent.putExtra(ACTIVITY_MODE, ACTIVITY_EMAIL_CREATE);
                startActivityForResult(intent, RC_EMAIL_SIGN_IN);
            }
        });



    }


    /**
     * Signs in with email
     * @param email Email ID
     * @param password Password
     */
    private void signInWithEmail(String email, String password){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Log.d(LOG_TAG, "signInWithEmail: Sign In successful");

                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.d(LOG_TAG, "signInWithEmail: Sign In Failed");
                            setResult(RESULT_CANCELED);

                        }
                    }
                });
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
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {

                mProgressTextView.setText("Integrating with Firebase");
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show();

                setResult(RESULT_CANCELED);
            }

        }

        /**
         *  For the email reg. result.
         */
        if (requestCode == RC_EMAIL_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                //Email Sign in success.
                //TODO: do stuff.

                setResult(RESULT_OK);
                finish();

            }

        }

        if (requestCode == RC_REGISTER){
            showProgress(false);
            if (resultCode == RESULT_OK){
                setResult(RESULT_OK);
                finish();
            }
        }

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
                            showProgress(false);
                            Log.w(LOG_TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            mProgressTextView.setText("Getting ready to collect data");
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            pushUserDetailsToDB(user);

                        }
                    }
                });
    }


    /**
     * Function to integrate the Facebook login with FirebaseAuth.
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
                        showProgress(false);
                        if (!task.isSuccessful()) {
                            Log.w(LOG_TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            setResult(RESULT_CANCELED);
                        } else {
                            mProgressTextView.setText("Getting ready to collect data");
                            showProgress(true);
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            pushUserDetailsToDB(user);
                        }
                    }
                });
    }

    /**
     * Shows the progress UI and hides the login form.
     * @param show boolean dictating whether or not the progress animation should be shown.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show){
            AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(this, R.drawable.logo_loading_vector);
            mProgressView.setImageDrawable(avd);
            final Animatable animatable = (Animatable) mProgressView.getDrawable();
            animatable.start();
            avd.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    if (show)
                        animatable.start();
                    else {
                        mProgressView.setVisibility(View.GONE);
                        mProgressTextView.setVisibility(View.GONE);
                    }
                }
            });
        }


    }


    /**
     * Function that checks if the data of the user is stored in the Database.
     * If not, it sends an intent to collect the data from the user and push it to the database.
     *
     * @param user Object containing the currently signed in user.
     */
    private void pushUserDetailsToDB(final FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference reference = database.getReference("users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child(user.getUid()).exists()) {
                    Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                    String name, email;
                    name = user.getDisplayName();
                    email = user.getEmail();
                    intent.putExtra("name", name);
                    intent.putExtra("email", email);
                    intent.putExtra(ACTIVITY_MODE, ACTIVITY_REGISTER);
                    startActivityForResult(intent, RC_REGISTER);
                } else{
                    showProgress(false);
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
