package gagik.vardanyan.quiz;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

public class KnowledgeBaseActivity extends AppCompatActivity {

    private RecyclerView rvSavedExplanations;
    private TextView tvEmpty;
    private SavedExplanationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_knowledge_base);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSavedExplanations = findViewById(R.id.rvSavedExplanations);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvSavedExplanations.setLayoutManager(new LinearLayoutManager(this));

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<SavedExplanation> list = AppDatabase.getInstance(this).quizDao().getAllSavedExplanations();
            runOnUiThread(() -> {
                if (list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvSavedExplanations.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvSavedExplanations.setVisibility(View.VISIBLE);
                    adapter = new SavedExplanationAdapter(list, this::deleteItem);
                    rvSavedExplanations.setAdapter(adapter);
                }
            });
        }).start();
    }

    private void deleteItem(SavedExplanation item) {
        new Thread(() -> {
            AppDatabase.getInstance(this).quizDao().deleteSavedExplanation(item.id);
            runOnUiThread(this::loadData);
        }).start();
    }
}
