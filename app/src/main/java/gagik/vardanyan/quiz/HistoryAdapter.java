package gagik.vardanyan.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<QuizResult> results;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public HistoryAdapter(List<QuizResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizResult res = results.get(position);
        holder.tvTopic.setText(res.topic);
        holder.tvDate.setText(dateFormat.format(new Date(res.timestamp)));
        holder.tvScore.setText(String.valueOf(res.score));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDate, tvScore;
        ViewHolder(View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvHistoryTopic);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvScore = itemView.findViewById(R.id.tvHistoryScore);
        }
    }
}
