package gagik.vardanyan.quiz;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class QuizResultCodec {
    private QuizResultCodec() {}

    public static String encode(List<AnswerDetail> details) {
        JSONArray arr = new JSONArray();
        for (AnswerDetail d : details) {
            JSONObject o = new JSONObject();
            try {
                o.put("q", d.getQuestionText());
                o.put("correct", d.getCorrectIndex());
                o.put("chosen", d.getChosenIndex());
                o.put("timeout", d.isTimeout());
                JSONArray opts = new JSONArray();
                for (String s : d.getOptions()) opts.put(s);
                o.put("opts", opts);
            } catch (Exception ignored) {
                // ignore broken item
            }
            arr.put(o);
        }
        return arr.toString();
    }

    public static List<AnswerDetail> decode(String json) {
        List<AnswerDetail> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String q = o.optString("q", "");
                int correct = o.optInt("correct", 0);
                int chosen = o.optInt("chosen", -1);
                boolean timeout = o.optBoolean("timeout", false);
                JSONArray optsArr = o.optJSONArray("opts");
                List<String> opts = new ArrayList<>();
                if (optsArr != null) {
                    for (int j = 0; j < optsArr.length(); j++) opts.add(optsArr.getString(j));
                }
                out.add(new AnswerDetail(q, opts, correct, chosen, timeout));
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}

