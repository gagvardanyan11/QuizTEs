package gagik.vardanyan.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class SavedExplanationAdapter extends RecyclerView.Adapter<SavedExplanationAdapter.VH> {
    private final List<SavedExplanation> items;
    private final OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete(SavedExplanation item);
    }

    public SavedExplanationAdapter(List<SavedExplanation> items, OnDeleteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_explanation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SavedExplanation item = items.get(position);
        h.tvQuestion.setText(item.question);
        h.tvAnswer.setText("Ответ: " + item.answer);
        h.tvExplanation.setText(item.explanation);
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer, tvExplanation;
        MaterialButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            tvExplanation = itemView.findViewById(R.id.tvExplanation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
