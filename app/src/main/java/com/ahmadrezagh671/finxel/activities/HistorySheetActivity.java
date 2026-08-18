package com.ahmadrezagh671.finxel.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.adapters.RVHistoryItemAdapter;
import com.ahmadrezagh671.finxel.models.HistoryItem;
import com.ahmadrezagh671.finxel.utilities.CsvManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a list of CSV history backup files with options
 * to restore, download, open, or share each file.
 */
public class HistorySheetActivity extends AppCompatActivity {
    public static final String EXTRA_ITEMS_UPDATED = "items_updated";

    private RecyclerView rvHistory;
    private RVHistoryItemAdapter adapter;
    private TextView tvNotingFound;
    private List<HistoryItem> historyItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_sheet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvHistory = findViewById(R.id.rvHistory);
        tvNotingFound = findViewById(R.id.tvNotingFound);

        loadHistoryFiles();

        adapter = new RVHistoryItemAdapter(historyItems);
        adapter.setOnItemClickListener(new RVHistoryItemAdapter.OnItemClickListener() {
            @Override
            public void onRestore(HistoryItem item, int position) {
                handleRestore(item, position);
            }

            @Override
            public void onDownload(HistoryItem item, int position) {
                handleDownload(item);
            }

            @Override
            public void onOpen(HistoryItem item, int position) {
                handleOpen(item);
            }

            @Override
            public void onShare(HistoryItem item, int position) {
                handleShare(item);
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        tvNotingFound.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Loads history CSV files, extracts bank names and timestamps, and sorts them.
     */
    private void loadHistoryFiles() {
        historyItems = new ArrayList<>();
        List<String> files = CsvManager.getHistoryFiles(this);
        for (String fileName : files) {
            long timestamp = CsvManager.extractTimestamp(fileName);
            String bankName = extractBankName(fileName);
            historyItems.add(new HistoryItem(fileName, bankName, timestamp));
        }
        historyItems.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
    }

    /**
     * Extracts the bank name from a history file name by removing the .csv extension
     * and anything after the last underscore.
     *
     * @param fileName the history file name
     * @return the extracted bank name
     */
    private String extractBankName(String fileName) {
        if (fileName.endsWith(".csv")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        int lastUnderscore = fileName.lastIndexOf("_");
        if (lastUnderscore > 0) {
            return fileName.substring(0, lastUnderscore);
        }
        return fileName;
    }

    /**
     * Restores a history file, showing a confirm dialog if existing records would be overwritten.
     *
     * @param item the history item to restore
     * @param position the position of the item in the list
     */
    private void handleRestore(HistoryItem item, int position) {
        if (CsvManager.isFileExists(this, item.getBankName())) {
            showConfirmRestoreDialog(item, position);
        } else {
            performRestore(item, position);
        }
    }

    /**
     * Shows a confirmation dialog before restoring a backup that would overwrite existing records.
     *
     * @param item the history item to restore
     * @param position the position of the item in the list
     */
    private void showConfirmRestoreDialog(HistoryItem item, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirm_restore_title))
                .setMessage(getString(R.string.confirm_restore_message))
                .setPositiveButton(getString(R.string.restore), (d, w) -> performRestore(item, position))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /**
     * Performs the actual restore operation by moving the current file to history and copying the backup back.
     *
     * @param item the history item to restore
     * @param position the position of the item in the list
     */
    private void performRestore(HistoryItem item, int position) {
        if (CsvManager.isFileExists(this, item.getBankName())){
            CsvManager.cutCsvToHistory(this,item.getBankName());
        }
        boolean success = CsvManager.restoreCsvFromHistory(this, item.getFileName());
        if (success) {
            Toast.makeText(this, R.string.file_restored_successfully, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_ITEMS_UPDATED, true));
            finish();
        } else {
            Toast.makeText(this, R.string.failed_to_restore_file, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Downloads a history CSV file to the device Downloads folder.
     *
     * @param item the history item to download
     */
    private void handleDownload(HistoryItem item) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        String downloadsFileName = item.getBankName()+ "_" + item.getTimestamp() + "_history.csv";

        File downloadsFile = new File(downloadsDir, downloadsFileName);

        File historyFile = new File(getFilesDir(), "csv" + File.separator + "history" + File.separator + item.getFileName());
        if (historyFile.exists()) {
            try {
                java.nio.file.Files.copy(historyFile.toPath(), downloadsFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Toast.makeText(this, R.string.file_downloaded_to_downloads, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Opens a history CSV file in an appropriate viewer app.
     *
     * @param item the history item to open
     */
    private void handleOpen(HistoryItem item) {
        File historyFile = new File(
                getFilesDir(),
                "csv" + File.separator + "history" + File.separator + item.getFileName()
        );

        if (!historyFile.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                historyFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/csv");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    R.string.no_app_found_to_open_file,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares a history CSV file via an implicit chooser intent.
     *
     * @param item the history item to share
     */
    private void handleShare(HistoryItem item) {
        File historyFile = new File(
                getFilesDir(),
                "csv" + File.separator + "history" + File.separator + item.getFileName()
        );

        if (!historyFile.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                historyFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.share_csv_file)));
    }
}