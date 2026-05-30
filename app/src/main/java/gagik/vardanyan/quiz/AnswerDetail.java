package gagik.vardanyan.quiz;

import java.util.List;

public class AnswerDetail {
    private final String questionText;
    private final List<String> options;
    private final int correctIndex;
    private final int chosenIndex; // -1 if timeout / no answer
    private final boolean timeout;

    public AnswerDetail(String questionText, List<String> options, int correctIndex, int chosenIndex, boolean timeout) {
        this.questionText = questionText;
        this.options = options;
        this.correctIndex = correctIndex;
        this.chosenIndex = chosenIndex;
        this.timeout = timeout;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public int getChosenIndex() {
        return chosenIndex;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public boolean isCorrect() {
        return chosenIndex == correctIndex && !timeout;
    }
}

