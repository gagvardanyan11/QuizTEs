package gagik.vardanyan.quiz;

public final class QuestionSets {
    private QuestionSets() {}

    public static int resolveRawResId(String topic, int difficultyIndex) {
        boolean isAndroid = topic != null && topic.toLowerCase().contains("android");
        switch (difficultyIndex) {
            case 1:
                return isAndroid ? R.raw.questions_android_medium : R.raw.questions_java_medium;
            case 2:
                return isAndroid ? R.raw.questions_android_hard : R.raw.questions_java_hard;
            case 0:
            default:
                return isAndroid ? R.raw.questions_android_easy : R.raw.questions_java_easy;
        }
    }
}

