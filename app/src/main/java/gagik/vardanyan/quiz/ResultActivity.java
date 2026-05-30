package gagik.vardanyan.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.airbnb.lottie.LottieAnimationView;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_result);

        int score = getIntent().getIntExtra(QuizActivity.EXTRA_SCORE, 0);
        int total = getIntent().getIntExtra(QuizActivity.EXTRA_TOTAL, 0);
        int correct = getIntent().getIntExtra(QuizActivity.EXTRA_CORRECT, 0);
        String detailsJson = getIntent().getStringExtra(QuizActivity.EXTRA_DETAILS_JSON);
        List<AnswerDetail> details = QuizResultCodec.decode(detailsJson);
        int setResId = getIntent().getIntExtra(QuizActivity.EXTRA_SET_RES_ID, R.raw.questions);
        String setTitle = getIntent().getStringExtra(QuizActivity.EXTRA_SET_TITLE);
        int numQuestions = getIntent().getIntExtra(QuizActivity.EXTRA_NUM_QUESTIONS, 5);
        boolean timerEnabled = getIntent().getBooleanExtra(QuizActivity.EXTRA_TIMER_ENABLED, true);
        int timeSec = getIntent().getIntExtra(QuizActivity.EXTRA_TIME_SEC, 10);
        String questionsJson = getIntent().getStringExtra(QuizActivity.EXTRA_QUESTIONS_JSON);

        TextView tvScore = findViewById(R.id.tvFinalScore);
        TextView tvCorrectCount = findViewById(R.id.tvCorrectCount);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvAccuracy = findViewById(R.id.tvAccuracy);
        RecyclerView rvDetails = findViewById(R.id.rvDetails);
        MaterialButton btnRestart = findViewById(R.id.btnRestart);
        MaterialButton btnHome = findViewById(R.id.btnHome);

        tvScore.setText(String.valueOf(score));
        tvCorrectCount.setText(String.valueOf(correct));
        tvSummary.setText(String.valueOf(total));
        int accuracy = total > 0 ? (correct * 100 / total) : 0;
        tvAccuracy.setText(accuracy + "%");

        if (accuracy == 100) {
            LottieAnimationView confetti = findViewById(R.id.lottieConfetti);
            if (confetti != null) {
                confetti.setVisibility(android.view.View.VISIBLE);
                confetti.playAnimation();
            }
        }

        // Save to Database
        final String topic = setTitle != null ? setTitle : "Общая викторина";
        final int finalScore = score;
        final int finalTotal = total;
        new Thread(() -> {
            AppDatabase.getInstance(this).quizDao().insert(
                    new QuizResult(topic, finalScore, finalTotal, System.currentTimeMillis())
            );
        }).start();

        // Progression: Earn XP (100 base + 50 per correct answer)
        boolean isDaily = getIntent().getBooleanExtra("isDaily", false);
        int xpEarned = 100 + (correct * 50);
        if (isDaily) {
            xpEarned *= 2;
            android.widget.Toast.makeText(this, "Вызов дня! Получено двойной опыт: " + xpEarned + " XP!", android.widget.Toast.LENGTH_SHORT).show();
        }

        ProgressionManager.Progress oldProg = ProgressionManager.getProgress(this);
        ProgressionManager.addXp(this, xpEarned);
        ProgressionManager.Progress newProg = ProgressionManager.getProgress(this);
        
        long minTimeMs = getIntent().getLongExtra(QuizActivity.EXTRA_MIN_TIME_MS, 0);
        
        // Check Achievements
        AchievementManager.checkAchievements(this, score, correct, total, newProg.level, newProg.maxStreak,
                getIntent().getStringExtra(QuizActivity.EXTRA_QUESTIONS_JSON) != null, minTimeMs);
        
        if (newProg.level > oldProg.level) {
            android.widget.Toast.makeText(this, "Уровень повышен! Теперь ты " + newProg.level + " уровня!", android.widget.Toast.LENGTH_LONG).show();
        }

        // Quest progression
        QuestManager.updateProgress(this, "q1", correct);
        QuestManager.updateProgress(this, "q2", 1);
        if (accuracy == 100) QuestManager.updateProgress(this, "q3", 1);

        rvDetails.setLayoutManager(new LinearLayoutManager(this));
        rvDetails.setAdapter(new AnswerDetailAdapter(details));

        btnRestart.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra(QuizActivity.EXTRA_SET_RES_ID, setResId);
            i.putExtra(QuizActivity.EXTRA_SET_TITLE, setTitle);
            i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, numQuestions);
            i.putExtra(QuizActivity.EXTRA_TIMER_ENABLED, timerEnabled);
            i.putExtra(QuizActivity.EXTRA_TIME_SEC, timeSec);
            i.putExtra(QuizActivity.EXTRA_QUESTIONS_JSON, questionsJson);
            startActivity(i);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}

