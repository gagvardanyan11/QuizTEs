package gagik.vardanyan.quiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_explanations")
public class SavedExplanation {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String question;
    public String answer;
    public String explanation;
    public long timestamp;

    public SavedExplanation(String question, String answer, String explanation, long timestamp) {
        this.question = question;
        this.answer = answer;
        this.explanation = explanation;
        this.timestamp = timestamp;
    }
}
