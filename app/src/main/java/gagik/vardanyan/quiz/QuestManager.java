package gagik.vardanyan.quiz;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private static final String PREFS_NAME = "quests_prefs";
    
    public static class Quest {
        public String id;
        public String title;
        public int goal;
        public int progress;
        public int rewardXp;
        public boolean completed;
        public boolean rewardClaimed;

        public Quest(String id, String title, int goal, int rewardXp) {
            this.id = id;
            this.title = title;
            this.goal = goal;
            this.rewardXp = rewardXp;
        }
    }

    public static List<Quest> getDailyQuests(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastReset = prefs.getLong("last_reset", 0);
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        if (lastReset != today) {
            resetQuests(prefs, today);
        }

        List<Quest> quests = new ArrayList<>();
        quests.add(loadQuest(prefs, "q1", "Ответь на 10 вопросов", 10, 100));
        quests.add(loadQuest(prefs, "q2", "Сыграй 3 квиза", 3, 150));
        quests.add(loadQuest(prefs, "q3", "Идеальный квиз (100%)", 1, 300));
        return quests;
    }

    private static void resetQuests(SharedPreferences prefs, long today) {
        prefs.edit()
                .putLong("last_reset", today)
                .putInt("q1_progress", 0).putBoolean("q1_claimed", false)
                .putInt("q2_progress", 0).putBoolean("q2_claimed", false)
                .putInt("q3_progress", 0).putBoolean("q3_claimed", false)
                .apply();
    }

    private static Quest loadQuest(SharedPreferences prefs, String id, String title, int goal, int xp) {
        Quest q = new Quest(id, title, goal, xp);
        q.progress = prefs.getInt(id + "_progress", 0);
        q.rewardClaimed = prefs.getBoolean(id + "_claimed", false);
        q.completed = q.progress >= q.goal;
        return q;
    }

    public static void updateProgress(Context context, String id, int add) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(id + "_progress", 0);
        prefs.edit().putInt(id + "_progress", current + add).apply();
    }

    public static void claimReward(Context context, Quest quest) {
        if (quest.completed && !quest.rewardClaimed) {
            ProgressionManager.addXp(context, quest.rewardXp);
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(quest.id + "_claimed", true).apply();
        }
    }
}
