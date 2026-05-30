package gagik.vardanyan.quiz;

import java.util.List;

public class Question {
    private final String text;
    private final List<String> options;
    private final int correctIndex;
    private final int timeLimitSec;

    public Question(String text, List<String> options, int correctIndex, int timeLimitSec) {
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.timeLimitSec = timeLimitSec;
    }

    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public int getTimeLimitSec() { return timeLimitSec; }
}