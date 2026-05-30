package gagik.vardanyan.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardUser> users;

    public static class LeaderboardUser {
        public String name;
        public int xp;
        public int avatarRes;
        public int rank;

        public LeaderboardUser(int rank, String name, int xp, int avatarRes) {
            this.rank = rank;
            this.name = name;
            this.xp = xp;
            this.avatarRes = avatarRes;
        }
    }

    public LeaderboardAdapter(List<LeaderboardUser> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardUser user = users.get(position);
        holder.tvRank.setText("#" + user.rank);
        holder.tvName.setText(user.name);
        holder.tvScore.setText(String.format("%,d XP", user.xp));
        holder.ivAvatar.setImageResource(user.avatarRes);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvScore;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvScore = itemView.findViewById(R.id.tvScore);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
