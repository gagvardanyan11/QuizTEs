package gagik.vardanyan.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupToggles();
        setupThemeButtons();

        findViewById(R.id.btnClearCache).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Очистить кэш?")
                    .setMessage("Все сохраненные через ИИ квизы будут удалены.")
                    .setPositiveButton("Удалить", (d, w) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(this).quizDao().clearCache();
                            runOnUiThread(() -> Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show());
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        findViewById(R.id.btnChangeApiKey).setOnClickListener(v -> {
            showChangeApiKeyDialog();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Выход")
                    .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                    .setPositiveButton("Выйти", (d, w) -> {
                        prefs.edit().putBoolean("is_logged_in", false).apply();
                        Intent intent = new Intent(this, AuthActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void showChangeApiKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить API Ключ");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setHint("Введите ваш Gemini API Key");
        
        String currentKey = prefs.getString("custom_api_key", "");
        input.setText(currentKey);

        builder.setView(viewInflated);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newKey = input.getText().toString().trim();
            prefs.edit().putString("custom_api_key", newKey).apply();
            Toast.makeText(this, "Ключ обновлен", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.setNeutralButton("Сбросить", (dialog, which) -> {
            prefs.edit().remove("custom_api_key").apply();
            Toast.makeText(this, "Используется ключ по умолчанию", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void setupToggles() {
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        if (switchDarkMode != null) {
            boolean isDark = prefs.getBoolean("dark_mode", false);
            switchDarkMode.setChecked(isDark);
            switchDarkMode.setOnCheckedChangeListener((v, checked) -> {
                prefs.edit().putBoolean("dark_mode", checked).apply();
                AppCompatDelegate.setDefaultNightMode(checked ? 
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        SwitchMaterial switchHaptics = findViewById(R.id.switchHaptics);
        if (switchHaptics != null) {
            switchHaptics.setChecked(prefs.getBoolean("haptics_enabled", true));
            switchHaptics.setOnCheckedChangeListener((v, checked) -> {
                prefs.edit().putBoolean("haptics_enabled", checked).apply();
            });
        }
    }

    private void setupThemeButtons() {
        findViewById(R.id.btnThemeDefault).setOnClickListener(v -> updateTheme(ThemeManager.Theme.DEFAULT));
        findViewById(R.id.btnThemeNeon).setOnClickListener(v -> updateTheme(ThemeManager.Theme.NEON));
        findViewById(R.id.btnThemeOcean).setOnClickListener(v -> updateTheme(ThemeManager.Theme.OCEAN));
    }

    private void updateTheme(ThemeManager.Theme theme) {
        ThemeManager.setTheme(this, theme);
        Toast.makeText(this, "Тема применена!", Toast.LENGTH_SHORT).show();
        recreate();
    }
}
