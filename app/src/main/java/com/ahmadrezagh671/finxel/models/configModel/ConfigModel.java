package com.ahmadrezagh671.finxel.models.configModel;

import android.util.Log;

import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.models.MySMS;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.ahmadrezagh671.finxel.utilities.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central model for processing SMS messages against user-defined configuration rules.
 * Handles skip logic, field extraction via regex, date formatting, math operations,
 * and result layout rendering.
 */
public class ConfigModel {
    private static final String TAG = "ConfigModel";
    public Information information;
    public List<Field> fields;
    public List<Field> fieldsBeforeClick;
    public List<NotificationEntry> notificationEntries;
    public List<CompareWithLastMessage> compareWithLastMessage;
    public List<Skip> skips;
    public List<List<Cell>> result;
    public List<LayoutComponent> layout;

    public ConfigModel(Information information,List<Skip> skips, List<Field> fields, List<Field> fieldsBeforeClick,List<NotificationEntry> notificationEntries, List<CompareWithLastMessage> compareWithLastMessage, List<List<Cell>> result,List<LayoutComponent> layout) {
        this.information = information;
        this.skips = skips;
        this.fields = fields;
        this.fieldsBeforeClick = fieldsBeforeClick;
        this.notificationEntries = notificationEntries;
        this.compareWithLastMessage = compareWithLastMessage;
        this.result = result;
        this.layout = layout;
    }

    /**
     * Checks whether the given SMS text body matches any skip rules defined in the configuration.
     *
     * @param textBody the body text of the SMS message to evaluate
     * @return the result of the skip check from ConfigManager
     */
    public String checkSkips(String textBody){
        return ConfigManager.checkSkips(textBody,skips);
    }

    /**
     * Extracts field values from an SMS message by evaluating each configured field type.
     * Populates a map of field names to their extracted or computed values.
     *
     * @param existFields the previously extracted field values to merge with new results
     * @param sms the SMS message to extract data from
     * @param db the database instance for lookups requiring persistence
     * @param onlyBeforeClickFields if true, only processes fields defined before the click action
     * @return a map of field names to their extracted or computed string values
     */
    public Map<String, String> getFieldsResult(Map<String, String> existFields, MySMS sms, AppDatabase db, boolean onlyBeforeClickFields){
        Map<String, String> allFieldsResult = existFields == null || existFields.isEmpty() ? new HashMap<>() : existFields;

        List<Field> targetFields = onlyBeforeClickFields
                ? fieldsBeforeClick
                : new ArrayList<>() {{
            addAll(fieldsBeforeClick);
            addAll(fields);
        }};

        for (int i = 0; i < targetFields.size(); i++) {
            Field field = targetFields.get(i);

            if (allFieldsResult.containsKey(field.name))
                continue;

            String fieldResult = "";
            try {
                switch (field.type){
                    case "REGEX_FIND_SMS_BODY":
                        fieldResult = funRegexFindSmsBody(sms,field);
                        break;
                    case "SMS_SENT_DATE":
                        fieldResult = sms.getFormatedDateSent(field.format);
                        break;
                    case "SMS_DATE":
                        fieldResult = sms.getFormatedDate(field.format);
                        break;
                    case "NOW_DATE":
                        fieldResult = DateUtils.formatDate(field.format);
                        break;
                    case "SMS_NUMBER":
                        fieldResult = sms.getAddress();
                        break;
                    case "COMBINE_FIELDS":
                        fieldResult = funCombineFields(allFieldsResult,field);
                        break;
                    case "TEXT":
                        fieldResult = field.text;
                        break;
                    case "MATH_OPERATION":
                        fieldResult = funMathOperation(allFieldsResult,field);
                        break;
                    case "L_LATITUDE":
                        fieldResult = sms.getLatitude(db);
                        break;
                    case "L_LONGITUDE":
                        fieldResult = sms.getLongitude(db);
                        break;
                    case "NOTIFICATION_ENTRY_DATA":
                        fieldResult = sms.getEntryData(db,field.key);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "getFieldsResult convert: " + field.name + ": " + e.getMessage() );
            }

            allFieldsResult.put(field.name, fieldResult);
        }

        return allFieldsResult;
    }



    /**
     * Extracts a 2D list of string values from the extracted field map,
     * mapping each cell in the result layout to its corresponding field value.
     *
     * @param allFieldsResult the map of field names to extracted values
     * @return a 2D list of strings representing the formatted result rows
     */
    public List<List<String>> extractWantedResult(Map<String, String> allFieldsResult) {
        List<List<String>> itemsList2D = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            List<Cell> row = result.get(i);
            List<String> newRow = new ArrayList<>();
            for (int j = 0; j < row.size(); j++) {
                String field = row.get(j).value;
                newRow.add(allFieldsResult.get(field));
            }
            itemsList2D.add(newRow);
        }
        return itemsList2D;
    }

    private static String funCombineFields(Map<String, String> result, Field field){
        List<String> fields = field.fields;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i);
            sb.append(result.get(fieldName));
        }

        String value = sb.toString();

        // replacements_after
        value = handelReplacementsAfter(field,value);

        return value;
    }

    private String funMathOperation(Map<String, String> allFieldsResult, Field field) throws NullPointerException,ClassCastException{
        List<String> fields = field.fields;

        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < fields.size(); i++) {
            BigDecimal fieldResult = new BigDecimal(allFieldsResult.get(fields.get(i)));
            if (i==0){
                result = fieldResult;
            }else {
                switch (field.operator){
                    case "+":
                        result = result.add(fieldResult);
                        break;
                    case "-":
                        result = result.subtract(fieldResult);
                        break;
                    case "*":
                        result = result.multiply(fieldResult);
                        break;
                    case "/":
                        result = result.divide(fieldResult, 5, RoundingMode.HALF_UP);
                        break;
                }
            }
        }

        return result.stripTrailingZeros().toPlainString();
    }

    private static String funRegexFindSmsBody(MySMS sms, Field field) throws NullPointerException {
        String body = sms.getBody();

        // replacements_before
        body = handelReplacementsBefore(field,body);

        // handle flags
        Pattern pattern;
        if (!field.flags.isEmpty()){
            int flags = field.getFlagsAsInt();
            pattern = Pattern.compile(field.regex,flags);
        }else {
            pattern = Pattern.compile(field.regex);
        }

        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()){
            return null;
        }

        String regexResult = matcher.group(field.group);

        // replacements_after
        regexResult = handelReplacementsAfter(field,regexResult);

        return regexResult;
    }

    private static String handelReplacementsBefore(Field field, String value) {
        if (!field.replacementsBefore.isEmpty()){
            List<Replacement> replacements = field.replacementsBefore;
            for (int j = 0; j < replacements.size(); j++) {
                Replacement replacement = replacements.get(j);
                value = value.replaceAll(Pattern.quote(replacement.find), replacement.replace);
            }
        }
        return value;
    }

    private static String handelReplacementsAfter(Field field,String value) {
        if (!field.replacementsAfter.isEmpty()){
            List<Replacement> replacements = field.replacementsAfter;
            for (int j = 0; j < replacements.size(); j++) {
                Replacement replacement = replacements.get(j);
                value = value.replaceAll(Pattern.quote(replacement.find), replacement.replace);

            }
        }
        return value;
    }



}