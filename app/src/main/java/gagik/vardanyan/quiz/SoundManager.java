package gagik.vardanyan.quiz;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.content.SharedPreferences;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;
    private int soundCorrect, soundWrong, soundClick, soundLevelUp, soundTimer;
    private boolean enabled;

    private SoundManager(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        // Загружаем звуки (предполагаем наличие файлов в res/raw)
        // soundCorrect = soundPool.load(context, R.raw.sfx_correct, 1);
        // soundWrong = soundPool.load(context, R.raw.sfx_wrong, 1);
        // soundClick = soundPool.load(context, R.raw.sfx_click, 1);
        // soundLevelUp = soundPool.load(context, R.raw.sfx_level_up, 1);
        // soundTimer = soundPool.load(context, R.raw.sfx_timer, 1);

        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        enabled = prefs.getBoolean("sound_enabled", true);
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) instance = new SoundManager(context);
        return instance;
    }

    public void playCorrect() { play(soundCorrect); }
    public void playWrong() { play(soundWrong); }
    public void playClick() { play(soundClick); }
    public void playLevelUp() { play(soundLevelUp); }
    public void playTimer() { play(soundTimer); }

    private void play(int soundId) {
        if (enabled && soundId != 0) {
            soundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }
    
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
