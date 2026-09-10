package com.ahmadrezagh671.finxel.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.activities.MainActivity;
import com.ahmadrezagh671.finxel.adapters.RVSmsAdapter;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.dialogs.BottomSheetDialogItemConfirmer;
import com.ahmadrezagh671.finxel.models.MySMS;
import com.ahmadrezagh671.finxel.models.SMSList;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.popups.PopupConfigChipHold;
import com.ahmadrezagh671.finxel.popups.PopupHomeFunctions;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.ahmadrezagh671.finxel.utilities.CsvManager;
import com.ahmadrezagh671.finxel.utilities.SMSManager;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the home screen with SMS configuration chips and a message list.
 * Allows users to select a configuration, view SMS messages, and interact with items.
 */
public class FragmentHome extends Fragment {

    ImageButton ibMenu;
    ChipGroup chipGroupConfigList;
    RecyclerView rvMessages;

    TextView tvComingSoon;

    MainActivity mainActivity;

    SwipeRefreshLayout swipeRefreshMessagesLayout;


    Map<String, SMSList> smsListDictionary = new HashMap<>();
    String lastUsedSmsList = "";

    View layoutAddConfig;
    View layoutTop;
    FloatingActionButton fbMoveTop;
    Button btnAddConfig;

    TextView tvNoMessageFound;


    public FragmentHome() {
        // Required empty public constructor
    }

    public static FragmentHome newInstance() {
        FragmentHome fragment = new FragmentHome();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        layoutTop = view.findViewById(R.id.layoutTop);

        fbMoveTop = view.findViewById(R.id.fbMoveTop);

        rvMessages = view.findViewById(R.id.rvMessages);
        ibMenu = view.findViewById(R.id.ibMenu);
        chipGroupConfigList = view.findViewById(R.id.chipGroupConfigList);
        swipeRefreshMessagesLayout = view.findViewById(R.id.swipeRefreshMessagesLayout);
        tvComingSoon = view.findViewById(R.id.tvComingSoon);

        tvNoMessageFound = view.findViewById(R.id.tvNoMessageFound);
        tvNoMessageFound.setVisibility(GONE);

        layoutAddConfig = view.findViewById(R.id.layoutAddConfig);
        btnAddConfig = view.findViewById(R.id.btnAddConfig);

        start();

        swipeRefreshMessagesLayout.setOnRefreshListener(this::onRefresh);
        btnAddConfig.setOnClickListener( v -> mainActivity.showImportConfigDialog());

        fbMoveTop.setOnClickListener(this::moveTop);

        ibMenu.setOnClickListener(this::menuClick);

        setRvMessagesOnScrollFunction();


        return view;
    }

