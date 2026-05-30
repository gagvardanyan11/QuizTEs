package gagik.vardanyan.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private static final String WEB_CLIENT_ID = "646338753457-n0l9te1ud5gnb3kbvujtk4bg4qvaqbb7.apps.googleusercontent.com";
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnAuth, btnGoogle, btnShowEmailFields, btnGuest;
    private LinearLayout layoutEmailFields;
    private TextView tvSwitchAuth;
    private boolean isLoginMode = true;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_auth);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnAuth = findViewById(R.id.btnAuth);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnShowEmailFields = findViewById(R.id.btnShowEmailFields);
        btnGuest = findViewById(R.id.btnGuest);
        layoutEmailFields = findViewById(R.id.layoutEmailFields);
        tvSwitchAuth = findViewById(R.id.tvSwitchAuth);

        btnShowEmailFields.setOnClickListener(v -> {
            if (layoutEmailFields.getVisibility() == View.VISIBLE) {
                layoutEmailFields.setVisibility(View.GONE);
            } else {
                layoutEmailFields.setVisibility(View.VISIBLE);
            }
        });

        btnGoogle.setOnClickListener(v -> {
            signInWithGoogle();
        });

        btnGuest.setOnClickListener(v -> {
            loginAsGuest();
        });

        tvSwitchAuth.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUi();
        });

        btnAuth.setOnClickListener(v -> handleAuth());
    }

    private void signInWithGoogle() {
        CredentialManager credentialManager = CredentialManager.create(this);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, executor, new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleSignInResult(result.getCredential());
            }

            @Override
            public void onError(GetCredentialException e) {
                runOnUiThread(() -> {
                    String errorMsg = e.getMessage();
                    if (e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {
                        errorMsg = "Вход отменен";
                    } else if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
                        errorMsg = "Аккаунты не найдены. Проверьте SHA-1 и Client ID.";
                    }
                    Toast.makeText(AuthActivity.this, "Ошибка Google: " + errorMsg, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        });
    }

    private void handleSignInResult(Credential credential) {
        if (credential instanceof GoogleIdTokenCredential) {
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
            String idToken = googleIdTokenCredential.getIdToken();
            String displayName = googleIdTokenCredential.getDisplayName();

            // Firebase Auth
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Use displayName or email as username
                        String username = displayName != null ? displayName : "Google User";

                        executor.execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            User existing = db.userDao().getUserByUsername(username);
                            if (existing == null) {
                                User newUser = new User(username, "google_auth", R.drawable.ic_avatar_1);
                                db.userDao().registerUser(newUser);
                                existing = db.userDao().login(username, "google_auth");
                            }
                            final User finalUser = existing;
                            runOnUiThread(() -> saveSession(finalUser));
                        });
                    } else {
                        Toast.makeText(AuthActivity.this, "Ошибка Firebase: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void updateUi() {
        if (isLoginMode) {
            btnAuth.setText(R.string.auth_btn_login);
            tvSwitchAuth.setText(R.string.auth_no_account);
        } else {
            btnAuth.setText(R.string.auth_btn_register);
            tvSwitchAuth.setText(R.string.auth_have_account);
        }
    }

    private void loginAsGuest() {
        User guestUser = new User("Гость", "", R.drawable.ic_avatar_1);
        guestUser.id = -1; // Special ID for guest
        saveSession(guestUser);
    }

    private void handleAuth() {
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, getString(R.string.auth_err_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (isLoginMode) {
                User authenticatedUser = db.userDao().login(user, pass);
                runOnUiThread(() -> {
                    if (authenticatedUser != null) {
                        saveSession(authenticatedUser);
                    } else {
                        Toast.makeText(this, getString(R.string.auth_err_wrong), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                User existing = db.userDao().getUserByUsername(user);
                if (existing != null) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.auth_err_exists), Toast.LENGTH_SHORT).show());
                } else {
                    User newUser = new User(user, pass, R.drawable.ic_avatar_1);
                    db.userDao().registerUser(newUser);
                    User registered = db.userDao().login(user, pass);
                    runOnUiThread(() -> saveSession(registered));
                }
            }
        });
    }

    private void saveSession(User user) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_logged_in", true)
                .putInt("user_id", user.id)
                .putString("user_name", user.username)
                .putInt("user_avatar", user.avatarRes)
                .apply();
        
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
