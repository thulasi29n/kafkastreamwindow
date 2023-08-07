import java.time.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        String epochTimeStr = generateRandomDateAndConvertToEpoch(LocalDate.of(1990, 1, 1), LocalDate.now());
        System.out.println("Epoch timestamp: " + epochTimeStr);
    }

    private static String generateRandomDateAndConvertToEpoch(LocalDate minDate, LocalDate maxDate) {
        LocalDate randomDate = generateRandomDate(minDate, maxDate);
        Instant instant = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long epochTime = instant.getEpochSecond();
        return Long.toString(epochTime);
    }

    private static LocalDate generateRandomDate(LocalDate minDate, LocalDate maxDate) {
        long minDay = minDate.toEpochDay();
        long maxDay = maxDate.toEpochDay();
        long randomDay = minDay + new Random().nextInt((int)(maxDay - minDay));
        return LocalDate.ofEpochDay(randomDay);
    }
}
