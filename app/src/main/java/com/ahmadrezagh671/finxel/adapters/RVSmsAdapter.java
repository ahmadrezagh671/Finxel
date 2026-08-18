package com.ahmadrezagh671.finxel.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.models.CompareResult;
import com.ahmadrezagh671.finxel.models.MyLocation;
import com.ahmadrezagh671.finxel.models.MySMS;
import com.ahmadrezagh671.finxel.models.configModel.CompareWithLastMessage;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.material.color.MaterialColors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying SMS messages with config-based
 * validation, field comparison alerts, and checked status tracking.
 */
public class RVSmsAdapter extends RecyclerView.Adapter<RVSmsAdapter.MyViewHolder>{

    List<MySMS> smsList;
    List<Map<String, String>> fieldResultsList;

    List<Map<String, CompareResult>> compareResultsList;
    ConfigModel configModel;

    OnItemClick onItemClick;
    AppDatabase db;

    public interface OnItemClick {
        void itemClicked(Map<String, String> existFields,MySMS sms,ConfigModel configModel, int position);
    }

    public RVSmsAdapter(AppDatabase db, List<MySMS> smsList, ConfigModel configModel, OnItemClick onItemClick) {
        this.db = db;
        this.smsList = smsList;
        this.configModel = configModel;
        this.onItemClick = onItemClick;

        fieldResultsList = new ArrayList<>();
        for (int i = 0; i < smsList.size(); i++) {
            fieldResultsList.add(new HashMap<>());
        }

        compareResultsList = new ArrayList<>();
        for (int i = 0; i < smsList.size(); i++) {
            compareResultsList.add(null);
        }

    }

