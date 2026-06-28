package com.parentcontrolapp.agent.utils;

/**
 * Утилиты для форматирования времени
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    /**
     * Форматирует минуты в читаемый вид: "45 мин", "2ч 30мин"
     */
    public static String formatMinutes(int minutes) {
        if (minutes < 0) return "0 мин";
        if (minutes < 60) return minutes + " мин";
        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours + "ч " + mins + "мин";
    }

    /**
     * Форматирует секунды в минуты (округление вверх)
     */
    public static int secondsToMinutes(long seconds) {
        return (int) (seconds / 60);
    }
}