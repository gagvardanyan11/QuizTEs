package gagik.vardanyan.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnStart;
    private TextInputEditText etTopic;
    private MaterialButtonToggleGroup tgMode;
    private MaterialButtonToggleGroup tgDifficulty;
    private MaterialButton btnSolo;
    private MaterialButton btnMulti;
    private MaterialButton btnEasy, btnMedium, btnHard;
    private CircularProgressIndicator progressAi;
    private TextView tvLevel, tvStreak, tvTitle;
    private ImageView ivAvatar;
    private com.google.android.material.progressindicator.LinearProgressIndicator pbExperience;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int SPEECH_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {




    `

        `
        .ThemeManager.applyTheme(this);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDark ? 
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : 
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        etTopic = findViewById(R.id.etTopic);
        tgMode = findViewById(R.id.tgMode);
        btnSolo = findViewById(R.id.btnSolo);
        btnMulti = findViewById(R.id.btnMulti);
        tgDifficulty = findViewById(R.id.tgDifficulty);
        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnHard = findViewById(R.id.btnHard);
        progressAi = findViewById(R.id.progressAi);
        tvLevel = findViewById(R.id.tvLevel);
        tvStreak = findViewById(R.id.tvStreak);
        tvTitle = findViewById(R.id.tvTitle);
        pbExperience = findViewById(R.id.pbExperience);

        findViewById(R.id.cardProgression).setOnClickListener(v -> {
            showProgressionMenu();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        com.google.android.material.textfield.TextInputLayout til = findViewById(R.id.tilTopic);
        til.setEndIconOnClickListener(v -> startSpeechToText());

        ivAvatar = findViewById(R.id.ivAvatar);
        ivAvatar.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
        
        findViewById(R.id.tvTitle).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        updateProgressUI();

        tgMode.check(R.id.btnSolo);
        tgMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            
            if (checkedId == R.id.btnSolo) {
                btnSolo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_purple)));
                btnSolo.setTextColor(Color.WHITE);
                btnMulti.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                btnMulti.setTextColor(ContextCompat.getColor(this, R.color.muted_gray));
            } else {
                startActivity(new Intent(this, LobbyActivity.class));
                group.check(R.id.btnSolo);
            }
        });

        setupDifficultyToggle();

        refreshCategories();

        findViewById(R.id.chipAdd).setOnClickListener(v -> showAddCategoryDialog());

        btnStart.setOnClickListener(v -> {
            String topicText = etTopic.getText() == null ? "" : etTopic.getText().toString().trim();
            if (topicText.isEmpty()) topicText = "Java";

            int diff = 0;
            int questionCount = 5;
            int timeSec = 25;

            if (tgDifficulty.getCheckedButtonId() == R.id.btnMedium) {
                diff = 1;
                questionCount = 10;
                timeSec = 20;
            } else if (tgDifficulty.getCheckedButtonId() == R.id.btnHard) {
                diff = 2;
                questionCount = 15;
                timeSec = 15;
            }

            boolean timerEnabled = true;

            String apiKey = AiQuizService.getApiKey(this);
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Добавьте API ключ в настройках", Toast.LENGTH_LONG).show();
                startOffline(topicText, diff, questionCount, timerEnabled, timeSec);
                return;
            }

            startQuizSequence(topicText, diff, questionCount, timeSec, "Normal");
        });
    }

    private void startQuizSequence(String topic, int diff, int questionCount, int timeSec, String style) {
        setLoading(true);
        executor.execute(() -> {
            try {
                CachedQuiz cached = AppDatabase.getInstance(this).quizDao().getCachedQuiz(topic);

                if (cached != null) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        launchQuiz(topic, questionCount, true, timeSec, cached.jsonContent);
                    });
                    return;
                }

                String apiKey = AiQuizService.getApiKey(this);
                if (apiKey.isEmpty()) {
                    throw new Exception("API ключ не задан. Перейдите в настройки.");
                }
                
                String quizJson = AiQuizService.generateQuizJson(apiKey, topic, questionCount, diff, timeSec, style);

                AppDatabase.getInstance(this).quizDao().insertCachedQuiz(
                        new CachedQuiz(topic, quizJson, System.currentTimeMillis())
                );

                runOnUiThread(() -> {
                    setLoading(false);
                    launchQuiz(topic, questionCount, true, timeSec, quizJson);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                    if (errorMsg.contains("429")) {
                        Toast.makeText(this, "Лимит AI исчерпан. Переходим в оффлайн-режим", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Ошибка ИИ: " + mapAiError(e), Toast.LENGTH_LONG).show();
                    }
                    startOffline(topic, diff, questionCount, true, timeSec);
                });
            }
        });
    }

    private void showProgressionMenu() {
        String[] options = {"Статистика и отчет ИИ", "Глобальный рейтинг"};
        new AlertDialog.Builder(this)
                .setTitle("Прогресс")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, StatisticsActivity.class));
                    else if (which == 1) startActivity(new Intent(this, LeaderboardActivity.class));
                })
                .show();
    }


    private void refreshCategories() {
        android.widget.GridLayout group = findViewById(R.id.categoryGrid);
        if (group == null) return;

        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child.getId() != R.id.chipAdd) group.removeViewAt(i);
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> deleted = prefs.getStringSet("deleted_cats", new HashSet<>());

        addDefaultCategory("Космос", "#3B82F6", R.drawable.ic_rocket, deleted);
        addDefaultCategory("Java", "#F97316", R.drawable.ic_code, deleted);
        addDefaultCategory("Технологии", "#8B5CF6", R.drawable.ic_laptop, deleted);
        addDefaultCategory("Еда", "#EF4444", R.drawable.ic_pizza, deleted);
        addDefaultCategory("Кино", "#EC4899", R.drawable.ic_film, deleted);
        addDefaultCategory("География", "#10B981", R.drawable.ic_globe, deleted);

        loadCustomCategories();
    }

    private void addDefaultCategory(String name, String color, int iconRes, Set<String> deleted) {
        if (!deleted.contains(name)) {
            addCategoryToUi(name, Color.parseColor(color), iconRes, false);
        }
    }

    private void setupDifficultyToggle() {
        tgDifficulty.check(R.id.btnEasy);
        updateDifficultyUI(R.id.btnEasy);

        tgDifficulty.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateDifficultyUI(checkedId);
        });
    }

    private void updateDifficultyUI(int checkedId) {
        resetDiffBtn(btnEasy);
        resetDiffBtn(btnMedium);
        resetDiffBtn(btnHard);

        if (checkedId == R.id.btnEasy) {
            applyDiffStyle(btnEasy, R.color.difficulty_easy);
        } else if (checkedId == R.id.btnMedium) {
            applyDiffStyle(btnMedium, R.color.difficulty_medium);
        } else if (checkedId == R.id.btnHard) {
            applyDiffStyle(btnHard, R.color.difficulty_hard);
        }
    }

    private void resetDiffBtn(MaterialButton btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btn.setTextColor(ContextCompat.getColor(this, R.color.muted_gray));
    }

    private void applyDiffStyle(MaterialButton btn, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        btn.setBackgroundTintList(ColorStateList.valueOf(color));
        btn.setTextColor(Color.WHITE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProgressUI();
        refreshAdvisorInsight();
    }

    private void refreshAdvisorInsight() {
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        if (tvSubtitle == null) return;

        ProgressionManager.Progress progress = ProgressionManager.getProgress(this);
        String tip = new AiQuizService().getQuickTip(progress.level, progress.maxStreak);
        tvSubtitle.setText(tip);
    }

    private void updateProgressUI() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "QuizMaster");
        int avatarRes = prefs.getInt("user_avatar", R.drawable.ic_avatar_1);
        
        if (tvTitle != null) tvTitle.setText(name);
        
        if (ivAvatar != null) {
            ivAvatar.setImageResource(avatarRes);
        }

        ProgressionManager.Progress progress = ProgressionManager.getProgress(this);
        if (tvLevel != null) tvLevel.setText("Уровень " + progress.level);
        if (tvStreak != null) tvStreak.setText("Рекорд: " + progress.maxStreak + " 🔥");
        if (pbExperience != null) {
            pbExperience.setProgress((int) (progress.percent * 100));
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 0);

        final EditText input = new EditText(this);
        input.setHint("Тема (напр. 'Древний Рим')");
        layout.addView(input);

        builder.setTitle("Новая категория")
                .setView(layout)
                .setPositiveButton("Создать с ИИ ✨", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;

                    String apiKey = AiQuizService.getApiKey(this);
                    setLoading(true);
                    executor.execute(() -> {
                        try {
                            String styleJson = AiQuizService.suggestCategoryStyle(apiKey, name);
                            if (styleJson.contains("```")) {
                                styleJson = styleJson.replaceAll("(?s)```(?:json)?\\s*(.*?)\\s*```", "$1").trim();
                            }
                            JSONObject obj = new JSONObject(styleJson);
                            String colorHex = obj.optString("color", "#6366F1");
                            String iconName = obj.optString("icon", "ic_book");

                            runOnUiThread(() -> {
                                setLoading(false);
                                try {
                                    int color = Color.parseColor(colorHex);
                                    
                                    String cleanedIcon = iconName.replace(".xml", "").replace("ic_", "");
                                    int iconRes = getResources().getIdentifier("ic_" + cleanedIcon, "drawable", getPackageName());
                                    if (iconRes == 0) iconRes = R.drawable.ic_book;

                                    addCategoryToUi(name, color, iconRes, true);
                                    saveCustomCategory(name, colorHex, "ic_" + cleanedIcon);
                                } catch (Exception e) {
                                    addCategoryToUi(name, Color.parseColor("#6366F1"), R.drawable.ic_book, true);
                                    saveCustomCategory(name, "#6366F1", "ic_book");
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                                Toast.makeText(this, "Ошибка ИИ: " + errorMsg, Toast.LENGTH_LONG).show();
                                addCategoryToUi(name, Color.parseColor("#6366F1"), R.drawable.ic_book, true);
                                saveCustomCategory(name, "#6366F1", "ic_book");
                            });
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addCategoryToUi(String name, int color, int iconRes, boolean isCustom) {
        android.widget.GridLayout group = findViewById(R.id.categoryGrid);
        if (group == null) return;
        
        View catView = getLayoutInflater().inflate(R.layout.item_category, group, false);
        
        android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
        params.width = 0;
        params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        params.setMargins(12, 12, 12, 12);
        catView.setLayoutParams(params);

        com.google.android.material.card.MaterialCardView iconContainer = catView.findViewById(R.id.iconContainer);
        ImageView ivIcon = catView.findViewById(R.id.ivIcon);
        TextView tvName = catView.findViewById(R.id.tvName);
        
        tvName.setText(name);
        ivIcon.setImageResource(iconRes);
        if (iconContainer != null) {
            iconContainer.setCardBackgroundColor(ColorStateList.valueOf(color));
        }
        
        catView.setOnClickListener(v -> etTopic.setText(name));

        catView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Удалить категорию?")
                    .setMessage("Вы хотите удалить тему '" + name + "'?")
                    .setPositiveButton("Удалить", (d, w) -> {
                        group.removeView(catView);
                        if (isCustom) deleteCustomCategory(name);
                        else markDefaultAsDeleted(name);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return true;
        });
        
        int index = group.getChildCount();
        View chipAdd = findViewById(R.id.chipAdd);
        if (chipAdd != null) index = group.indexOfChild(chipAdd);
        group.addView(catView, Math.max(0, index));
    }

    private void markDefaultAsDeleted(String name) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> deleted = new HashSet<>(prefs.getStringSet("deleted_cats", new HashSet<>()));
        deleted.add(name);
        prefs.edit().putStringSet("deleted_cats", deleted).apply();
    }

    private void saveCustomCategory(String name, String colorHex, String iconName) {
        try {
            SharedPreferences prefs = getSharedPreferences("custom_cats", MODE_PRIVATE);
            String json = prefs.getString("list", "[]");
            JSONArray arr = new JSONArray(json);
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("color", colorHex);
            obj.put("iconName", iconName);
            arr.put(obj);
            prefs.edit().putString("list", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void deleteCustomCategory(String name) {
        try {
            SharedPreferences prefs = getSharedPreferences("custom_cats", MODE_PRIVATE);
            String json = prefs.getString("list", "[]");
            JSONArray oldArr = new JSONArray(json);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < oldArr.length(); i++) {
                JSONObject obj = oldArr.getJSONObject(i);
                if (!obj.getString("name").equals(name)) newArr.put(obj);
            }
            prefs.edit().putString("list", newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadCustomCategories() {
        try {
            SharedPreferences prefs = getSharedPreferences("custom_cats", MODE_PRIVATE);
            String json = prefs.getString("list", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String iconName = obj.optString("iconName", "ic_book");
                int resId = getResources().getIdentifier(iconName, "drawable", getPackageName());
                if (resId == 0) resId = R.drawable.ic_book;
                addCategoryToUi(obj.getString("name"), Color.parseColor(obj.getString("color")), resId, true);
            }
        } catch (Exception ignored) {}
    }

    private void startOffline(String topic, int diff, int questionCount, boolean timerEnabled, int timeSec) {
        int setResId = QuestionSets.resolveRawResId(topic, diff);
        Intent i = new Intent(this, QuizActivity.class);
        i.putExtra(QuizActivity.EXTRA_SET_RES_ID, setResId);
        i.putExtra(QuizActivity.EXTRA_SET_TITLE, topic);
        i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, questionCount);
        i.putExtra(QuizActivity.EXTRA_TIMER_ENABLED, timerEnabled);
        i.putExtra(QuizActivity.EXTRA_TIME_SEC, timeSec);
        startActivity(i);
    }

    private void launchQuiz(String topic, int count, boolean timer, int time, String json) {
        Intent i = new Intent(this, QuizActivity.class);
        i.putExtra(QuizActivity.EXTRA_SET_TITLE, topic);
        i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, count);
        i.putExtra(QuizActivity.EXTRA_TIMER_ENABLED, timer);
        i.putExtra(QuizActivity.EXTRA_TIME_SEC, time);
        i.putExtra(QuizActivity.EXTRA_QUESTIONS_JSON, json);
        startActivity(i);
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressAi.show();
            btnStart.setEnabled(false);
            btnStart.setText("");
            btnStart.setIcon(null);
        } else {
            progressAi.hide();
            btnStart.setEnabled(true);
            btnStart.setText("ИГРАТЬ");
            btnStart.setIconResource(android.R.drawable.ic_media_play);
        }
    }

    private String mapAiError(Exception e) {
        String raw = e == null ? "Unknown" : String.valueOf(e.getMessage());
        String lower = raw.toLowerCase();
        
        if (raw.contains("HTTP")) return raw;
        
        if (lower.contains("429") || lower.contains("quota")) return "Gemini: квота исчерпана (429).";
        if (lower.contains("host") || lower.contains("connect")) return "Нет сети. Проверьте интернет.";
        if (lower.contains("403")) return "Ошибка 403: Неверный API ключ или доступ запрещен.";
        
        return "Детали: " + raw;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void startSpeechToText() {
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Назовите тему квиза");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Голосовой ввод не поддерживается", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> results = data.getStringArrayListExtra(
                    android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                etTopic.setText(results.get(0));
            }
        }
    }
}
