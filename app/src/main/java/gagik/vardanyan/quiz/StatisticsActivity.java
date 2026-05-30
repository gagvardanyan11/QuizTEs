package gagik.vardanyan.quiz;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_statistics);

        LineChart chart = findViewById(R.id.chart);
        TextView tvTotalGames = findViewById(R.id.tvTotalGames);
        TextView tvBestScore = findViewById(R.id.tvBestScore);
        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        RecyclerView rvAchievements = findViewById(R.id.rvAchievements);
        TextView tvCoachReport = findViewById(R.id.tvCoachReport);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvAchievements.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        new Thread(() -> {
            List<QuizResult> results = AppDatabase.getInstance(this).quizDao().getAllResults();
            List<AchievementManager.Achievement> achievements = AchievementManager.getAchievements(this);
            
            runOnUiThread(() -> {
                tvTotalGames.setText("Всего игр: " + results.size());
                rvHistory.setAdapter(new HistoryAdapter(results));
                rvAchievements.setAdapter(new AchievementAdapter(achievements));
                
                int max = 0;
                List<Entry> entries = new ArrayList<>();
                StringBuilder summary = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    QuizResult r = results.get(results.size() - 1 - i);
                    entries.add(new Entry(i, r.score));
                    if (r.score > max) max = r.score;
                    if (i < 5) {
                        summary.append("Тема: ").append(r.topic)
                                .append(", Очки: ").append(r.score)
                                .append("/").append(r.totalQuestions).append("; ");
                    }
                }
                tvBestScore.setText("Рекорд: " + max);

                if (!entries.isEmpty()) {
                    LineDataSet dataSet = new LineDataSet(entries, "История очков");
                    dataSet.setColor(getColor(R.color.brand_purple));
                    dataSet.setCircleColor(getColor(R.color.brand_purple));
                    dataSet.setLineWidth(3f);
                    dataSet.setCircleRadius(5f);
                    dataSet.setDrawValues(false);
                    
                    LineData lineData = new LineData(dataSet);
                    chart.setData(lineData);
                    chart.invalidate();
                    
                    new Thread(() -> {
                        try {
                            String apiKey = AiQuizService.getApiKey(this);
                            if (apiKey != null && !apiKey.isEmpty()) {
                                String report = AiQuizService.getCoachReport(apiKey, summary.toString());
                                runOnUiThread(() -> tvCoachReport.setText(report));
                            } else {
                                runOnUiThread(() -> tvCoachReport.setText("API ключ не найден."));
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> tvCoachReport.setText("Не удалось получить совет от тренера."));
                        }
                    }).start();
                } else {
                    tvCoachReport.setText("Сыграйте хотя бы раз, чтобы ИИ-тренер дал совет!");
                }
            });
        }).start();
    }
}
