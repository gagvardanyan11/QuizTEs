package gagik.vardanyan.quiz;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface QuizDao {
    @Insert
    void insert(QuizResult result);

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    List<QuizResult> getAllResults();

    @Query("SELECT * FROM quiz_results WHERE topic = :topic ORDER BY timestamp DESC")
    List<QuizResult> getResultsByTopic(String topic);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertCachedQuiz(CachedQuiz quiz);

    @Query("SELECT * FROM cached_quizzes WHERE topic = :topic LIMIT 1")
    CachedQuiz getCachedQuiz(String topic);

    @Query("SELECT topic FROM cached_quizzes ORDER BY timestamp DESC")
    List<String> getCachedTopics();

    @Query("DELETE FROM cached_quizzes")
    void clearCache();

    @Insert
    void insertSavedExplanation(SavedExplanation explanation);

    @Query("SELECT * FROM saved_explanations ORDER BY timestamp DESC")
    List<SavedExplanation> getAllSavedExplanations();

    @Query("DELETE FROM saved_explanations WHERE id = :id")
    void deleteSavedExplanation(int id);
}
