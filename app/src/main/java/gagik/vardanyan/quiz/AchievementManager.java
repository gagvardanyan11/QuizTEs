package gagik.vardanyan.quiz;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class AchievementManager {
    private static final String PREFS_NAME = "achievements_prefs";

    public static class Achievement {
        public String id;
        public String title;
        public String description;
        public boolean unlocked;

        public Achievement(String id, String title, String description, boolean unlocked) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.unlocked = unlocked;
        }
    }

    public static List<Achievement> getAchievements(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<Achievement> list = new ArrayList<>();
        
        list.add(new Achievement("first_game", "Первооткрыватель", "Пройди свою первую игру", prefs.getBoolean("first_game", false)));
        list.add(new Achievement("perfect_score", "Перфекционист", "Ответь правильно на все вопросы", prefs.getBoolean("perfect_score", false)));
        list.add(new Achievement("streak_10", "Мастер серий", "Набери стрик из 10 правильных ответов", prefs.getBoolean("streak_10", false)));
        list.add(new Achievement("level_5", "Знаток", "Достигни 5 уровня", prefs.getBoolean("level_5", false)));
        list.add(new Achievement("fast_hand", "Быстрая рука", "Ответь за 1 секунду", prefs.getBoolean("fast_hand", false)));
        list.add(new Achievement("night_owl", "Ночная сова", "Сыграй игру после полуночи", prefs.getBoolean("night_owl", false)));
        list.add(new Achievement("ai_fan", "Любитель ИИ", "Сыграй 5 игр, сгенерированных ИИ", prefs.getBoolean("ai_fan", false)));
        
        return list;
    }

    public static void checkAchievements(Context context, int score, int correct, int total, int currentLevel, int maxStreak, boolean isAi, long timeTakenMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (!prefs.getBoolean("first_game", false)) editor.putBoolean("first_game", true);
        if (correct == total && total > 0) editor.putBoolean("perfect_score", true);
        if (maxStreak >= 10) editor.putBoolean("streak_10", true);
        if (currentLevel >= 5) editor.putBoolean("level_5", true);
        
        if (timeTakenMs < 1000 && timeTakenMs > 0) editor.putBoolean("fast_hand", true);
        
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 5) editor.putBoolean("night_owl", true);

        if (isAi) {
            int aiGames = prefs.getInt("ai_games_count", 0) + 1;
            editor.putInt("ai_games_count", aiGames);
            if (aiGames >= 5) editor.putBoolean("ai_fan", true);
        }

        editor.apply();
    }
}
