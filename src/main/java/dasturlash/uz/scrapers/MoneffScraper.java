package dasturlash.uz.scrapers;

import dasturlash.uz.dto.RateDto;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;

@Service("moneffScraper")
public class MoneffScraper implements Scraper {

    private static final String API_URL = "https://moneff.com/landing-service-api/easytransfer/transferRateEstimation";

    @Override
    public Optional<RateDto> scrape() {
        try {
            String jsonBody = """
                {
                    "sourceCurrency": "GBP",
                    "deliveryCurrency": "UZS",
                    "fixedSide": "SOURCE",
                    "transferAmount": 10,
                    "legalEntityId": "UK"
                }
                """;

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

            return Optional.of(new RateDto("Moneff", rate, Instant.now(),
                    "1 GBP = " + rate + " UZS"));

        } catch (Exception e) {
            System.err.println("Moneff scrape failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
