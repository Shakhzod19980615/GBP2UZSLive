package dasturlash.uz.scrapers;

import dasturlash.uz.dto.RateDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Scanner;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

@Service("transferGoScraper")
public class TransferGoScraper implements Scraper {

    private static final String API_URL = "https://my.transfergo.com/api/fx-rates?from=GBP&to=UZS&amount=1";

    @Override
    public Optional<RateDto> scrape() {

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("TransferGo API error: HTTP " + responseCode);
                return Optional.empty();
            }

            String response;
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                response = scanner.useDelimiter("\\A").next();
            }

            // Extract "rate" from JSON
            String rateStr = response.replaceAll(".*\"rate\"\\s*:\\s*([0-9.]+).*", "$1");
            BigDecimal rate = new BigDecimal(rateStr);

            return Optional.of(new RateDto(
                    "TransferGo",
                    rate,
                    Instant.now(),
                    "1 GBP = " + rate + " UZS"
            ));

        } catch (Exception e) {
            System.err.println("TransferGo API scrape failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
