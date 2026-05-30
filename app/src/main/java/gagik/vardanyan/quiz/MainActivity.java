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
    private TextView tvLevel, tvStreak, tvDailyTopic, tvTitle, tvAdvisorInsight;
    private View cardDaily, cardAdvisor;
    private LinearLayout llTrendingContainer;
    private ImageView ivAvatar;
    private com.google.android.material.progressindicator.LinearProgressIndicator pbExperience;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String[] DAILY_TOPICS = {
            "История Древнего Рима", "Великие открытия", "Мировая кухня",
            "Космические миссии", "Классическая музыка", "Мифология",
            "Чудеса света", "Животные океана", "Кино 90-х"
    };

    private static final int SPEECH_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme from settings before super.onCreate
        ThemeManager.applyTheme(this);

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
        pbExperience = findViewById(R.id.pbExperience);
        cardDaily = findViewById(R.id.cardDaily);
        tvDailyTopic = findViewById(R.id.tvDailyTopic);
        cardAdvisor = findViewById(R.id.cardAdvisor);
        tvAdvisorInsight = findViewById(R.id.tvAdvisorInsight);
        llTrendingContainer = findViewById(R.id.llTrendingContainer);

        setupDailyChallenge();
        setupAiAdvisor();
        loadTrendingTopics();

        findViewById(R.id.cardProgression).setOnClickListener(v -> {
            showProgressionMenu();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.btnKnowledge).setOnClickListener(v -> {
            startActivity(new Intent(this, KnowledgeBaseActivity.class));
        });

        findViewById(R.id.cardDaily).setOnLongClickListener(v -> {
            showQuestsDialog();
            return true;
        });

        com.google.android.material.textfield.TextInputLayout til = findViewById(R.id.tilTopic);
        til.setEndIconOnClickListener(v -> startSpeechToText());

        findViewById(R.id.ivAvatar).setOnClickListener(v -> {
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
                // 1. Проверяем кэш в базе
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
                
                // Note: Updated generateQuizJson signature
                String quizJson = AiQuizService.generateQuizJson(apiKey, topic, questionCount, diff, timeSec, style);

                // 2. Сохраняем в кэш
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

    private void loadTrendingTopics() {
        executor.execute(() -> {
            try {
                String apiKey = AiQuizService.getApiKey(this);
                String topics = AiQuizService.getTrendingTopics(apiKey);
                String[] split = topics.split(",");
                
                runOnUiThread(() -> {
                    llTrendingContainer.removeAllViews();
                    for (String t : split) {
                        String topic = t.trim();
                        if (topic.isEmpty()) continue;
                        
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                        chip.setText(topic);
                        chip.setChipIconResource(android.R.drawable.ic_menu_search);
                        chip.setOnClickListener(v -> etTopic.setText(topic));
                        
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        lp.setMarginEnd(12);
                        llTrendingContainer.addView(chip, lp);
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private void setupAiAdvisor() {
        cardAdvisor.setVisibility(View.VISIBLE);
        refreshAdvisorInsight();
        findViewById(R.id.btnRefreshAdvisor).setOnClickListener(v -> refreshAdvisorInsight());
    }

    private void refreshAdvisorInsight() {
        tvAdvisorInsight.setText("AI анализирует ваши успехи...");
        executor.execute(() -> {
            try {
                AiQuizService aiService = new AiQuizService();
                ProgressionManager.Progress progress = ProgressionManager.getProgress(this);
                String tip = aiService.getQuickTip(progress.level, progress.maxStreak);
                runOnUiThread(() -> tvAdvisorInsight.setText(tip));
            } catch (Exception e) {
                runOnUiThread(() -> tvAdvisorInsight.setText("Знание — сила! Продолжайте учиться."));
            }
        });
    }

    private void showProgressionMenu() {
        String[] options = {"Статистика и отчет ИИ", "Глобальный рейтинг", "Ежедневные задания"};
        new AlertDialog.Builder(this)
                .setTitle("Прогресс")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, StatisticsActivity.class));
                    else if (which == 1) startActivity(new Intent(this, LeaderboardActivity.class));
                    else if (which == 2) showQuestsDialog();
                })
                .show();
    }

    private void showQuestsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quests, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        LinearLayout container = dialogView.findViewById(R.id.llQuestsContainer);
        List<QuestManager.Quest> quests = QuestManager.getDailyQuests(this);

        for (QuestManager.Quest q : quests) {
            View itemView = getLayoutInflater().inflate(R.layout.item_quest, container, false);
            ((TextView) itemView.findViewById(R.id.tvQuestTitle)).setText(q.title);
            ((TextView) itemView.findViewById(R.id.tvQuestReward)).setText("+" + q.rewardXp + " XP");
            
            com.google.android.material.progressindicator.LinearProgressIndicator pb = itemView.findViewById(R.id.pbQuest);
            pb.setMax(q.goal);
            pb.setProgress(q.progress);

            Button btnClaim = itemView.findViewById(R.id.btnClaim);
            ImageView ivCheck = itemView.findViewById(R.id.ivCheck);

            if (q.rewardClaimed) {
                ivCheck.setVisibility(View.VISIBLE);
                btnClaim.setVisibility(View.GONE);
            } else if (q.completed) {
                btnClaim.setVisibility(View.VISIBLE);
                ivCheck.setVisibility(View.GONE);
                btnClaim.setOnClickListener(v -> {
                    QuestManager.claimReward(this, q);
                    updateProgressUI();
                    dialog.dismiss();
                    showQuestsDialog();
                });
            }

            container.addView(itemView);
        }

        dialogView.findViewById(R.id.btnCloseQuests).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupDailyChallenge() {
        // Выбираем тему на основе дня
        long dayIndex = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        String dailyTopic = DAILY_TOPICS[(int) (dayIndex % DAILY_TOPICS.length)];
        tvDailyTopic.setText(dailyTopic);

        cardDaily.setOnClickListener(v -> {
            startDailyQuiz(dailyTopic);
        });
    }

    private void startDailyQuiz(String topic) {
        String apiKey = AiQuizService.getApiKey(this);
        setLoading(true);

        executor.execute(() -> {
            try {
                // Для ежедневного вызова всегда пытаемся получить свежий квиз или из кэша
                CachedQuiz cached = AppDatabase.getInstance(this).quizDao().getCachedQuiz("DAILY_" + topic);
                String json;
                if (cached != null) {
                    json = cached.jsonContent;
                } else {
                    json = AiQuizService.generateQuizJson(apiKey, topic, 10, 1, 15, null); // 10 вопросов, средний уровень
                    AppDatabase.getInstance(this).quizDao().insertCachedQuiz(new CachedQuiz("DAILY_" + topic, json, System.currentTimeMillis()));
                }

                runOnUiThread(() -> {
                    setLoading(false);
                    Intent i = new Intent(this, QuizActivity.class);
                    i.putExtra(QuizActivity.EXTRA_SET_TITLE, topic);
                    i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, 10);
                    i.putExtra(QuizActivity.EXTRA_TIMER_ENABLED, true);
                    i.putExtra(QuizActivity.EXTRA_TIME_SEC, 15);
                    i.putExtra(QuizActivity.EXTRA_QUESTIONS_JSON, json);
                    i.putExtra("isDaily", true);
                    startActivity(i);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Не удалось загрузить вызов дня", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void refreshCategories() {
        android.widget.GridLayout group = findViewById(R.id.categoryGrid);
        if (group == null) return;

        // Clear existing categories (keep chipAdd)
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child.getId() != R.id.chipAdd) group.removeViewAt(i);
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> deleted = prefs.getStringSet("deleted_cats", new HashSet<>());

        // Default Categories
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

    private void updateProgressUI() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "QuizMaster");
        int avatarRes = prefs.getInt("user_avatar", R.drawable.ic_avatar_1);
        
        if (tvTitle != null) tvTitle.setText(name);
        
        ImageView ivAvatar = findViewById(R.id.ivAvatar);
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
                            // If Gemini returns markdown-wrapped JSON, clean it
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
                                    
                                    // Умный поиск ресурса иконки
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
        
        // Если это HTTP ошибка, покажем её код
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
