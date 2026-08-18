package com.ahmadrezagh671.finxel.models;

import android.os.Parcelable;

import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;

import java.util.List;

/**
 * Holds a list of SMS messages along with scroll position state
 * for RecyclerView restoration, and provides lookup for the last unchecked item.
 */
public class SMSList {

    List<MySMS> smsList;
    Parcelable currentPosition;


    public SMSList(List<MySMS> smsList, Parcelable currentPosition) {
        this.smsList = smsList;
        this.currentPosition = currentPosition;
    }

    public List<MySMS> getSmsList() {
        return smsList;
    }

    public void setSmsList(List<MySMS> smsList) {
        this.smsList = smsList;
    }

    public Parcelable getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Parcelable currentPosition) {
        this.currentPosition = currentPosition;
    }

    public interface GetLastUncheckedItemResult {
        void found(int position);
        void notingFound();
    }
    public void getLastUncheckedItem(AppDatabase db, ConfigModel configModel, GetLastUncheckedItemResult resultCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < smsList.size(); i++) {
                    SmsRecord smsRecord = db.smsRecordDao().getRecordById(smsList.get(i).getId());

                    if (configModel.checkSkips(smsList.get(i).getBody()) != null){
                        continue;
                    }

                    if (smsRecord != null){
                        Object object = smsRecord.getValueFromJson("checked");
                        if (object instanceof Boolean){
                            continue;
                        } else {
                            resultCallback.found(i);
                            return;
                        }
                    }else {
                        resultCallback.found(i);
                        return;
                    }
                }
                resultCallback.notingFound();
            }
        }).start();
    }
}
