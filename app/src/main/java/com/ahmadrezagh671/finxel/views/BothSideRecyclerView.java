package com.ahmadrezagh671.finxel.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;

/**
 * A custom FrameLayout that wraps a HorizontalScrollView around a
 * RecyclerView to enable bidirectional (both-side) diagonal scrolling.
 */
public class BothSideRecyclerView extends FrameLayout {

    private HorizontalScrollView horizontalScrollView;
    private RecyclerView recyclerView;

    /**
     * Constructs a new BothSideRecyclerView with the given context.
     *
     * @param context the context
     */
    public BothSideRecyclerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructs a new BothSideRecyclerView with the given context and attributes.
     *
     * @param context the context
     * @param attrs   the attribute set
     */
    public BothSideRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructs a new BothSideRecyclerView with the given context, attributes, and style.
     *
     * @param context      the context
     * @param attrs        the attribute set
     * @param defStyleAttr the default style attribute
     */
    public BothSideRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Initializes the view hierarchy, custom attributes, and diagonal scrolling logic.
     *
     * @param context the context
     * @param attrs   the attribute set
     */
    private void init(Context context, AttributeSet attrs) {
        // 1. Create and configure views
        horizontalScrollView = new HorizontalScrollView(getContext());
        horizontalScrollView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        horizontalScrollView.setHorizontalScrollBarEnabled(false);

        recyclerView = new RecyclerView(context);
        ViewGroup.LayoutParams rvParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        // 2. Read custom XML attributes (if provided)
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BothSideRecyclerView);

            // Get values from XML, with default fallbacks
            int paddingBottom = a.getDimensionPixelSize(R.styleable.BothSideRecyclerView_rvPaddingBottom, 0);
            boolean clipToPadding = a.getBoolean(R.styleable.BothSideRecyclerView_rvClipToPadding, true);

            // Apply them to the RecyclerView
            recyclerView.setPadding(0, 0, 0, paddingBottom);
            recyclerView.setClipToPadding(clipToPadding);

            a.recycle(); // Always recycle TypedArray when done
        }

        // 3. Assemble and apply touch logic
        horizontalScrollView.addView(recyclerView, rvParams);
        addView(horizontalScrollView);
        setupDiagonalScrolling();
    }

    /**
     * Sets up diagonal scrolling by linking horizontal and vertical touch events
     * between the RecyclerView and the HorizontalScrollView.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupDiagonalScrolling() {
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            Integer lastPosition;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                        lastPosition = null;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (lastPosition == null){
                            lastPosition = (int) event.getRawX();
                        }else {
                            int movedValueX = (int)(lastPosition - event.getRawX());
                            horizontalScrollView.scrollBy(movedValueX, 0);
                            lastPosition = (int) event.getRawX();
                        }
                        break;
                }
                return false;
            }
        });

        horizontalScrollView.setOnTouchListener(new OnTouchListener() {
            Integer lastPosition;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                        lastPosition = null;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (lastPosition == null){
                            lastPosition = (int) event.getRawY();
                        }else {
                            int movedValueY = (int)(lastPosition - event.getRawY());
                            recyclerView.scrollBy(0, movedValueY);
                            lastPosition = (int) event.getRawY();
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Returns the inner RecyclerView instance.
     */
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Returns the inner HorizontalScrollView instance.
     */
    public HorizontalScrollView getHorizontalScrollView() {
        return horizontalScrollView;
    }
}