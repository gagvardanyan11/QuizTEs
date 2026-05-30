package gagik.vardanyan.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class QuizRandomizer {
    private QuizRandomizer() {}

    public static List<Question> shuffleQuestionsAndOptions(List<Question> in) {
        if (in == null || in.isEmpty()) return in;

        Random rnd = new Random();
        List<Question> out = new ArrayList<>(in.size());

        for (Question q : in) {
            List<String> opts = q.getOptions();
            if (opts == null || opts.size() < 2) {
                out.add(q);
                continue;
            }

            ArrayList<Integer> indices = new ArrayList<>();
            for (int i = 0; i < opts.size(); i++) indices.add(i);
            Collections.shuffle(indices, rnd);

            ArrayList<String> shuffled = new ArrayList<>(opts.size());
            int newCorrect = 0;
            for (int newPos = 0; newPos < indices.size(); newPos++) {
                int oldPos = indices.get(newPos);
                shuffled.add(opts.get(oldPos));
                if (oldPos == q.getCorrectIndex()) newCorrect = newPos;
            }

            out.add(new Question(q.getText(), shuffled, newCorrect, q.getTimeLimitSec()));
        }

        Collections.shuffle(out, rnd);
        return out;
    }
}

