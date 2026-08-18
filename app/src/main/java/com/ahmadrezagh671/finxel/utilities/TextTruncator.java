package com.ahmadrezagh671.finxel.utilities;

/**
 * Utility for truncating text by line count or character count,
 * preserving the beginning and end of the content with an ellipsis.
 */
public class TextTruncator {


    /**
     * Truncates text to a maximum number of lines by keeping the first and last
     * lines and replacing the middle with an ellipsis.
     * @param text The input text to truncate
     * @param maxLines The maximum number of lines to keep
     * @return The truncated text, or null if input was null
     */
    public static String truncateLines(String text, int maxLines) {
        if (text == null) return null;

        String[] lines = text.split("\n", -1);
        if (lines.length <= maxLines) return text;

        int keepStart = (maxLines - 1) / 2 + (maxLines - 1) % 2; // extra line goes to start
        int keepEnd = (maxLines - 1) / 2;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keepStart; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("...\n");
        for (int i = lines.length - keepEnd; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i != lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Truncates text to a maximum number of characters by keeping the first and last
     * characters and replacing the middle with an ellipsis.
     * @param text The input text to truncate
     * @param maxChars The maximum number of characters to keep
     * @return The truncated text, or null if input was null
     */
    public static String truncateChars(String text, int maxChars) {
        if (text == null) return null;
        if (text.length() <= maxChars) return text;

        int keep = maxChars - 3; // reserve space for "..."
        int keepStart = keep / 2 + keep % 2;
        int keepEnd = keep / 2;

        String start = text.substring(0, keepStart);
        String end = text.substring(text.length() - keepEnd);

        return start + "..." + end;
    }

    /**
     * Applies line truncation first, then character truncation.
     * @param text The input text to truncate
     * @param maxLines The maximum number of lines to keep
     * @param maxChars The maximum number of characters to keep
     * @return The truncated text
     */
    public static String truncate(String text, int maxLines, int maxChars) {
        String result = truncateLines(text, maxLines);
        result = truncateChars(result, maxChars);
        return result;
    }
}