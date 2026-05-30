package gagik.vardanyan.quiz;

import android.content.Context;
import android.content.SharedPreferences;

public class ProgressionManager {
    private static final String PREFS_NAME = "progression_prefs";
    private static final String KEY_XP = "total_xp";
    private static final String KEY_STREAK = "answer_streak";
    private static final String KEY_MAX_STREAK = "max_streak";
    
    public static class Progress {
        public int level;
        public int currentXp;
        public int xpForNextLevel;
        public float percent;
        public int maxStreak;

        public Progress(int xp, int maxStreak) {
            level = 1;
            int remainingXp = xp;
            int required = 1000;
            
            while (remainingXp >= required) {
                remainingXp -= required;
                level++;
                required = level * 1000;
            }
            
            currentXp = remainingXp;
            xpForNextLevel = required;
            percent = (float) currentXp / xpForNextLevel;
            this.maxStreak = maxStreak;
        }
    }

    public static Progress getProgress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new Progress(prefs.getInt(KEY_XP, 0), prefs.getInt(KEY_MAX_STREAK, 0));
    }

    public static void updateStreak(Context context, boolean correct) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_STREAK, 0);
        int max = prefs.getInt(KEY_MAX_STREAK, 0);
        
        if (correct) {
            current++;
            if (current > max) {
                prefs.edit().putInt(KEY_MAX_STREAK, current).apply();
            }
            prefs.edit().putInt(KEY_STREAK, current).apply();
        } else {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
        }
    }

    public static int addXp(Context context, int xpToAdd) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int oldXp = prefs.getInt(KEY_XP, 0);
        int totalXp = oldXp + xpToAdd;
        prefs.edit().putInt(KEY_XP, totalXp).apply();
        
        Progress oldP = new Progress(oldXp, 0);
        Progress newP = new Progress(totalXp, 0);
        
        if (newP.level > oldP.level) {
            showLevelUpDialog(context, newP.level);
        }
        
        return totalXp;
    }

    private static void showLevelUpDialog(Context context, int newLevel) {
        if (!(context instanceof android.app.Activity)) return;
        
        android.app.Activity activity = (android.app.Activity) context;
        activity.runOnUiThread(() -> {
            SoundManager.getInstance(context).playLevelUp();
            
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
                    .setView(android.view.LayoutInflater.from(context).inflate(R.layout.dialog_level_up, null))
                    .create();

            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.show();

            android.view.View view = dialog.findViewById(android.R.id.content);
            if (view != null) {
                android.widget.TextView tv = view.findViewById(R.id.tvNewLevel);
                if (tv != null) tv.setText("Вы достигли уровня " + newLevel + "!");
                android.view.View btn = view.findViewById(R.id.btnCollect);
                if (btn != null) btn.setOnClickListener(v -> dialog.dismiss());
            }
        });
    }
}
