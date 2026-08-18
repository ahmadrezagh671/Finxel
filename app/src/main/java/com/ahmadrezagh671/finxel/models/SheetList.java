package com.ahmadrezagh671.finxel.models;

import android.os.Parcelable;

import java.util.List;

/**
 * Holds CSV data along with scroll position state for restoring
 * a sheet tab's RecyclerView and horizontal scroll offset.
 */
public class SheetList {
    List<List<String>> csvList;
    Parcelable currentPosition;
    int horizontalScrollPosition;

    public SheetList(List<List<String>> csvList, Parcelable currentPosition, int horizontalScrollPosition) {
        this.currentPosition = currentPosition;
        this.csvList = csvList;
        this.horizontalScrollPosition = horizontalScrollPosition;
    }

    public List<List<String>> getCsvList() {
        return csvList;
    }

    public void setCsvList(List<List<String>> csvList) {
        this.csvList = csvList;
    }

    public Parcelable getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Parcelable currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getHorizontalScrollPosition() {
        return horizontalScrollPosition;
    }

    public void setHorizontalScrollPosition(int horizontalScrollPosition) {
        this.horizontalScrollPosition = horizontalScrollPosition;
    }
}
