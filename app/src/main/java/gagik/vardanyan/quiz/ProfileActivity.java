package gagik.vardanyan.quiz;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ImageView ivSelectedAvatar;
    private TextInputEditText etName;
    private int selectedAvatarRes = R.drawable.ic_avatar_1;
    
    private final int[] avatarResList = {
            R.drawable.ic_avatar_1, R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3, R.drawable.ic_avatar_4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivSelectedAvatar = findViewById(R.id.ivSelectedAvatar);
        etName = findViewById(R.id.etName);
        GridLayout avatarGrid = findViewById(R.id.avatarGrid);

        // Load current profile
        String currentName = prefs.getString("user_name", "Игрок");
        selectedAvatarRes = prefs.getInt("user_avatar", R.drawable.ic_avatar_1);
        
        etName.setText(currentName);
        ivSelectedAvatar.setImageResource(selectedAvatarRes);

        // Setup avatar selection grid
        for (int resId : avatarResList) {
            View view = getLayoutInflater().inflate(R.layout.item_avatar_choice, avatarGrid, false);
            ImageView iv = view.findViewById(R.id.ivAvatarChoice);
            iv.setImageResource(resId);
            
            MaterialCardView card = view.findViewById(R.id.cardAvatarChoice);
            
            view.setOnClickListener(v -> {
                selectedAvatarRes = resId;
                ivSelectedAvatar.setImageResource(resId);
            });
            
            avatarGrid.addView(view);
        }

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) name = "Игрок";
            
            prefs.edit()
                    .putString("user_name", name)
                    .putInt("user_avatar", selectedAvatarRes)
                    .apply();
            
            Toast.makeText(this, "Профиль сохранен!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
