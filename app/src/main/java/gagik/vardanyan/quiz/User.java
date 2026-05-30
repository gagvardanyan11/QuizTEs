package gagik.vardanyan.quiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String username;
    public String password;
    public int avatarRes;
    public int level;
    public int xp;

    public User(String username, String password, int avatarRes) {
        this.username = username;
        this.password = password;
        this.avatarRes = avatarRes;
        this.level = 1;
        this.xp = 0;
    }
}
