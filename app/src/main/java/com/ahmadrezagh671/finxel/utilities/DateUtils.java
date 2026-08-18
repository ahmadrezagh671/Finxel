package com.ahmadrezagh671.finxel.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for formatting dates using SimpleDateFormat.
 * Provides overloaded convenience methods for common formatting scenarios.
 */
public class DateUtils {
    /**
     * Formats a Date object into a string using the given pattern.
     * @param date The date to format
     * @param format The SimpleDateFormat pattern string
     * @return The formatted date string
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }
    /**
     * Formats a timestamp (milliseconds since epoch) into a string using the given pattern.
     * @param date The timestamp to format
     * @param format The SimpleDateFormat pattern string
     * @return The formatted date string
     */
    public static String formatDate(Long date, String format) {
        return formatDate(new Date(date),format);
    }
    /**
     * Formats the current time into a string using the given pattern.
     * @param format The SimpleDateFormat pattern string
     * @return The formatted date string
     */
    public static String formatDate(String format) {
        return formatDate(System.currentTimeMillis(),format);
    }
}
