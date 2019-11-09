package edu.illinois.cs.cs125.fall2019.mp;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.firebase.ui.auth.AuthUI;

import java.util.Arrays;
import java.util.List;

/**
 * code.
 */
public class LaunchActivity extends AppCompatActivity {
    /**
     * code.
     */
    private static final int RC_SIGN_IN = 123;

    /**
     * javadoc.
     * @param savedInstanceState - saved
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // startActivity
            Intent myintent = new Intent(LaunchActivity.this, MainActivity.class);
            LaunchActivity.this.startActivity(myintent);
            finish();
        } else {
            createSignInIntent();
            Button login = findViewById(R.id.goLogin);
            login.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    createSignInIntent();
                }
                });
        }
    }

    /**
     * code.
     */
    public void createSignInIntent() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build());
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().
                setAvailableProviders(providers).build(), RC_SIGN_IN);
    }

    /**
     * @param requestCode code.
     * @param resultCode code
     * @param data data
     */
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    // startActivity
                    Intent myintent = new Intent(LaunchActivity.this, MainActivity.class);
                    LaunchActivity.this.startActivity(myintent);
                    finish();
                }
            }
        }
    }
}
