package com.ahmadrezagh671.finxel.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.CompareResult;
import com.ahmadrezagh671.finxel.utilities.Utilities;

import java.util.List;

/**
 * RecyclerView adapter for displaying configuration comparison alerts in a list.
 */
public class RVCompareAlertAdapter extends RecyclerView.Adapter<RVCompareAlertAdapter.ViewHolder>{

    List<CompareResult> compareDataList;

    public RVCompareAlertAdapter(List<CompareResult> compareDataList) {
        this.compareDataList = compareDataList;
    }

    /**
     * Creates a new view holder for a comparison alert item.
     */
    @NonNull
    @Override
    public RVCompareAlertAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_compare_alert, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds comparison alert data to the view holder at the given position.
     *
     * @param holder   the view holder to bind
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull RVCompareAlertAdapter.ViewHolder holder, int position) {
        CompareResult compareData = compareDataList.get(position);

        holder.textView.setText(compareData.error + compareData.difference.stripTrailingZeros().toPlainString());
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utilities.copyToClipboard(v.getContext(), v.getContext().getString(R.string.compare_value), compareData.difference.stripTrailingZeros().toPlainString());
            }
        });
    }

    /**
     * Returns the number of comparison alert items in the list.
     */
    @Override
    public int getItemCount() {
        return compareDataList.size();
    }

    /**
     * ViewHolder for a single comparison alert item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.textView);
        }
    }
}