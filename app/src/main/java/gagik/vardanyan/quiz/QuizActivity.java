package gagik.vardanyan.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_CORRECT = "extra_correct";
    public static final String EXTRA_DETAILS_JSON = "extra_details_json";
    public static final String EXTRA_SET_RES_ID = "extra_set_res_id";
    public static final String EXTRA_SET_TITLE = "extra_set_title";
    public static final String EXTRA_NUM_QUESTIONS = "extra_num_questions";
    public static final String EXTRA_TIMER_ENABLED = "extra_timer_enabled";
    public static final String EXTRA_TIME_SEC = "extra_time_sec";
    public static final String EXTRA_QUESTIONS_JSON = "extra_questions_json";
    public static final String EXTRA_MIN_TIME_MS = "extra_min_time_ms";

    private TextView tvProgress;
    private TextView tvScore;
    private TextView tvTimer;
    private TextView tvQuestion;
    private LinearProgressIndicator pbTime;
    private MaterialButton[] optionButtons;

    private List<Question> questions;
    private int index = 0;
    private int score = 0;
    private int correctCount = 0;
    private final ArrayList<AnswerDetail> details = new ArrayList<>();
    private final ArrayList<Long> timesTaken = new ArrayList<>();
    private long questionStartTime;
    private int setResId = R.raw.questions;
    private String setTitle = null;
    private int numQuestions = 5;
    private boolean timerEnabled = true;
    private int timeSec = 10;
    private String questionsJsonExtra = null;

    private CountDownTimer timer;
    private long remainingMs;
    private boolean locked = false;
    private ToneGenerator tone;
    private Vibrator vibrator;

    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_quiz);

        soundManager = SoundManager.getInstance(this);

        tvProgress = findViewById(R.id.tvProgress);
        tvScore = findViewById(R.id.tvScore);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestion = findViewById(R.id.tvQuestion);
        pbTime = findViewById(R.id.pbTime);

        optionButtons = new MaterialButton[] {
                findViewById(R.id.btnA),
                findViewById(R.id.btnB),
                findViewById(R.id.btnC),
                findViewById(R.id.btnD)
        };

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        vibrator = getSystemService(Vibrator.class);

        setResId = getIntent().getIntExtra(EXTRA_SET_RES_ID, R.raw.questions);
        setTitle = getIntent().getStringExtra(EXTRA_SET_TITLE);
        numQuestions = getIntent().getIntExtra(EXTRA_NUM_QUESTIONS, 5);
        timerEnabled = getIntent().getBooleanExtra(EXTRA_TIMER_ENABLED, true);
        timeSec = getIntent().getIntExtra(EXTRA_TIME_SEC, 10);
        questionsJsonExtra = getIntent().getStringExtra(EXTRA_QUESTIONS_JSON);

        if (setTitle != null) setTitle(setTitle);

        if (questionsJsonExtra != null && !questionsJsonExtra.trim().isEmpty()) {
            questions = QuizRepository.loadFromJsonString(questionsJsonExtra);
            if (questions.isEmpty()) {
                Toast.makeText(this, "Ошибка: ИИ прислал некорректный формат", Toast.LENGTH_LONG).show();
            }
            questions = QuizRandomizer.shuffleQuestionsAndOptions(questions);
        } else {
            questions = QuizRandomizer.shuffleQuestionsAndOptions(QuizRepository.loadFromRaw(this, setResId));
        }
        
        if (numQuestions > 0 && questions.size() > numQuestions) {
            questions = questions.subList(0, numQuestions);
        }
        
        if (questions.isEmpty()) {
            Toast.makeText(this, "Вопросы не найдены", Toast.LENGTH_SHORT).show();
            finish(); // Просто закрываем, если пусто, вместо перехода к 0 баллов
            return;
        }

        for (int i = 0; i < optionButtons.length; i++) {
            int choice = i;
            optionButtons[i].setOnClickListener(v -> onAnswer(choice));
        }

        findViewById(R.id.btnHint).setOnClickListener(v -> showHint());

        showQuestion();
    }

    private void showHint() {
        if (locked) return;
        
        MaterialButton btnHint = findViewById(R.id.btnHint);
        btnHint.setEnabled(false);
        btnHint.setText("Ищу подсказку...");

        new Thread(() -> {
            try {
                String apiKey = AiQuizService.getApiKey(this);
                Question q = questions.get(index);
                String optionsStr = String.join(", ", q.getOptions());
                String hint = AiQuizService.getHint(apiKey, q.getText(), optionsStr);

                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Подсказка от ИИ 💡")
                            .setMessage(hint)
                            .setPositiveButton("Понятно", null)
                            .setOnDismissListener(d -> {
                                btnHint.setText("Подсказка использована");
                                // We keep it disabled or maybe charge XP/points later
                            })
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnHint.setEnabled(true);
                    btnHint.setText("Нужна подсказка? 💡");
                    Toast.makeText(this, "Ошибка связи с ИИ", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showQuestion() {
        locked = false;
        findViewById(R.id.btnHint).setEnabled(true);
        ((MaterialButton)findViewById(R.id.btnHint)).setText("Нужна подсказка? 💡");

        Question q = questions.get(index);
        questionStartTime = System.currentTimeMillis();

        tvProgress.setText("Вопрос " + (index + 1) + "/" + questions.size());
        tvScore.setText(String.valueOf(score));
        
        // Question fade-in animation
        tvQuestion.setAlpha(0f);
        tvQuestion.setTranslationY(20f);
        tvQuestion.setText(q.getText());
        tvQuestion.animate().alpha(1f).translationY(0f).setDuration(400).start();

        for (int i = 0; i < optionButtons.length; i++) {
            optionButtons[i].setEnabled(true);
            optionButtons[i].setBackgroundTintList(getColorStateList(R.color.option_bg));
            optionButtons[i].setTextColor(getColor(R.color.border_dark));
            optionButtons[i].setStrokeColor(getColorStateList(R.color.track_gray));
            optionButtons[i].setStrokeWidth(3);
            optionButtons[i].setText(q.getOptions().get(i));
            
            // Staggered entry animation for buttons
            optionButtons[i].setAlpha(0f);
            optionButtons[i].setTranslationY(30f);
            optionButtons[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(100 + (i * 50L))
                    .start();
        }

        if (timerEnabled) {
            pbTime.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.VISIBLE);
            startTimer(Math.max(3, timeSec));
        } else {
            stopTimer();
            pbTime.setVisibility(View.GONE);
            tvTimer.setVisibility(View.GONE);
        }
    }

    private void startTimer(int seconds) {
        stopTimer();
        pbTime.setMax(seconds * 1000);
        pbTime.setProgress(seconds * 1000);
        remainingMs = seconds * 1000L;
        tvTimer.setText(seconds + "с");

        timer = new CountDownTimer(seconds * 1000L, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                pbTime.setProgress((int) millisUntilFinished);
                int sec = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvTimer.setText(sec + "с");
                
                if (sec <= 3) {
                    soundManager.playTimer();
                    tvTimer.setTextColor(getColor(R.color.wrong_bg));
                    tvTimer.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                            .withEndAction(() -> tvTimer.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                            .start();
                } else {
                    tvTimer.setTextColor(getColor(R.color.brand_blue));
                }
            }

            @Override
            public void onFinish() {
                remainingMs = 0;
                pbTime.setProgress(0);
                tvTimer.setText("0с");
                onTimeout();
            }
        }.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void onAnswer(int choice) {
        if (locked) return;
        locked = true;
        stopTimer();
        long timeTaken = System.currentTimeMillis() - questionStartTime;
        timesTaken.add(timeTaken);

        Question q = questions.get(index);
        int correct = q.getCorrectIndex();

        for (MaterialButton b : optionButtons) b.setEnabled(false);

        boolean isCorrect = choice == correct;
        if (isCorrect) {
            correctCount++;
            int timeBonus = timerEnabled ? (int) (remainingMs / 1000L) * 50 : 0;
            score += 1000 + timeBonus;
            soundManager.playCorrect();
        } else {
            soundManager.playWrong();
        }
        ProgressionManager.updateStreak(this, isCorrect);

        details.add(new AnswerDetail(q.getText(), q.getOptions(), correct, choice, false));

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("haptics_enabled", true)) {
            playFeedback(isCorrect);
        }

        optionButtons[correct].setBackgroundTintList(getColorStateList(R.color.correct_bg));
        optionButtons[correct].setTextColor(getColor(android.R.color.white));
        optionButtons[correct].setStrokeColor(getColorStateList(R.color.correct_bg));

        if (!isCorrect) {
            optionButtons[choice].setBackgroundTintList(getColorStateList(R.color.wrong_bg));
            optionButtons[choice].setTextColor(getColor(android.R.color.white));
            optionButtons[choice].setStrokeColor(getColorStateList(R.color.wrong_bg));
        }

        animateChoice(optionButtons[choice]);
        if (!isCorrect) animateChoice(optionButtons[correct]);

        new Handler(Looper.getMainLooper()).postDelayed(this::next, 650);
    }

    private void onTimeout() {
        if (locked) return;
        locked = true;
        timesTaken.add((long) timeSec * 1000);

        Question q = questions.get(index);
        int correct = q.getCorrectIndex();

        for (MaterialButton b : optionButtons) b.setEnabled(false);
        optionButtons[correct].setBackgroundTintList(getColorStateList(R.color.correct_bg));
        optionButtons[correct].setTextColor(getColor(android.R.color.white));
        optionButtons[correct].setStrokeColor(getColorStateList(R.color.correct_bg));

        details.add(new AnswerDetail(q.getText(), q.getOptions(), correct, -1, true));

        playTimeoutFeedback();
        animateChoice(optionButtons[correct]);

        new Handler(Looper.getMainLooper()).postDelayed(this::next, 650);
    }

    private void playFeedback(boolean correct) {
        try {
            if (tone != null) {
                tone.startTone(correct ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 140);
            }
        } catch (Exception ignored) {
        }

        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                VibrationEffect effect = VibrationEffect.createOneShot(correct ? 45 : 90, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            }
        } catch (Exception ignored) {
        }
    }

    private void playTimeoutFeedback() {
        try {
            if (tone != null) {
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 180);
            }
        } catch (Exception ignored) {
        }

        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = new long[] {0, 60, 50, 60};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
        } catch (Exception ignored) {
        }
    }

    private void animateChoice(View v) {
        if (v == null) return;
        v.animate()
                .scaleX(0.98f).scaleY(0.98f)
                .setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    private void next() {
        index++;
        if (index >= questions.size()) {
            finishToResult();
        } else {
            showQuestion();
        }
    }

    private void finishToResult() {
        stopTimer();
        long minTime = Long.MAX_VALUE;
        for (long t : timesTaken) if (t < minTime) minTime = t;
        if (timesTaken.isEmpty()) minTime = 0;

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(EXTRA_SCORE, score);
        intent.putExtra(EXTRA_TOTAL, questions == null ? 0 : questions.size());
        intent.putExtra(EXTRA_CORRECT, correctCount);
        intent.putExtra(EXTRA_DETAILS_JSON, QuizResultCodec.encode(details));
        intent.putExtra(EXTRA_SET_RES_ID, setResId);
        intent.putExtra(EXTRA_SET_TITLE, setTitle);
        intent.putExtra(EXTRA_NUM_QUESTIONS, numQuestions);
        intent.putExtra(EXTRA_TIMER_ENABLED, timerEnabled);
        intent.putExtra(EXTRA_TIME_SEC, timeSec);
        intent.putExtra(EXTRA_QUESTIONS_JSON, questionsJsonExtra);
        intent.putExtra(EXTRA_MIN_TIME_MS, minTime);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        if (tone != null) {
            tone.release();
            tone = null;
        }
        super.onDestroy();
    }
}