    /**
     * Shows the home functions popup menu with options for settings,
     * scrolling to top, finding the last unchecked item, and exiting.
     *
     * @param view The anchor view for the popup menu.
     */
    private void menuClick(View view) {
        PopupHomeFunctions.show(view, new PopupHomeFunctions.OnHomeFunctionsClickListener() {
            @Override
            public void onSettingsClicked() {
                mainActivity.buttonNavView.setSelectedItemId(R.id.menuSettings);
            }

            @Override
            public void onMoveTopClicked() {
                if (rvMessages != null && rvMessages.getAdapter() != null)
                    rvMessages.smoothScrollToPosition(0);
            }

            @Override
            public void onLastUncheckedClicked() {
                if (rvMessages != null && rvMessages.getAdapter() != null){
                    String selectedChip = Utilities.getSelectedChipText(chipGroupConfigList);
                    smsListDictionary.get(selectedChip).getLastUncheckedItem(mainActivity.db, mainActivity.getConfigs().get(selectedChip), new SMSList.GetLastUncheckedItemResult() {
                        @Override
                        public void found(int position) {
                            rvMessages.smoothScrollToPosition(position);
                        }
                        @Override
                        public void notingFound() {
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mainActivity, getString(R.string.no_unchecked_item_found), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onExitClicked() {
                mainActivity.finish();
            }
        });
    }

    /**
     * Scrolls the message list to the top and hides the floating action button.
     *
     * @param view The view that triggered this action.
     */
    private void moveTop(View view) {
        rvMessages.smoothScrollToPosition(0);
        fbMoveTop.setVisibility(GONE);
    }

    /**
     * Sets up a scroll listener on the message RecyclerView to show or hide
     * the top bar and floating action button based on scroll direction and position.
     */
    private void setRvMessagesOnScrollFunction() {
        final int[] recyclerState = new int[1];
        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                recyclerState[0] = newState;
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int firstVisibleItem = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

                if(firstVisibleItem < 5){
                    layoutTop.setVisibility(VISIBLE);
                    fbMoveTop.setVisibility(GONE);
                } else if (dy > 50){
                    if (recyclerState[0] == 0 || recyclerState[0] == 2){
                        layoutTop.setVisibility(GONE);
                        fbMoveTop.setVisibility(GONE);
                    }
                }else if (dy < -160){
                    if (recyclerState[0] == 0 || recyclerState[0] == 2){
                        layoutTop.setVisibility(VISIBLE);
                        if(firstVisibleItem > 10){
                            fbMoveTop.setVisibility(VISIBLE);
                        }
                    }
                }
            }
        });

        chipGroupConfigList.setOnClickListener(v -> layoutTop.setVisibility(VISIBLE));
    }

    /**
     * Initializes the fragment UI based on whether configurations exist.
     * Shows the import dialog if no configs are available, otherwise loads the chip list.
     */
    public void start(){
        if (mainActivity.getConfigs().isEmpty()) {
            mainActivity.showImportConfigDialog();
            rvMessages.setVisibility(GONE);
            swipeRefreshMessagesLayout.setVisibility(GONE);
            chipGroupConfigList.setVisibility(GONE);
            layoutAddConfig.setVisibility(VISIBLE);
        } else {
            rvMessages.setVisibility(VISIBLE);
            swipeRefreshMessagesLayout.setVisibility(VISIBLE);
            chipGroupConfigList.setVisibility(VISIBLE);
            layoutAddConfig.setVisibility(GONE);
            loadChipsFromConfigs(null);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }


    /**
     * Opens the import configuration dialog when the add config chip is clicked.
     *
     * @param view The view that triggered this action.
     */
    private void addConfigClick(View view) {
        mainActivity.showImportConfigDialog();
    }

    /**
     * Handles chip selection changes to switch the active SMS list tab.
     *
     * @param group The ChipGroup containing the checked chips.
     * @param checkedIds The list of checked chip view IDs.
     */
    public void onChipsCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
        if (!checkedIds.isEmpty()) {
            // Since singleSelection="true", checkedIds will only have 1 item
            int checkedId = checkedIds.get(0);
            Chip selectedChip = group.findViewById(checkedId);

            if (selectedChip == null) {
                return;
            }

            String chipKey = selectedChip.getText().toString();

            if (chipKey.equals(getString(R.string.all_messages))){
                tvComingSoon.setVisibility(VISIBLE);
                rvMessages.setVisibility(GONE);
                swipeRefreshMessagesLayout.setVisibility(GONE);
                tvNoMessageFound.setVisibility(GONE);
                return;
            }

            // save last sms list position
            if (!lastUsedSmsList.isEmpty()){
                smsListDictionary.get(lastUsedSmsList).setCurrentPosition(rvMessages.getLayoutManager().onSaveInstanceState());
            }

            lastUsedSmsList = chipKey;

            tvComingSoon.setVisibility(GONE);
            rvMessages.setVisibility(VISIBLE);
            swipeRefreshMessagesLayout.setVisibility(VISIBLE);

            reload(false);
        }
    }

    /**
     * Handles an SMS item click by showing a detail dialog and saving the checked status.
     *
     * @param existFields The existing fields map for the SMS item.
     * @param mySMS The SMS message that was clicked.
     * @param configModel The configuration model used to extract fields.
     * @param position The adapter position of the clicked item.
     */
    private void onItemClicked(Map<String, String> existFields,MySMS mySMS, ConfigModel configModel, int position) {
        rvMessages.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> fieldsResult = configModel.getFieldsResult(existFields,mySMS,mainActivity.db,false);

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BottomSheetDialogItemConfirmer dialog = new BottomSheetDialogItemConfirmer(getActivity(), fieldsResult, configModel.layout, new BottomSheetDialogItemConfirmer.DialogResult() {
                            @Override
                            public void close() {
                            }

                            @Override
                            public void dismiss() {
                                rvMessages.setEnabled(true);
                            }

                            @Override
                            public void submit(Map<String, String> submittedText) {
                                fieldsResult.putAll(submittedText);

                                List<List<String>> result = configModel.extractWantedResult(fieldsResult);

                                CsvManager.appendToCsv(getContext(),result,configModel.information.name);
                                mainActivity.fragmentSheet.updateAvailable = true;

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SmsRecord smsRecord = mainActivity.db.smsRecordDao().getRecordById(mySMS.getId());
                                        if (smsRecord == null){
                                            smsRecord = new SmsRecord(mySMS.getId(),null);
                                        }
                                        boolean saved = smsRecord.addValueToJson("checked",true);
                                        if (!saved){
                                            Toast.makeText(mainActivity, getString(R.string.failed_to_save_checked_status), Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        mainActivity.db.smsRecordDao().insertOrUpdate(smsRecord);
                                        rvMessages.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                rvMessages.getAdapter().notifyItemChanged(position);
                                            }
                                        });
                                    }
                                }).start();

                            }
                        });
                        dialog.show();
                    }
                });
            }
        }).start();
    }

    public void onRefresh() {
        reload(true);
    }

    /**
     * Reloads the active SMS list data and updates the RecyclerView adapter.
     *
     * @param force If true, forces a reload from the data source even if cached.
     */
    public void reload(boolean force){
        String selectedChip = Utilities.getSelectedChipText(chipGroupConfigList);

        if (!force && smsListDictionary.containsKey(selectedChip)){
            rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
            rvMessages.setAdapter(new RVSmsAdapter(mainActivity.db,smsListDictionary.get(selectedChip).getSmsList(),mainActivity.getConfigs().get(selectedChip),this::onItemClicked));
            rvMessages.getLayoutManager().onRestoreInstanceState(smsListDictionary.get(selectedChip).getCurrentPosition());
        }else {
            ConfigModel currentConfigModel = mainActivity.getConfigs().get(selectedChip);
            if (currentConfigModel != null){
                List<MySMS> newSmsList= SMSManager.loadSms(getContext(),currentConfigModel.information.provider);
                smsListDictionary.put(selectedChip,new SMSList(newSmsList,null));
                rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
                rvMessages.setAdapter(new RVSmsAdapter(mainActivity.db,smsListDictionary.get(selectedChip).getSmsList(),mainActivity.getConfigs().get(selectedChip),this::onItemClicked));
            }else {
                rvMessages.setAdapter(null);
            }
        }

        tvNoMessageFound.setVisibility(smsListDictionary.get(selectedChip).getSmsList().isEmpty() ? VISIBLE : GONE);

        swipeRefreshMessagesLayout.setRefreshing(false);
    }


    /**
     * Populates the ChipGroup with configuration names and the "All Messages" option.
     *
     * @param selectedItem The text of the chip to select after loading, or null to select the first config.
     */
    public void loadChipsFromConfigs(String selectedItem){
        List<String> names = ConfigManager.getNameOfConfigs(mainActivity.getConfigs());
        names.add(0,getString(R.string.all_messages));
        names.add(getString(R.string.add_config));

        chipGroupConfigList.removeAllViews();

        populateChips(names);

        chipGroupConfigList.setOnCheckedStateChangeListener(this::onChipsCheckedChanged);

        if (selectedItem == null) {
            selectChipByText(names.get(1));
        }else {
            selectChipByText(selectedItem);
        }
    }
    /**
     * Creates and adds Chip views for each configuration item to the ChipGroup.
     *
     * @param items The list of chip labels to create.
     */
    private void populateChips(List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);

            Chip chip = new Chip(getContext());

            ChipDrawable choiceChipDrawable = ChipDrawable.createFromAttributes(
                    getContext(),
                    null,
                    0,
                    com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice
            );
            chip.setChipDrawable(choiceChipDrawable);

            chip.setText(item);
            chip.setTag(item);

            chip.setCheckable(true);
            chip.setClickable(true);

            chip.setId(View.generateViewId());

            chip.setOnLongClickListener(this::onChipsHold);

            chip.setOnClickListener(v -> layoutTop.setVisibility(VISIBLE));

            if (i == items.size() - 1) {
                Drawable addIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_add);
                chip.setChipIcon(addIcon);

                chip.setChipIconVisible(true);

                chip.setChipIconTint(chip.getTextColors());

                chip.setClickable(true);
                chip.setCheckable(false);

                int iconSizePx = (int) (24 * getContext().getResources().getDisplayMetrics().density);
                chip.setChipIconSize(iconSizePx);

                int paddingPx = (int) (4 * getContext().getResources().getDisplayMetrics().density);
                chip.setIconStartPadding(paddingPx);

                chip.setOnClickListener(this::addConfigClick);
            }

            chipGroupConfigList.addView(chip);
        }
    }



    /**
     * Shows the configuration chip hold popup menu on long press,
     * allowing the user to edit or delete the configuration.
     *
     * @param view The view that was long-pressed.
     * @return true if the long press was handled, false otherwise.
     */
    private boolean onChipsHold(View view) {
        Chip chip = (Chip) view;
        String chipText = chip.getTag().toString();
        if (chipText.equals(getString(R.string.add_config)) || chipText.equals(getString(R.string.all_messages))) {
            return false;
        }
        PopupConfigChipHold.show(view, new PopupConfigChipHold.OnConfigMenuClickListener() {
            @Override
            public void onEditConfigSelected() {
                mainActivity.editConfig(chipText);
            }

            @Override
            public void onDeleteConfigSelected() {
                mainActivity.deleteConfig(chipText);
            }

            @Override
            public void onShareConfigSelected() {
                mainActivity.shareConfig(chipText);
            }
        });
        return true;
    }

    /**
     * Selects a chip in the ChipGroup by matching its text tag.
     *
     * @param targetText The text of the chip to select.
     */
    private void selectChipByText(String targetText) {
        Chip chip = chipGroupConfigList.findViewWithTag(targetText);
        if (chip != null) {
            chip.setChecked(true);
        }
    }

}