package com.ahmadrezagh671.finxel.utilities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for reading, writing, and managing CSV files in the app's internal storage.
 * Supports append, read, overwrite, history archival, and restoration operations.
 */
public class CsvManager {

    private static final String FOLDER_NAME = "csv";

    /**
     * Returns the File object for a CSV file in the app's internal "csv" directory,
     * creating the directory if needed.
     * @param context Application context
     * @param filename The name of the file (without .csv extension)
     * @return The File object for the CSV file
     */
    private static File getCsvFile(Context context, String filename) {
        File csvDir = new File(context.getFilesDir(), FOLDER_NAME);
        if (!csvDir.exists()) {
            csvDir.mkdirs(); // Creates the 'csv' directory if it doesn't exist
        }
        return new File(csvDir, filename + ".csv");
    }

    /**
     * Appends rows of data to a CSV file, creating it if it doesn't exist.
     * @param context Application context
     * @param data List of rows, where each row is a List of Strings
     * @param filename The name of the CSV file (without .csv extension)
     */
    public static void appendToCsv(Context context, List<List<String>> data,String filename) {
        if (data == null || data.isEmpty()) return;

        File csvFile = getCsvFile(context,filename);

        // 'true' in FileWriter enables append mode
        try (FileWriter writer = new FileWriter(csvFile, true);
             BufferedWriter bw = new BufferedWriter(writer)) {

            for (List<String> row : data) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    sb.append(escapeCsvField(row.get(i)));
                    if (i < row.size() - 1) {
                        sb.append(","); // Separate columns with a comma
                    }
                }
                bw.write(sb.toString());
                bw.newLine(); // Move to the next row
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a CSV file and returns its contents as a list of rows.
     * Returns an empty list if the file doesn't exist.
     * @param context Application context
     * @param filename The name of the CSV file (without .csv extension)
     * @return A list of rows, where each row is a List of Strings
     */
    public static List<List<String>> readFromCsv(Context context,String filename) {
        List<List<String>> data = new ArrayList<>();
        File csvFile = getCsvFile(context,filename);

        // If the file doesn't exist yet, return the empty list gracefully
        if (!csvFile.exists()) {
            return data;
        }

        try (FileReader reader = new FileReader(csvFile);
             BufferedReader br = new BufferedReader(reader)) {

            String line;
            while ((line = br.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                data.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Reads a CSV file and returns its contents as a tab-separated string.
     * Returns an empty string if the file doesn't exist.
     * @param context Application context
     * @param filename The name of the CSV file (without .csv extension)
     * @return The file contents as a string with tab-separated columns
     */
    public static String readCsvAsString(Context context, String filename) {
        StringBuilder stringBuilder = new StringBuilder();
        File csvFile = getCsvFile(context, filename);

        // If the file doesn't exist yet, return an empty string gracefully
        if (!csvFile.exists()) {
            return "";
        }

        try (FileReader reader = new FileReader(csvFile);
             BufferedReader br = new BufferedReader(reader)) {

            String line;
            while ((line = br.readLine()) != null) {
                // Parse the line using your existing method to handle CSV formatting securely
                List<String> row = parseCsvLine(line);

                // Join the items with a tab (\t) and append a newline (\n) at the end of the row
                stringBuilder.append(String.join("\t", row)).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    /**
     * Moves a CSV file from the main directory to the "csv/history" subdirectory
     * with a millisecond timestamp appended to the filename.
     * @param context Application context
     * @param filename The name of the file to move (without .csv extension)
     * @return true if the file was successfully moved, false otherwise
     */
    public static boolean cutCsvToHistory(Context context, String filename) {
        // 1. Get the source file and ensure it actually exists
        File sourceFile = getCsvFile(context, filename);
        if (!sourceFile.exists()) {
            return false;
        }

        // 2. Create the 'csv/history' sub-directory if it doesn't exist
        File historyDir = new File(context.getFilesDir(), FOLDER_NAME + File.separator + "history");
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }

        // 3. Generate the destination file with the timestamp
        long timestamp = System.currentTimeMillis();
        String historyFilename = filename + "_" + timestamp + ".csv";
        File destFile = new File(historyDir, historyFilename);

        // 4. Perform the "cut" operation
        return sourceFile.renameTo(destFile);
    }

    /**
     * Overwrites the entire CSV file with new data. Useful for editing or deleting rows.
     * @param context Application context
     * @param data List of rows to write
     * @param filename The name of the CSV file (without .csv extension)
     */
    public static void overwriteCsv(Context context, List<List<String>> data,String filename) {
        File csvFile = getCsvFile(context,filename);

        List<List<String>> dataCopy = new ArrayList<>(data);

        removeTrailingEmptyRows(dataCopy);

        // 'false' or omitting the second argument overwrites the file entirely
        try (FileWriter writer = new FileWriter(csvFile, false);
             BufferedWriter bw = new BufferedWriter(writer)) {

            for (List<String> row : dataCopy) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    sb.append(escapeCsvField(row.get(i)));
                    if (i < row.size() - 1) {
                        sb.append(",");
                    }
                }
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escapes a CSV field by wrapping it in quotes if it contains commas, quotes, or newlines.
     * @param field The raw field value
     * @return The properly escaped CSV field
     */
    private static String escapeCsvField(String field) {
        if (field == null) return "";
        // If the text contains a comma, quote, or newline, wrap it in quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Parses a single CSV line into a list of tokens, handling quoted fields
     * and escaped quotes inside quoted fields.
     * @param line A single line from a CSV file
     * @return A list of parsed field values
     */
    private static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote ("") inside a quoted field -> literal "
                    sb.append('"');
                    i++; // skip the second quote, we've already consumed the pair
                } else {
                    // Either entering or leaving a quoted section
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0); // Clear buffer for next column
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString()); // Add the last column
        return tokens;
    }

    /**
     * Removes trailing empty rows from the end of a matrix.
     * A row is considered empty if it is null, empty, or contains only empty strings.
     * @param matrix The list of rows to trim
     */
    public static void removeTrailingEmptyRows(List<List<String>> matrix) {
        if (matrix == null || matrix.isEmpty()) {
            return;
        }

        // Start from the very last row and move backwards
        for (int i = matrix.size() - 1; i >= 0; i--) {
            List<String> row = matrix.get(i);

            if (isRowEmpty(row)) {
                matrix.remove(i); // Safely removes the trailing row
            } else {
                // The moment we hit a row with data , we stop!
                break;
            }
        }
    }

    // Helper method to check if a row is completely empty
    private static boolean isRowEmpty(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }

        for (String item : row) {
            // If we find even one non-empty string, the row is NOT empty
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }
        return true; // All items were null or ""
    }

    /**
     * Restores a CSV file from the history directory back to the main csv directory.
     * If a file with the same name already exists, it will be deleted first (overwritten).
     * @param context Application context
     * @param historyFilename The filename in history (with timestamp, e.g., "Bank_1234567890.csv")
     * @return true if restored successfully, false if file not found
     */
    public static boolean restoreCsvFromHistory(Context context, String historyFilename) {
        File historyDir = new File(context.getFilesDir(), FOLDER_NAME + File.separator + "history");
        File historyFile = new File(historyDir, historyFilename);

        if (!historyFile.exists()) {
            return false;
        }

        String originalName = extractOriginalName(historyFilename);
        File targetFile = getCsvFile(context, originalName);

        // Delete existing file if present (overwrite)
        if (targetFile.exists()) {
            targetFile.delete();
        }

        return historyFile.renameTo(targetFile);
    }


    /**
     * Checks if a CSV file exists in the main csv directory.
     * @param context Application context
     * @param filename The name of the file (without .csv extension)
     * @return true if the file exists
     */
    public static boolean isFileExists(Context context, String filename) {
        File csvFile = getCsvFile(context, filename);
        return csvFile.exists();
    }

    /**
     * Returns a list of all CSV filenames in the history directory.
     * @param context Application context
     * @return A list of history CSV filenames
     */
    public static List<String> getHistoryFiles(Context context) {
        List<String> historyFiles = new ArrayList<>();
        File historyDir = new File(context.getFilesDir(), FOLDER_NAME + File.separator + "history");

        if (historyDir.exists() && historyDir.isDirectory()) {
            File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    historyFiles.add(file.getName());
                }
            }
        }

        return historyFiles;
    }

    /**
     * Extracts the original name from a history filename by stripping the timestamp.
     * E.g., "Bank_1234567890.csv" -> "Bank"
     * @param historyFilename The history filename
     * @return The original base name
     */
    private static String extractOriginalName(String historyFilename) {
        if (historyFilename.endsWith(".csv")) {
            historyFilename = historyFilename.substring(0, historyFilename.length() - 4);
        }
        int lastUnderscore = historyFilename.lastIndexOf("_");
        if (lastUnderscore > 0) {
            return historyFilename.substring(0, lastUnderscore);
        }
        return historyFilename;
    }

    /**
     * Extracts the timestamp from a history filename.
     * E.g., "Bank_1234567890.csv" -> 1234567890
     * @param historyFilename The history filename
     * @return The parsed timestamp, or 0 if parsing fails
     */
    public static long extractTimestamp(String historyFilename) {
        if (historyFilename.endsWith(".csv")) {
            historyFilename = historyFilename.substring(0, historyFilename.length() - 4);
        }
        int lastUnderscore = historyFilename.lastIndexOf("_");
        if (lastUnderscore > 0 && lastUnderscore < historyFilename.length() - 1) {
            try {
                return Long.parseLong(historyFilename.substring(lastUnderscore + 1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}