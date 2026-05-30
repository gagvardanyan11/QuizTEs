package gagik.vardanyan.quiz;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    public enum Theme {
        DEFAULT, NEON, OCEAN
    }

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String themeStr = prefs.getString("theme_preset", "DEFAULT");
        Theme theme = Theme.valueOf(themeStr);
        
        switch (theme) {
            case NEON:
                context.setTheme(R.style.Theme_Quiz_Neon);
                break;
            case OCEAN:
                context.setTheme(R.style.Theme_Quiz_Ocean);
                break;
            default:
                context.setTheme(R.style.Theme_Quiz);
                break;
        }
    }

    public static void setTheme(Context context, Theme theme) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putString("theme_preset", theme.name()).apply();
    }
}
