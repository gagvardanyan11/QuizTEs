package gagik.vardanyan.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;

public class LobbyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_lobby);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        LottieAnimationView lottie = findViewById(R.id.lottieSearch);
        if (lottie != null) {
            lottie.setSpeed(1.2f);
        }

        // Simulate finding a player after 3 seconds
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, "Противник найден! Подключение...", Toast.LENGTH_SHORT).show();
                
                // Переходим в викторину в режиме соревнования
                Intent intent = new Intent(this, QuizActivity.class);
                intent.putExtra(QuizActivity.EXTRA_SET_TITLE, "Дуэль");
                intent.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, 10);
                intent.putExtra(QuizActivity.EXTRA_TIME_SEC, 7); // Быстрее в дуэли
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}
