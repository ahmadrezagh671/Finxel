package com.ahmadrezagh671.finxel.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.HistoryItem;
import com.ahmadrezagh671.finxel.utilities.DateUtils;

import java.util.List;

/**
 * RecyclerView adapter for displaying history backup items with
 * action options to restore, download, open, or share each file.
 */
public class RVHistoryItemAdapter extends RecyclerView.Adapter<RVHistoryItemAdapter.HistoryViewHolder> {
    public interface OnItemClickListener {
        void onRestore(HistoryItem item, int position);
        void onDownload(HistoryItem item, int position);
        void onOpen(HistoryItem item, int position);
        void onShare(HistoryItem item, int position);
    }

    private List<HistoryItem> historyItems;
    private OnItemClickListener listener;

    public RVHistoryItemAdapter(List<HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Creates a new view holder for a history item.
     */
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_history_item, parent, false);
        return new HistoryViewHolder(view);
    }

    /**
     * Binds a history item to the view holder at the given position.
     *
     * @param holder   the view holder to bind
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        holder.bind(item, position);
    }

    /**
     * Returns the number of history items in the list.
     */
    @Override
    public int getItemCount() {
        return historyItems != null ? historyItems.size() : 0;
    }

    /**
     * ViewHolder for a single history item row.
     */
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvBankName, tvDeletedTime;
        ImageButton ibMore;
        View cardView;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBankName = itemView.findViewById(R.id.tvBankName);
            tvDeletedTime = itemView.findViewById(R.id.tvDeletedTime);
            ibMore = itemView.findViewById(R.id.ibMore);
            cardView = itemView.findViewById(R.id.cardView);
        }

        /**
         * Binds the history item data to the view and sets up the popup menu.
         *
         * @param item     the history item to bind
         * @param position the position of the item in the list
         */
        void bind(HistoryItem item, int position) {
            tvBankName.setText(item.getBankName());
            tvDeletedTime.setText(DateUtils.formatDate(item.getTimestamp(),"MMM dd, yyyy HH:mm"));

            cardView.setOnClickListener(v -> {
                showPopupMenu(ibMore, item, position);
            });
        }

        /**
         * Shows a popup menu with restore, download, open, and share actions.
         *
         * @param anchor   the view to anchor the popup menu to
         * @param item     the history item the actions apply to
         * @param position the position of the item in the list
         */
        private void showPopupMenu(View anchor, HistoryItem item, int position) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.getMenu().add(0, 1001, 0, R.string.restore_action);
            popupMenu.getMenu().add(0, 1002, 1, R.string.download_file);
            popupMenu.getMenu().add(0, 1003, 2, R.string.open_file);
            popupMenu.getMenu().add(0, 1004, 3, R.string.share_action);

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (listener == null) return false;
                switch (menuItem.getItemId()) {
                    case 1001:
                        listener.onRestore(item, position);
                        return true;
                    case 1002:
                        listener.onDownload(item, position);
                        return true;
                    case 1003:
                        listener.onOpen(item, position);
                        return true;
                    case 1004:
                        listener.onShare(item, position);
                        return true;
                    default:
                        return false;
                }
            });

            popupMenu.show();
        }
    }
}