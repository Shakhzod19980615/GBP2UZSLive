package dasturlash.uz.scrapers;

import dasturlash.uz.cons.Amounts;
import dasturlash.uz.cons.TimeFormatter;
import dasturlash.uz.dto.RateDto;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Service("moneffScraper")
public class MoneffScraper implements Scraper {

    private static final String API_URL = "https://moneff.com/landing-service-api/easytransfer/transferRateEstimation";
    @Override
    public String getProviderName() {
        return "Moneff";
    }
    @Override
    public List<RateDto> scrape() {
        List<RateDto> results = new ArrayList<>();
        BigDecimal tenRate = null;
        for(BigDecimal amount : Amounts.SUPPORTED_AMOUNTS){
        try {
            if (amount.compareTo(BigDecimal.ONE) == 0) {
                // skip API call for 1, derive later
                continue;
            }

            String jsonBody = """
                {
                    "sourceCurrency": "GBP",
                    "deliveryCurrency": "UZS",
                    "fixedSide": "SOURCE",
                    "transferAmount": %s,
                    "legalEntityId": "UK"
                }
                """.formatted(amount.stripTrailingZeros().toPlainString());

            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Origin", "https://moneff.com");
            conn.setRequestProperty("Referer", "https://moneff.com/");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            String response;
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                response = scanner.hasNext() ? scanner.next() : "";
            }

            // Extract the numeric rate
            String rateStr = response.replaceAll(".*\"sourceCurrencyRate\"\\s*:\\s*([0-9.]+).*", "$1");
            BigDecimal rate = new BigDecimal(rateStr);
            results.add(new RateDto(
                    getProviderName(),
                    amount.setScale(2),
                    rate,
                    LocalDateTime.now().withSecond(0).withNano(0),
                    "GBP " + amount + " = " + rate + " UZS"
            ));
            if (amount.compareTo(BigDecimal.TEN) == 0) {
                tenRate = rate; // save for deriving £1
            }
        } catch (Exception e) {
            System.err.println("Moneff scrape failed for amount " + amount + ": " + e.getMessage());
        }
    }
        if (tenRate != null) {
            BigDecimal oneRate = tenRate; // because rate is "per 1 GBP" already
            results.add(new RateDto("Moneff", BigDecimal.ONE.setScale(2), oneRate,
                    LocalDateTime.now().withSecond(0).withNano(0),
                    "GBP " + "1" + " = " + oneRate + " UZS"));
        } else {
            results.add(new RateDto("Moneff", BigDecimal.ONE.setScale(2), null,
                    LocalDateTime.now().withSecond(0).withNano(0),
                    "UNAVAILABLE: cannot derive (no £10 quote)"));
        }
    return results;
    }


}
