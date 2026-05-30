package gagik.vardanyan.quiz;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiQuizService {
    public AiQuizService() {}

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String customKey = prefs.getString("custom_api_key", "");
        if (!customKey.isEmpty()) {
            return customKey;
        }
        return BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
    }

    private static String getBaseUrl(String apiKey) {
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;
    }

    public static String generateQuizJson(String apiKey, String topic, int count, int difficulty, int timeSec, String style) throws Exception {
        URL url = new URL(getBaseUrl(apiKey));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(40000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        String diffStr = difficulty == 0 ? "легкий" : (difficulty == 1 ? "средний" : "сложный");
        String stylePrompt = (style != null && !style.isEmpty() && !style.equals("Normal")) ? " В стиле: " + style + "." : "";

        String prompt = "Сгенерируй квиз на тему \"" + topic + "\". Вопросов: " + count + ". Сложность: " + diffStr + "." + stylePrompt +
                " Верни строго JSON массив объектов: [{\"question\": \"...\", \"options\": [\"...\", \"...\", \"...\", \"...\"], \"answer\": 0}]. Без лишнего текста. Только JSON.";

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject part = new JSONObject().put("text", prompt);
        contents.put(new JSONObject().put("parts", new JSONArray().put(part)));
        body.put("contents", contents);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }

        if (code != 200) {
            String error = sb.toString();
            if (code == 429) throw new Exception("HTTP 429: Превышен лимит запросов. Попробуйте через минуту.");
            if (code == 404) throw new Exception("HTTP 404: Модель не найдена. Проверьте AiQuizService.");
            throw new Exception("HTTP " + code + ": " + error);
        }

        try {
            JSONObject resp = new JSONObject(sb.toString());
            return cleanJson(resp.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text"));
        } catch (Exception e) {
            throw new Exception("Ошибка парсинга ответа ИИ: " + e.getMessage());
        }
    }

    private static String cleanJson(String text) {
        if (text == null) return "[]";
        text = text.trim();
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            if (text.contains("```")) text = text.substring(0, text.indexOf("```"));
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            if (text.contains("```")) text = text.substring(0, text.indexOf("```"));
        }
        return text.trim();
    }

    public static String getCoachReport(String apiKey, String quizStatsJson) throws Exception {
        try {
            URL url = new URL(getBaseUrl(apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String prompt = "Проанализируй результаты квиза: " + quizStatsJson + ". Дай короткий мотивирующий отчет на русском (2-3 предложения).";

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return "Отличная работа! Продолжай в том же духе.";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject resp = new JSONObject(sb.toString());
            return resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "Хороший результат! Дальше — больше.";
        }
    }

    public static String getHint(String apiKey, String question, String options) throws Exception {
        try {
            URL url = new URL(getBaseUrl(apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String prompt = "Дай короткую подсказку для вопроса: \"" + question + "\". Варианты: " + options + ". Не называй ответ прямо. На русском.";

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return "Подумай хорошенько!";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject resp = new JSONObject(sb.toString());
            return resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "Вспомни основы этой темы.";
        }
    }

    public static String getTrendingTopics(String apiKey) throws Exception {
        try {
            URL url = new URL(getBaseUrl(apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String prompt = "Предложи 3 темы для квиза через запятую. Только темы. На русском.";

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return "Наука, История, Кино";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject resp = new JSONObject(sb.toString());
            return resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "Наука, История, Кино";
        }
    }

    public static String suggestCategoryStyle(String apiKey, String topic) throws Exception {
        try {
            URL url = new URL(getBaseUrl(apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String prompt = "Для темы \"" + topic + "\" выбери цвет Hex и иконку (ic_book, ic_science, ic_history). Ответь JSON: {\"color\": \"#Hex\", \"icon\": \"ic_name\"}";

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return "{\"color\": \"#6366F1\", \"icon\": \"ic_book\"}";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject resp = new JSONObject(sb.toString());
            return resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "{\"color\": \"#6366F1\", \"icon\": \"ic_book\"}";
        }
    }

    public static String getExplanation(String apiKey, String question, String correctAnswer) throws Exception {
        try {
            URL url = new URL(getBaseUrl(apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String prompt = "Объясни кратко ответ \"" + correctAnswer + "\" на вопрос \"" + question + "\". На русском.";

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return "Это правильный ответ согласно фактам.";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject resp = new JSONObject(sb.toString());
            return resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "Этот ответ подтвержден проверенными источниками.";
        }
    }

    public String getQuickTip(int level, int maxStreak) {
        return "Знание — это приключение, которое никогда не заканчивается.";
    }
}
