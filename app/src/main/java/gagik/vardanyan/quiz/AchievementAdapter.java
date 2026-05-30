package gagik.vardanyan.quiz;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    private final List<AchievementManager.Achievement> list;

    public AchievementAdapter(List<AchievementManager.Achievement> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementManager.Achievement ach = list.get(position);
        holder.tvTitle.setText(ach.title);
        holder.tvDesc.setText(ach.description);

        if (ach.unlocked) {
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFD700"))); // Gold
            holder.itemView.setAlpha(1.0f);
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeColor(Color.parseColor("#FFD700"));
        } else {
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD"))); // Gray
            holder.itemView.setAlpha(0.6f);
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeColor(Color.parseColor("#E0E0E0"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDesc;
        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivAchievementIcon);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDesc = itemView.findViewById(R.id.tvAchievementDesc);
        }
    }
}
