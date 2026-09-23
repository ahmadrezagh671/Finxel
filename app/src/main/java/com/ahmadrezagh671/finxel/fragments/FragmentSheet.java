package com.ahmadrezagh671.finxel.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.views.BothSideRecyclerView;
import com.ahmadrezagh671.finxel.activities.HistorySheetActivity;
import com.ahmadrezagh671.finxel.activities.MainActivity;
import com.ahmadrezagh671.finxel.adapters.RVRowCellAdapter;
import com.ahmadrezagh671.finxel.dialogs.BottomSheetDialogInputText;
import com.ahmadrezagh671.finxel.models.SheetList;
import com.ahmadrezagh671.finxel.models.configModel.Cell;
import com.ahmadrezagh671.finxel.popups.PopupSheetFunctions;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.ahmadrezagh671.finxel.utilities.CsvManager;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.ahmadrezagh671.finxel.views.VerticalZoomBar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the sheet view with tabbed CSV editing.
 * Each tab corresponds to a configuration and shows its data in a scrollable table.
 */
public class FragmentSheet extends Fragment {

    public boolean updateAvailable;

    TabLayout tabLayout;
    RecyclerView sheetRecyclerView;
    HorizontalScrollView horizontalScroll;
    SwipeRefreshLayout swipeRefreshLayout;
    MainActivity mainActivity;

    ImageButton ibMenu;
    VerticalZoomBar verticalZoomBar;

    Map<String, SheetList> sheetListDictionary = new HashMap<>();

    String lastUsedSheetList;

    ActivityResultLauncher<Intent> historySheetLauncher;

    public FragmentSheet() {
        // Required empty public constructor
    }

    public static FragmentSheet newInstance() {
        return new FragmentSheet();
    }

    /**
     * Reloads the sheet data, refreshing the CSV content and UI state.
     */
    public void reload(){
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setRefreshing(true);
        loadCsvData();

        if (mainActivity.getConfigs().isEmpty()) {
            swipeRefreshLayout.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);

            loadTabsFromConfigs(tabLayout.getSelectedTabPosition());
        }
        updateAvailable = false;
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            if (updateAvailable && swipeRefreshLayout != null){
                reload();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        historySheetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            boolean updated = result.getData().getBooleanExtra(HistorySheetActivity.EXTRA_ITEMS_UPDATED, false);
                            if (updated) {
                                mainActivity.runOnUiThread(() -> mainActivity.refreshConfigsFragments());
                            }
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sheet, container, false);

        BothSideRecyclerView bothSideRecyclerView = view.findViewById(R.id.sheetBothSideRecyclerView);
        sheetRecyclerView = bothSideRecyclerView.getRecyclerView();
        horizontalScroll = bothSideRecyclerView.getHorizontalScrollView();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        ibMenu = view.findViewById(R.id.ibMenu);

        ibMenu.setOnClickListener(this::menuClick);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabName = tab.getText().toString();

                if (!sheetListDictionary.containsKey(tabName)){
                    loadCsvData();
                }

                List<List<String>> currentCsv = sheetListDictionary.get(tabName).getCsvList();

                if (lastUsedSheetList != null && !lastUsedSheetList.isEmpty()){
                    sheetListDictionary.get(lastUsedSheetList).setCurrentPosition(sheetRecyclerView.getLayoutManager().onSaveInstanceState());
                    sheetListDictionary.get(lastUsedSheetList).setHorizontalScrollPosition(horizontalScroll.getScrollX());
                    sheetListDictionary.get(lastUsedSheetList).setZoomLevel(verticalZoomBar.getProgress());
                }

                lastUsedSheetList = tabName;

                sheetRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                Parcelable lastPosition = sheetListDictionary.get(tabName).getCurrentPosition();
                if (lastPosition != null){
                    sheetRecyclerView.getLayoutManager().onRestoreInstanceState(lastPosition);
                }

