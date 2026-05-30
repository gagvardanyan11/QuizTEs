package gagik.vardanyan.quiz;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_leaderboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvLeaderboard = findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));

        TextView tvUserRank = findViewById(R.id.tvUserRank);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserScore = findViewById(R.id.tvUserScore);

        ProgressionManager.Progress progress = ProgressionManager.getProgress(this);
        String currentName = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("user_name", "QuizMaster");
        
        tvUserName.setText(currentName);
        tvUserScore.setText(String.format("%,d XP", (int)(progress.level * 1000 + progress.percent * 1000)));

        List<LeaderboardAdapter.LeaderboardUser> mockUsers = generateMockUsers();
        
        // Find user position in mock list
        int userXP = (int)(progress.level * 1000 + progress.percent * 1000);
        int rank = 1;
        for (LeaderboardAdapter.LeaderboardUser u : mockUsers) {
            if (u.xp > userXP) rank++;
        }
        tvUserRank.setText("#" + rank);

        rvLeaderboard.setAdapter(new LeaderboardAdapter(mockUsers));
    }

    private List<LeaderboardAdapter.LeaderboardUser> generateMockUsers() {
        List<LeaderboardAdapter.LeaderboardUser> list = new ArrayList<>();
        String[] names = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Leo", "Max", "Sonic", "Pixel", "Droid"};
        int[] avatars = {R.drawable.ic_avatar_1, R.drawable.ic_avatar_2, R.drawable.ic_avatar_3, R.drawable.ic_avatar_4};
        Random r = new Random();

        for (int i = 0; i < names.length; i++) {
            int xp = 5000 + r.nextInt(20000);
            list.add(new LeaderboardAdapter.LeaderboardUser(0, names[i], xp, avatars[r.nextInt(avatars.length)]));
        }

        // Sort by XP
        Collections.sort(list, (u1, u2) -> Integer.compare(u2.xp, u1.xp));
        
        // Assign ranks
        for (int i = 0; i < list.size(); i++) {
            list.get(i).rank = i + 1;
        }
        
        return list;
    }
}
