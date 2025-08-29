package dasturlash.uz.cons;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeFormatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeFormatter() {}

    // format LocalDateTime to String
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    // parse from String to LocalDateTime
    public static LocalDateTime parse(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, FORMATTER);
    }

    // now truncated to minutes
    public static LocalDateTime now() {
        return LocalDateTime.now().withSecond(0).withNano(0);
    }
}