                RVRowCellAdapter rowAdapter = new RVRowCellAdapter(currentCsv, mainActivity.getConfigs().get(tabName));
                rowAdapter.setOnItemClick(new RVRowCellAdapter.OnItemClick() {
                    @Override
                    public void itemClicked(int rowPosition, int columnPosition, int inRecyclerPosition, View v, Cell currentCell) {
                        String value = currentCsv.get(rowPosition).get(columnPosition);
                        BottomSheetDialogInputText dialog = new BottomSheetDialogInputText(getActivity(), value, currentCell, new BottomSheetDialogInputText.DialogResult() {
                            @Override
                            public void close() {

                            }

                            @Override
                            public void dismiss() {

                            }

                            @Override
                            public void save(String value) {
                                currentCsv.get(rowPosition).set(columnPosition,value);
                                rowAdapter.notifyItemChanged(inRecyclerPosition);
                                CsvManager.overwriteCsv(getContext(),currentCsv,tabName);
                            }
                        });
                        dialog.show();
                    }
                });
                verticalZoomBar.setProgress(sheetListDictionary.get(tabName).getZoomLevel());
                rowAdapter.setZoomLevel(zoomProgressToZoomLevel(verticalZoomBar.getProgress()));
                sheetRecyclerView.setAdapter(rowAdapter);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        horizontalScroll.setScrollX(sheetListDictionary.get(tabName).getHorizontalScrollPosition());
                    }
                },5);  // delay this to give recycler time to load items
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        verticalZoomBar = view.findViewById(R.id.verticalZoomBar);
        verticalZoomBar.setOnZoomChangeListener(new VerticalZoomBar.OnZoomChangeListener() {
            @Override
            public void onZoomChanged(VerticalZoomBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;

                zoomHandler.removeCallbacks(zoomRunnable);
                zoomHandler.postDelayed(zoomRunnable, 80);
            }

            @Override
            public void onStartTrackingTouch(VerticalZoomBar bar) {

            }

            @Override
            public void onStopTrackingTouch(VerticalZoomBar bar) {

            }
        });

        if (mainActivity.getConfigs().isEmpty()) {
            swipeRefreshLayout.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            verticalZoomBar.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            verticalZoomBar.setVisibility(View.VISIBLE);
            loadTabsFromConfigs(0);
        }

        swipeRefreshLayout.setOnRefreshListener(this::onSwipeRefresh);

        loadCsvData();

        return view;
    }

    /**
     * Shows the sheet functions popup menu for the currently selected tab.
     *
     * @param view The anchor view for the popup menu.
     */
    private void menuClick(View view) {
        String sheetName = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
        PopupSheetFunctions.show(view, sheetName, new PopupSheetFunctions.OnSheetFunctionsClickListener() {
            @Override
            public void onCopySheetSelected() {
                String data = CsvManager.readCsvAsString(getContext(),sheetName);
                Utilities.copyToClipboard(requireContext(),"CSV Data",data);
            }

            @Override
            public void onClearSheetSelected() {
                CsvManager.cutCsvToHistory(getContext(),sheetName);
                sheetListDictionary.remove(sheetName);
                reload();
            }

            @Override
            public void onSheetHistorySelected() {
                Intent intent = new Intent(getContext(), HistorySheetActivity.class);
                historySheetLauncher.launch(intent);
            }
        });
    }

    /**
     * Populates the TabLayout with tabs from the current configuration names.
     *
     * @param selectedPosition The position of the tab to select after loading.
     */
    private void loadTabsFromConfigs(int selectedPosition) {
        List<String> names = ConfigManager.getNameOfConfigs(mainActivity.getConfigs());

        tabLayout.removeAllTabs();

        for (String title : names) {
            tabLayout.addTab(tabLayout.newTab().setText(title));
        }

        TabLayout.Tab firstTab = tabLayout.getTabAt(selectedPosition);
        if (firstTab != null) {
            firstTab.select();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Handles the swipe-to-refresh action by reloading the sheet data.
     */
    private void onSwipeRefresh() {
        reload();
    }

    /**
     * Loads CSV data for each configuration into the sheet list dictionary,
     * padding each sheet with 20 empty rows.
     */
    private void loadCsvData() {
        mainActivity.getConfigs().forEach((key,value) -> {
            List<List<String>> newData = CsvManager.readFromCsv(getContext(),key);
            int columnCount = newData.isEmpty() ? value.result.get(0).size() : newData.get(0).size();
            for (int i = 0; i < 20; i++) {
                newData.add(
                        new ArrayList<>(Collections.nCopies(columnCount, ""))
                );
            }

            sheetListDictionary.put(key,new SheetList(newData,null,0,50));
        });

    }


    private final Handler zoomHandler = new Handler(Looper.getMainLooper());
    private final Runnable zoomRunnable = new Runnable() {
        @Override
        public void run() {
            int progress = verticalZoomBar.getProgress();
            ((RVRowCellAdapter) sheetRecyclerView.getAdapter()).setZoomLevel(zoomProgressToZoomLevel(progress));
        }
    };
    private float zoomProgressToZoomLevel(int progress){
        return 1.0f + (progress - 50) * 0.01f;
    }

}
