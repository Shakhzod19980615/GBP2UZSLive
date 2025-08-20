package dasturlash.uz;

import dasturlash.uz.dto.RateDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class PollingScheduler {

    private final RateAggregator aggregator;
    private final TelegramPublisher publisher;

    private final Map<Integer, String> lastMessageIds = new HashMap<>();
    private final Map<Integer, Map<String, BigDecimal>> lastRates = new HashMap<>();

    // Use all hours for testing
    private final List<Integer> postHours = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);

    public PollingScheduler(RateAggregator aggregator, TelegramPublisher publisher) {
        this.aggregator = aggregator;
        this.publisher = publisher;
    }

    @Scheduled(cron = "0 * * * * *")
    public void pollAndPublish() {
        int currentHour = LocalDateTime.now().getHour();
        System.out.println("===== Polling at hour: " + currentHour + " =====");

        var rates = aggregator.collectRates();
        if (rates == null || rates.isEmpty()) {
            System.out.println("No rates collected. Skipping.");
            return;
        }

        String messageId = lastMessageIds.get(currentHour);

        if (postHours.contains(currentHour)) {
            if (messageId == null) {
                // First time this hour → publish new
                messageId = publisher.publishNew(rates);
                lastMessageIds.put(currentHour, messageId);

                // Save rates for comparison
                Map<String, BigDecimal> ratesMap = rates.stream()
                        .collect(Collectors.toMap(RateDto::provider, RateDto::rate));
                lastRates.put(currentHour, ratesMap);

                System.out.println("Published new message for hour " + currentHour + ", messageId=" + messageId);
            } else {
                // Already posted this hour → update with differences
                Map<String, BigDecimal> prevRates = lastRates.get(currentHour);
                if (prevRates != null) {
                    publisher.updateMessage(messageId, rates, prevRates);

                    // Update stored rates after editing
                    Map<String, BigDecimal> ratesMap = rates.stream()
                            .collect(Collectors.toMap(RateDto::provider, RateDto::rate));
                    lastRates.put(currentHour, ratesMap);

                    System.out.println("Updated message for hour " + currentHour + ", messageId=" + messageId);
                }
            }
        } else {
            System.out.println("Not a scheduled post hour. Skipping.");
            // Non-scheduled hours → update last scheduled message
            /*Optional<Integer> lastHourOpt = postHours.stream()
                    .filter(h -> h < currentHour)
                    .max(Integer::compareTo);

            if (lastHourOpt.isPresent()) {
                int lastHour = lastHourOpt.get();
                 messageId = lastMessageIds.get(lastHour);
                Map<String, BigDecimal> prevRates = lastRates.get(lastHour);

                if (messageId != null && prevRates != null) {
                    publisher.updateMessage(messageId, rates, prevRates);

                    Map<String, BigDecimal> ratesMap = rates.stream()
                            .collect(Collectors.toMap(RateDto::provider, RateDto::rate));
                    lastRates.put(lastHour, ratesMap);

                    System.out.println("Updated last scheduled message for hour " + lastHour + ", messageId=" + messageId);
                }
            }
*/
        }

        System.out.println("===== Polling complete =====\n");
    }

}


