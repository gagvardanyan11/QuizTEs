package gagik.vardanyan.quiz;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class QuizRepository {
    private QuizRepository() {}

    public static List<Question> loadDefaultQuestions(Context context) {
        return loadFromRaw(context, R.raw.questions);
    }

    public static List<Question> loadFromRaw(Context context, int rawResId) {
        try {
            InputStream is = context.getResources().openRawResource(rawResId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return loadFromJsonString(sb.toString());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<Question> loadFromJsonString(String json) {
        try {
            // Очистка строки от возможного мусора в начале или конце
            json = json.trim();
            
            JSONArray arr;
            if (json.startsWith("{")) {
                // Если ИИ вернул объект вместо массива, попробуем найти массив внутри
                JSONObject obj = new JSONObject(json);
                if (obj.has("questions")) {
                    arr = obj.getJSONArray("questions");
                } else if (obj.has("quiz")) {
                    arr = obj.getJSONArray("quiz");
                } else {
                    // Если не нашли стандартных ключей, берем первый попавшийся массив
                    JSONArray names = obj.names();
                    arr = null;
                    if (names != null) {
                        for (int i = 0; i < names.length(); i++) {
                            Object val = obj.get(names.getString(i));
                            if (val instanceof JSONArray) {
                                arr = (JSONArray) val;
                                break;
                            }
                        }
                    }
                    if (arr == null) return new ArrayList<>();
                }
            } else {
                arr = new JSONArray(json);
            }

            List<Question> questions = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String text = o.optString("question", "Без вопроса");
                
                JSONArray optArr = o.getJSONArray("options");
                List<String> options = new ArrayList<>();
                for (int j = 0; j < optArr.length(); j++) options.add(optArr.getString(j));
                
                // Проверяем все возможные ключи для правильного ответа
                int correctIndex = 0;
                if (o.has("answer")) correctIndex = o.getInt("answer");
                else if (o.has("correctIndex")) correctIndex = o.getInt("correctIndex");
                else if (o.has("correct_index")) correctIndex = o.getInt("correct_index");
                else if (o.has("correct")) correctIndex = o.getInt("correct");

                int timeLimitSec = o.optInt("timeLimitSec", 15);
                questions.add(new Question(text, options, correctIndex, timeLimitSec));
            }
            return questions;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

