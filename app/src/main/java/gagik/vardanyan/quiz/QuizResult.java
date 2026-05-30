package gagik.vardanyan.quiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_results")
public class QuizResult {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String topic;
    public int score;
    public int totalQuestions;
    public long timestamp;

    public QuizResult(String topic, int score, int totalQuestions, long timestamp) {
        this.topic = topic;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = timestamp;
    }
}
