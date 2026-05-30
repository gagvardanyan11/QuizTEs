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

        VH(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardResult);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvYour = itemView.findViewById(R.id.tvYour);
            tvCorrect = itemView.findViewById(R.id.tvCorrect);
        }
    }
}

