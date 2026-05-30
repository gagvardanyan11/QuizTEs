package gagik.vardanyan.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnswerDetailAdapter extends RecyclerView.Adapter<AnswerDetailAdapter.VH> {
    private final List<AnswerDetail> items;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AnswerDetailAdapter(List<AnswerDetail> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_answer_detail, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AnswerDetail d = items.get(position);

        h.tvNumber.setText(String.valueOf(position + 1));
        h.tvQuestion.setText(d.getQuestionText());

        String correctText = safeOption(d, d.getCorrectIndex());
        String chosenText = d.isTimeout() ? "Время вышло" : safeOption(d, d.getChosenIndex());

        h.tvCorrect.setText("Правильный: " + correctText);
        h.tvYour.setText("Твой: " + chosenText);

        int colorRes = d.isCorrect() ? R.color.correct_bg : R.color.wrong_bg;
        int bgColorRes = d.isCorrect() ? R.color.correct_light : R.color.wrong_light;
        
        h.tvStatus.setText(d.isCorrect() ? "Верно" : "Неверно");
        h.tvStatus.setTextColor(h.itemView.getContext().getColor(colorRes));

        h.cardView.setCardBackgroundColor(h.itemView.getContext().getColor(bgColorRes));
        h.cardView.setStrokeColor(h.itemView.getContext().getColor(colorRes));
        h.cardView.setStrokeWidth(3);

        h.btnExplain.setOnClickListener(v -> {
            showAiExplanation(v.getContext(), d.getQuestionText(), correctText);
        });
    }

    private void showAiExplanation(android.content.Context context, String question, String answer) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_ai_explanation, null);
        dialog.setContentView(view);

        TextView tvContent = view.findViewById(R.id.tvExplanationContent);
        com.google.android.material.progressindicator.CircularProgressIndicator progress = view.findViewById(R.id.progressExplanation);
        View btnClose = view.findViewById(R.id.btnDismiss);
        View btnSave = view.findViewById(R.id.btnSave);
        
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnSave != null) btnSave.setEnabled(false);

        if (progress != null) progress.show();
        dialog.show();

        String apiKey = AiQuizService.getApiKey(context);
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        if (apiKey == null || apiKey.isEmpty()) {
            tvContent.setText("Ошибка: API ключ не найден в настройках.");
            if (progress != null) progress.hide();
            return;
        }

        executor.execute(() -> {
            try {
                String explanation = AiQuizService.getExplanation(apiKey, question, answer);
                mainHandler.post(() -> {
                    if (progress != null) progress.hide();
                    tvContent.setText(explanation);
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setOnClickListener(v -> {
                            saveToDatabase(context, question, answer, explanation);
                            btnSave.setEnabled(false);
                            Toast.makeText(context, "Сохранено в базу знаний!", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (progress != null) progress.hide();
                    tvContent.setText("Не удалось получить объяснение. Проверьте интернет или лимиты API.");
                });
            }
        });
    }

    private void saveToDatabase(android.content.Context context, String q, String a, String e) {
        new Thread(() -> {
            AppDatabase.getInstance(context).quizDao().insertSavedExplanation(
                    new SavedExplanation(q, a, e, System.currentTimeMillis())
            );
        }).start();
    }

    private String safeOption(AnswerDetail d, int idx) {
        if (idx < 0 || idx >= d.getOptions().size()) return "—";
        return d.getOptions().get(idx);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView tvNumber;
        final TextView tvStatus;
        final TextView tvQuestion;
        final TextView tvYour;
        final TextView tvCorrect;
        final MaterialButton btnExplain;

        VH(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardResult);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvYour = itemView.findViewById(R.id.tvYour);
            tvCorrect = itemView.findViewById(R.id.tvCorrect);
            btnExplain = itemView.findViewById(R.id.btnExplain);
        }
    }
}

