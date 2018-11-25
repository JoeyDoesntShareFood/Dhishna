package com.example.jyothisp.dhishna;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;

import android.app.Activity;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.Animatable2Compat;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * A login screen that offers login via email/password.
 */
public class EmailLoginActivity extends Activity {


    // UI references.
    private EditText mEmailView;
    private EditText mNameView;
    private EditText mPhoneView;
    private EditText mPasswordView;
    private EditText mInstituteView;
    private View mLoginFormView;
    private ImageView mProgressView;
    private TextView mProgressTextView;
    private ImageView mReloadIcon;

    private FirebaseUser firebaseUser;

    private static final String LOG_TAG = "EmailLoginActivity";
    private static final String ACTIVITY_MODE = "ACTIVITY_MODE";
    private RadioGroup mGenderGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        // Set up the UI elements.
        mNameView = (EditText) findViewById(R.id.name);
        mPhoneView = (EditText) findViewById(R.id.phone);
        mPasswordView = (EditText) findViewById(R.id.password);
        mEmailView = (EditText) findViewById(R.id.email);
        mInstituteView = (EditText) findViewById(R.id.institute);
        mGenderGroup = (RadioGroup) findViewById(R.id.gender_group);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = (ImageView) findViewById(R.id.login_progress);
        mProgressTextView = (TextView) findViewById(R.id.text_progress);
        mReloadIcon = (ImageView) findViewById(R.id.reload);


        //If the User is login in via one of the providers, we fetch their name and email ID.
        String name = getIntent().getStringExtra("name");
        if (name != null) {
            mNameView.setText(name);
        }
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            mEmailView.setText(email);
            mEmailView.setInputType(InputType.TYPE_NULL);       //They shouldn't be allowed to change the emailID from here.
            mNameView.requestFocus();
        }


        //The password field only needs to be active when the user is creating a new account.
        final boolean hasPassword = getIntent().getBooleanExtra(ACTIVITY_MODE, true);

        if (!hasPassword)
            mPasswordView.setVisibility(View.INVISIBLE);
        else {
            mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                        attemptRegistration(hasPassword);
                        return true;
                    }
                    return false;
                }
            });
        }


        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration(hasPassword);
            }
        });

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     *
     * @param hasPassword this is true only for the case of a new Registration.
     */
    private void attemptRegistration(boolean hasPassword) {

        // Reset errors.
        mEmailView.setError(null);
        mNameView.setError(null);
        mPhoneView.setError(null);
        mInstituteView.setError(null);


        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        final String name = mNameView.getText().toString();
        String phone = mPhoneView.getText().toString();
        String institute = mInstituteView.getText().toString();
        String password = null;
        String gender = ((RadioButton) findViewById(mGenderGroup.getCheckedRadioButtonId())).getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (hasPassword) {
            mPasswordView.setError(null);
            password = mPasswordView.getText().toString();

            // Check for a valid password, if the user entered one.
            if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
                mPasswordView.setError(getString(R.string.error_invalid_password));
                focusView = mPasswordView;
                cancel = true;
            }
        }


        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        //Check if name and phone number entered.
        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }        // Check for a valid email address.
        if (TextUtils.isEmpty(phone)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        if (TextUtils.isEmpty(institute)) {
            mInstituteView.setError(getString(R.string.error_field_required));
            focusView = mInstituteView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            final DhishnaUser dhishnaUser = new DhishnaUser(name, email, phone, gender, institute);

            if (hasPassword)
                createEmailAccount(password, dhishnaUser);
            else {
                mProgressTextView.setText("Pushing to Database");
                pushToDBandExit(dhishnaUser);
            }

        }
    }


    /**
     * Function which handles creating a new account with emailID and password.
     *
     * @param password    Password.
     * @param dhishnaUser A user object which contains the details of the user.
     */
    private void createEmailAccount(String password, final DhishnaUser dhishnaUser) {

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();


        /**
         * A listener which listens for when the verification email is sent
         * and waits for the user to go verify it.
         * When the mail is verified, the user details are pushed into the DB.
         */
        final OnCompleteListener<Void> verificationEmailListener = new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "emailVerification: Verification link sent");
                    mProgressTextView.setText("Email Verification Link sent. Open your email and click the link. Then click below to complete your registration.");
                    mReloadIcon.setVisibility(View.VISIBLE);
                    mReloadIcon.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            firebaseUser.reload();
                            Log.d(LOG_TAG, "emailVerification: Reloading");
                            if (firebaseUser.isEmailVerified()) {
                                mReloadIcon.setVisibility(View.GONE);
                                Log.d(LOG_TAG, "emailVerification: Email verified.");
                                mProgressTextView.setText("Pushing to DB");
                                pushToDBandExit(dhishnaUser);
                            } else {
                                Log.d(LOG_TAG, "emailVerification: Email still not verified.");
                                Toast.makeText(EmailLoginActivity.this, "Click the link in your mail.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Log.d(LOG_TAG, "emailVerification: Verification email not sent.");
                }
            }
        };

        /**
         * A listener which listens for when the Firebase account has been created.
         * Verifies email upon successful completion.
         */
        OnCompleteListener<AuthResult> accountCreationListener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    //Firebase account has been successfully created.
                    Log.d(LOG_TAG, "createUserWithEmail:success");
                    firebaseUser = mAuth.getCurrentUser();

                    //Sending the verification Email.
                    mProgressTextView.setText("Sending Verification email.");
                    firebaseUser.sendEmailVerification().addOnCompleteListener(verificationEmailListener);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(LOG_TAG, "createUserWithEmail:failure", task.getException());

                    Toast.makeText(EmailLoginActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        };


        mAuth.createUserWithEmailAndPassword(dhishnaUser.getEmail(), password)
                .addOnCompleteListener(this, accountCreationListener);
        showProgress(true);
        mProgressTextView.setText("Creating account");

    }

    private void pushToDBandExit(DhishnaUser user) {
        FirebaseUser updatedUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("users");
        reference.child(updatedUser.getUid()).setValue(user);
        Log.d(LOG_TAG, "pushed");
        setResult(RESULT_OK);
        finish();
    }


    /**
     * Shows the progress UI and hides the login form.
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

        AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(this, R.drawable.logo_loading_vector);
        mProgressView.setImageDrawable(avd);
        final Animatable animatable = (Animatable) mProgressView.getDrawable();
        animatable.start();
        avd.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                if (show)
                    animatable.start();

                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                mProgressTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with password parameters.
        return password.length() > 6;
    }

}

