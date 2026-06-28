package com.parentcontrolapp.agent.ui.adapters;

import com.parentcontrolapp.agent.data.model.ActivityLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimelineItem {
    public enum Type {DATE_HEADER, EVENT}

    public final Type type;
    public final String dateLabel; // для заголовка
    public final ActivityLog log;  // для события

    private TimelineItem(Type type, String dateLabel, ActivityLog log) {
        this.type = type;
        this.dateLabel = dateLabel;
        this.log = log;
    }

    public static TimelineItem dateHeader(String label) {
        return new TimelineItem(Type.DATE_HEADER, label, null);
    }

    public static TimelineItem event(ActivityLog log) {
        return new TimelineItem(Type.EVENT, null, log);
    }

    /**
     * Группирует список логов по датам с заголовками
     */
    public static List<TimelineItem> groupByDate(List<ActivityLog> logs) {
        List<TimelineItem> result = new ArrayList<>();
        String lastDate = null;

        for (ActivityLog log : logs) {
            String dateLabel = formatDateLabel(log.occurredAt);
            if (!dateLabel.equals(lastDate)) {
                result.add(dateHeader(dateLabel));
                lastDate = dateLabel;
            }
            result.add(event(log));
        }
        return result;
    }

    private static String formatDateLabel(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return "Неизвестно";

        TimeZone moscowTZ = TimeZone.getTimeZone("Europe/Moscow");

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            String cleanDate = isoTimestamp.replace("Z", "").split("\\+")[0];
            if (cleanDate.contains(".")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("."));
            }

            Date dateUtc = inputFormat.parse(cleanDate);
            if (dateUtc == null) return "Неизвестно";

            Calendar calMoscow = Calendar.getInstance(moscowTZ);
            calMoscow.setTime(dateUtc);

            Calendar todayMoscow = Calendar.getInstance(moscowTZ);

            if (calMoscow.get(Calendar.YEAR) == todayMoscow.get(Calendar.YEAR) &&
                    calMoscow.get(Calendar.DAY_OF_YEAR) == todayMoscow.get(Calendar.DAY_OF_YEAR)) {
                return "Сегодня";
            }

            todayMoscow.add(Calendar.DAY_OF_YEAR, -1);
            if (calMoscow.get(Calendar.YEAR) == todayMoscow.get(Calendar.YEAR) &&
                    calMoscow.get(Calendar.DAY_OF_YEAR) == todayMoscow.get(Calendar.DAY_OF_YEAR)) {
                return "Вчера";
            }

            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
            outputFormat.setTimeZone(moscowTZ);
            return outputFormat.format(dateUtc);

        } catch (Exception e) {
            return "Неизвестно";
        }
    }
}