    /**
     * Creates a new view holder for an SMS message item.
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_message, parent, false);
        return new MyViewHolder(v);
    }

    /**
     * Binds SMS data to the view holder, running validation, comparison checks,
     * and database status retrieval on background threads as needed.
     *
     * @param holder   the view holder to bind
     * @param position the position of the SMS in the list
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MySMS currentSMS = smsList.get(position);

        holder.tvBody.setText(currentSMS.getBody());
        holder.tvDate.setText(currentSMS.getFormatedDate("MMM dd, hh:mm a"));

        holder.tvError.setVisibility(View.GONE);
        holder.tvUnknownError.setVisibility(View.GONE);
        holder.tagsLayout.setVisibility(View.VISIBLE);
        holder.rvCompareAlerts.setVisibility(View.GONE);
        holder.cardViewMain.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(holder.cardViewMain, com.google.android.material.R.attr.colorPrimaryContainer)));
        holder.tvDate.setTextColor(ColorStateList.valueOf(MaterialColors.getColor(holder.tvDate, com.google.android.material.R.attr.colorOnPrimaryContainer)));

        String error = configModel.checkSkips(currentSMS.getBody());
        if (error != null){ // error exist
            holder.tvError.setText(error);
            holder.tvError.setVisibility(View.VISIBLE);
            holder.tagsLayout.setVisibility(View.GONE);
            holder.cardViewMain.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(holder.cardViewMain, com.google.android.material.R.attr.colorErrorContainer)));
            holder.tvDate.setTextColor(ColorStateList.valueOf(MaterialColors.getColor(holder.tvDate, com.google.android.material.R.attr.colorOnErrorContainer)));
        }else {
            fieldResultsList.set(position,configModel.getFieldsResult(fieldResultsList.get(position),currentSMS,db,true));
            try {
                // set compare alerts
                if (!configModel.compareWithLastMessage.isEmpty()){
                    // get compareResults for position if not defined before
                    if (compareResultsList.get(position) == null){
                        compareResultsList.set(position,new HashMap<>());

                        // find last message
                        int lastMessagePosition = position + 1;
                        while (lastMessagePosition < smsList.size()){
                            if (configModel.checkSkips(smsList.get(lastMessagePosition).getBody()) != null){
                                lastMessagePosition = lastMessagePosition + 1;
                            }else {
                                fieldResultsList.set(lastMessagePosition,configModel.getFieldsResult(fieldResultsList.get(lastMessagePosition),smsList.get(lastMessagePosition),db,true));
                                break;
                            }
                        }

                        if (lastMessagePosition < smsList.size()){
                            // get compare data
                            for (CompareWithLastMessage compareWithLastMessage: configModel.compareWithLastMessage) {
                                BigDecimal first = new BigDecimal(fieldResultsList.get(position).get(compareWithLastMessage.field));
                                BigDecimal second = new BigDecimal(fieldResultsList.get(lastMessagePosition).get(compareWithLastMessage.lastMessageField));
                                BigDecimal difference = first.subtract(second);
                                if (difference.compareTo(BigDecimal.ZERO) != 0){
                                    compareResultsList.get(position).put(compareWithLastMessage.name,new CompareResult(compareWithLastMessage.errorText,difference));
                                }
                            }
                        }
                    }

                    if (!compareResultsList.get(position).isEmpty()){
                        holder.rvCompareAlerts.setVisibility(View.VISIBLE);
                        holder.rvCompareAlerts.setLayoutManager(new LinearLayoutManager(holder.rvCompareAlerts.getContext()));
                        holder.rvCompareAlerts.setAdapter(new RVCompareAlertAdapter(new ArrayList<>(compareResultsList.get(position).values())));
                    }
                }
            } catch (Exception e) {
                holder.tvUnknownError.setVisibility(View.VISIBLE);
                holder.tvUnknownError.setText(holder.view.getContext().getString(R.string.error_101));
                compareResultsList.set(position,null);
            }
        }

        holder.view.setOnClickListener(v -> onItemClick.itemClicked(fieldResultsList.get(position),currentSMS,configModel,position));

        holder.cardViewStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = holder.tvStatus.getText().toString();

                boolean newStatus = !status.equals("Checked");
                holder.setStatus(newStatus);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SmsRecord smsRecord = db.smsRecordDao().getRecordById(currentSMS.getId());
                        if (smsRecord == null)
                            smsRecord = new SmsRecord(currentSMS.getId(),null);
                        smsRecord.addValueToJson("checked",newStatus);
                        db.smsRecordDao().insertOrUpdate(smsRecord);
                    }
                }).start();
            }
        });

        // update checked status and location from db
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 1. Database work on the background thread
                SmsRecord smsRecord = db.smsRecordDao().getRecordById(currentSMS.getId());


                // 2. Determine the text to display
                final boolean readStatus;
                final MyLocation myLocation;
                if (smsRecord != null){
                    // status
                    Object object = smsRecord.getValueFromJson("checked");
                    if (object instanceof Boolean){
                        readStatus = (boolean) object;
                    } else {
                        readStatus = false;
                    }

                    // location
                    Object objectLat = smsRecord.getValueFromJson("lat");
                    Object objectLng = smsRecord.getValueFromJson("lng");

                    if (objectLat != null && objectLng != null){
                        myLocation = new MyLocation(Double.parseDouble(objectLat.toString()),Double.parseDouble(objectLng.toString()));
                    }else {
                        myLocation = null;
                    }
                }else {
                    readStatus = false;
                    myLocation = null;
                }

                // 3. Switch back to the Main UI Thread to update the TextView
                holder.view.post(new Runnable() {
                    @Override
                    public void run() {
                        holder.setStatus(readStatus);
                        holder.setLocation(myLocation);
                    }
                });
            }
        }).start();
    }

    /**
     * Returns the number of SMS items in the list.
     */
    @Override
    public int getItemCount() {
        return smsList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tvBody, tvDate, tvStatus, tvLocation, tvError, tvUnknownError;
        View tagsLayout;
        CardView cardViewMain,cardViewStatus,cardViewLocation;

        RecyclerView rvCompareAlerts;

        View view;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBody = itemView.findViewById(R.id.tvBody);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardViewStatus = itemView.findViewById(R.id.cardViewStatus);
            cardViewLocation = itemView.findViewById(R.id.cardViewLocation);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvError = itemView.findViewById(R.id.tvError);
            tvUnknownError = itemView.findViewById(R.id.tvUnknownError);

            tagsLayout = itemView.findViewById(R.id.tagsLayout);
            cardViewMain = itemView.findViewById(R.id.cardViewMain);

            rvCompareAlerts = itemView.findViewById(R.id.rvCompareAlerts);

            view = itemView;
        }

        public void setStatus(boolean status){
            tvStatus.setText(status ? tvStatus.getContext().getString(R.string.checked) : tvStatus.getContext().getString(R.string.check));
            if (status){
                cardViewStatus.setBackgroundTintList(ColorStateList.valueOf(cardViewStatus.getContext().getColor(R.color.successContainer)));
                tvStatus.setTextColor(ColorStateList.valueOf(tvStatus.getContext().getColor(R.color.onSuccessContainer)));
            }else {
                cardViewStatus.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(cardViewStatus, com.google.android.material.R.attr.colorSurfaceContainerHighest)));
                tvStatus.setTextColor(ColorStateList.valueOf(MaterialColors.getColor(tvStatus, com.google.android.material.R.attr.colorOnSurface)));
            }
        }

        public void setLocation(MyLocation location){
            if (location == null){
                cardViewLocation.setVisibility(View.GONE);
            }else {
                cardViewLocation.setVisibility(View.VISIBLE);
                cardViewLocation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utilities.openUrl(cardViewLocation.getContext(),"geo:"+location.lat+","+location.lng);
                    }
                });
            }
        }
    }
}
