package dasturlash.uz;

import dasturlash.uz.dto.RateDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PollingScheduler {
    private static final Logger log = LoggerFactory.getLogger(PollingScheduler.class);
    private final RateAggregator aggregator;
    private final TelegramPublisher publisher;
    private List<RateDto> lastRates = Collections.emptyList();

    public PollingScheduler(RateAggregator aggregator, TelegramPublisher publisher) {
        this.aggregator = aggregator;
        this.publisher = publisher;
    }

    @Scheduled(cron = "${scraper.schedule.cron}")
    public void pollAndPublish() {
        var rates = aggregator.collectRates();
        if (!rates.equals(lastRates)) {
            publisher.publish(rates);
            lastRates = rates;
        } else {
            System.out.println("No change; skipping publish.");
        }
    }
}
