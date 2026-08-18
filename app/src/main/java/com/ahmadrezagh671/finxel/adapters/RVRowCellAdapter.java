package com.ahmadrezagh671.finxel.adapters;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.configModel.Cell;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.utilities.Utilities;

import java.util.List;

/**
 * RecyclerView adapter for displaying a grid of cells in a
 * sheet layout, including row/column index headers and data cells.
 */
public class RVRowCellAdapter extends RecyclerView.Adapter<RVRowCellAdapter.RowViewHolder> {

    public interface OnItemClick {
        void itemClicked(int rowPosition, int columnPosition,int inRecyclerPosition, View v, Cell currentCell);
    }
    List<List<String>> sheetData;
    ConfigModel configModel;
    OnItemClick onItemClick;

    public RVRowCellAdapter(List<List<String>> sheetData, ConfigModel configModel) {
        this.sheetData = sheetData;
        this.configModel = configModel;
    }

    public void setOnItemClick(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    /**
     * Creates a new view holder for a row cell.
     */
    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_row_cell, parent, false);
        return new RowViewHolder(view);
    }

    /**
     * Binds row data to the view holder at the given position,
     * building index headers, column headers, and data cells.
     *
     * @param holder   the view holder to bind
     * @param position the position of the row in the list
     */
    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.container.removeAllViews();

        // add index cell ///////////////////////
        View viewCellIndex = LayoutInflater.from(holder.container.getContext()).inflate(R.layout.rv_cell, holder.container, false);
        TextView textCellIndex = viewCellIndex.findViewById(R.id.textCell);
        textCellIndex.setText(String.valueOf(position));
        if (position == 0) textCellIndex.setText("");
        textCellIndex.setOnClickListener(null);
        int widthCellIndex = holder.container.getContext()
                .getResources()
                .getDimensionPixelSize(R.dimen.cellWidth);
        ViewGroup.LayoutParams paramCellIndex = textCellIndex.getLayoutParams();
        paramCellIndex.width = (int) (widthCellIndex * 0.5);
        if (position == 0) paramCellIndex.height = (int) (paramCellIndex.height * 0.7);
        textCellIndex.setLayoutParams(paramCellIndex);
        ((GradientDrawable)textCellIndex.getBackground()).setColor(Utilities.getThemeAttrColor(textCellIndex,com.google.android.material.R.attr.colorOutlineVariant));
        holder.container.addView(viewCellIndex);
        // /////////////////////////////////


        if (position == 0){
            // a b c d row //////////////////////////////////////
            for (int i = 0; i < sheetData.get(0).size(); i++){
                char character = (char)('A' + i);
                Cell currentCell = configModel.result.get(0).get(i);

                View viewCellAlpha = LayoutInflater.from(holder.container.getContext()).inflate(R.layout.rv_cell, holder.container, false);
                TextView textCellAlpha = viewCellAlpha.findViewById(R.id.textCell);
                textCellAlpha.setText(String.valueOf(character));
                textCellAlpha.setOnClickListener(null);
                int widthCellAlpha = holder.container.getContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.cellWidth);
                ViewGroup.LayoutParams paramCellAlpha = textCellAlpha.getLayoutParams();
                paramCellAlpha.width = (int) (widthCellAlpha * currentCell.size);
                paramCellAlpha.height = (int) (paramCellAlpha.height * 0.7);
                textCellAlpha.setLayoutParams(paramCellAlpha);
                ((GradientDrawable)textCellAlpha.getBackground()).setColor(Utilities.getThemeAttrColor(textCellAlpha,com.google.android.material.R.attr.colorOutlineVariant));
                holder.container.addView(viewCellAlpha);
            }
            return;
        }

        // data row //////////////////////////////////////
        int dataPosition = position - 1;
        List<String> rowData = sheetData.get(dataPosition);

        for (int i = 0; i < rowData.size(); i++) {
            String item = rowData.get(i);
            Cell currentCell = configModel.result.get(dataPosition % configModel.result.size()).get(i);

            View view = LayoutInflater.from(holder.container.getContext()).inflate(R.layout.rv_cell, holder.container, false);

            TextView textCell = view.findViewById(R.id.textCell);
            textCell.setText(item);
            textCell.setTag(dataPosition + ";" + i);
            textCell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int rowPosition = Integer.parseInt(v.getTag().toString().split(";")[0]);
                    int columnPosition = Integer.parseInt(v.getTag().toString().split(";")[1]);
                    onItemClick.itemClicked(rowPosition, columnPosition,position,v,currentCell);
                }
            });

            // background color
            ColorStateList backgroundColor = currentCell.b_color == -1 ? ColorStateList.valueOf(0x00000000) : ColorStateList.valueOf(currentCell.b_color);
            ((GradientDrawable)textCell.getBackground()).setColor(backgroundColor);

            // foreground text color
            ColorStateList foregroundColor = currentCell.f_color == -1 ? Utilities.getThemeAttrColor(textCell, com.google.android.material.R.attr.colorOnBackground) : ColorStateList.valueOf(currentCell.f_color);
            textCell.setTextColor(foregroundColor);

            // set width
            int width = holder.container.getContext()
                    .getResources()
                    .getDimensionPixelSize(R.dimen.cellWidth);

            ViewGroup.LayoutParams params = textCell.getLayoutParams();
            params.width = (int) (width * currentCell.size);
            textCell.setLayoutParams(params);

            holder.container.addView(view);
        }
    }

    /**
     * Returns the number of rows including the header row.
     */
    @Override
    public int getItemCount() {
        // + 1 for a b c row
        return sheetData != null ? sheetData.size() + 1 : 0;
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
        }
    }
}
