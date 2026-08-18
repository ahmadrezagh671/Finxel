package com.ahmadrezagh671.finxel.models;

import android.database.Cursor;

import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.utilities.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Models an SMS message with fields parsed from the Android SMS content provider,
 * including ID, thread ID, address, subject, body, service center, and timestamps.
 */
public class MySMS {
    String id,threadId,address,subject,body,service_center;
    long date,date_sent;

    public MySMS() {
    }

    public MySMS(String id, String threadId, String address, String subject, String body, String service_center, long date, long date_sent) {
        this.id = id;
        this.threadId = threadId;
        this.address = address;
        this.subject = subject;
        this.body = body;
        this.service_center = service_center;
        this.date = date;
        this.date_sent = date_sent;
    }

    public static List<MySMS> getSmsList(Cursor cursor) {
        List<MySMS> mySMSList = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MySMS mySMS = new MySMS();
                mySMS.id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                mySMS.threadId = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"));
                mySMS.address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                mySMS.subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));
                mySMS.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                mySMS.service_center = cursor.getString(cursor.getColumnIndexOrThrow("service_center"));
                mySMS.date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                mySMS.date_sent = cursor.getLong(cursor.getColumnIndexOrThrow("date_sent"));
                mySMSList.add(mySMS);
            }
            cursor.close();
        }
        return mySMSList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getService_center() {
        return service_center;
    }

    public void setService_center(String service_center) {
        this.service_center = service_center;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate_sent() {
        return date_sent;
    }

    public void setDate_sent(long date_sent) {
        this.date_sent = date_sent;
    }

    @Override
    public String toString() {
        return "MySMS{" +
                "id='" + id + '\'' +
                ", threadId='" + threadId + '\'' +
                ", address='" + address + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", service_center='" + service_center + '\'' +
                ", date=" + date +
                ", date_sent=" + date_sent +
                '}';
    }

    public String getFormatedDate(String format) {
        return DateUtils.formatDate(new Date(getDate()),format);
    }

    public String getFormatedDateSent(String format) {
        return DateUtils.formatDate(new Date(getDate_sent()),format);
    }

    public String getLatitude(AppDatabase db) {
        SmsRecord smsRecord = db.smsRecordDao().getRecordById(getId());
        return smsRecord.getValueFromJson("lat").toString();
    }

    public String getLongitude(AppDatabase db) {
        SmsRecord smsRecord = db.smsRecordDao().getRecordById(getId());
        return smsRecord.getValueFromJson("lng").toString();
    }

    public String getEntryData(AppDatabase db, String key) {
        SmsRecord smsRecord = db.smsRecordDao().getRecordById(getId());
        return smsRecord.getValueFromJson(AppDatabase.ENTRY_START_KEY_DB + key).toString();
    }
}
