package gagik.vardanyan.quiz;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_quizzes")
public class CachedQuiz {
    @PrimaryKey
    @NonNull
    public String topic;
    
    public String jsonContent;
    public long timestamp;

    public CachedQuiz(@NonNull String topic, String jsonContent, long timestamp) {
        this.topic = topic;
        this.jsonContent = jsonContent;
        this.timestamp = timestamp;
    }
}
