import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        LocalDate randomDate = generateRandomDate(LocalDate.of(1990, 1, 1), LocalDate.now());
        System.out.println("Random date: " + randomDate);
        
        Instant instant = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long epochTime = instant.getEpochSecond();
        System.out.println("Epoch timestamp: " + epochTime);
    }

    private static LocalDate generateRandomDate(LocalDate minDate, LocalDate maxDate) {
        long minDay = minDate.toEpochDay();
        long maxDay = maxDate.toEpochDay();
        long randomDay = minDay + new Random().nextInt((int)(maxDay - minDay));
        return LocalDate.ofEpochDay(randomDay);
    }
}
