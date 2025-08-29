package dasturlash.uz.scrapers;

import dasturlash.uz.dto.RateDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service("mockTransferGoScraper")
public class MockTransferGoScraper implements Scraper {

    private final Random random = new Random();

    @Override
    public List<RateDto> scrape() {
        List<RateDto> rates = new ArrayList<>();

        // Base mock rate for 1 GBP
        BigDecimal baseRate = new BigDecimal("16369.27");

        // Optional: add a tiny random delta to simulate live fluctuation
        BigDecimal fluctuation = BigDecimal.valueOf(random.nextDouble() * 5 - 2.5); // ±2.5
        BigDecimal currentRate = baseRate.add(fluctuation).setScale(2, BigDecimal.ROUND_HALF_UP);

        rates.add(new RateDto(
                getProviderName(),
                BigDecimal.ONE,
                currentRate,
                LocalDateTime.now().withSecond(0).withNano(0),
                "GBP 1 = " + currentRate + " UZS (mock)"
        ));

        return rates;
    }

    @Override
    public String getProviderName() {
        return "TransferGo";
    }
}
