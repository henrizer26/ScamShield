package com.example.scamshield.utils;

import android.content.Context;
import com.example.scamshield.R;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Turns the stored {@code yyyy-MM-dd HH:mm:ss} stamp into the wording the
 * reference uses: "Today / Yesterday / 25 Aug 2026" beside a clock time.
 */
public final class DateFormats {

    private static final String STORED_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private DateFormats() {
    }

    private static SimpleDateFormat stored() {
        return new SimpleDateFormat(STORED_PATTERN, Locale.US);
    }

    public static Date parse(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            return stored().parse(stored);
        } catch (ParseException e) {
            return null;
        }
    }

    /** "Today", "Yesterday", or "25 Aug 2026". */
    public static String dayLabel(Context context, String storedValue) {
        Date date = parse(storedValue);
        if (date == null) {
            return storedValue == null ? "" : storedValue;
        }

        Calendar then = Calendar.getInstance();
        then.setTime(date);
        Calendar now = Calendar.getInstance();

        if (isSameDay(then, now)) {
            return context.getString(R.string.date_today);
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(then, now)) {
            return context.getString(R.string.date_yesterday);
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    /** "10:30 AM". */
    public static String timeLabel(String storedValue) {
        Date date = parse(storedValue);
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    /** "28 Aug 2026, 10:30 AM" - the detail and review screens. */
    public static String full(String storedValue) {
        Date date = parse(storedValue);
        if (date == null) {
            return storedValue == null ? "" : storedValue;
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date);
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
