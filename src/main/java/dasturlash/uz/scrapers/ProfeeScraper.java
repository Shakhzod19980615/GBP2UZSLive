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

@Service("profeeScraper")
public class ProfeeScraper implements Scraper {

    private static final String API_URL = "https://terminal.profee.com/api/v2/transfer/terminal/calculation";

    @Override
    public Optional<RateDto> scrape() {
        HttpURLConnection conn = null;
        try {
            String jsonBody = """
                {
                    "from": {
                        "currency": "GBP",
                        "amount": 10,
                        "country": 826
                    },
                    "to": {
                        "currency": "UZS",
                        "amount": null,
                        "country": 860
                    },
                    "skipLimitValidation": true
                }
                """;

            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Origin", "https://profee.com");
            conn.setRequestProperty("Referer", "https://profee.com/");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String response;
            if (responseCode == 200 || responseCode == 201) {
                try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    response = scanner.hasNext() ? scanner.next() : "";
                }
            } else {
                try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    response = scanner.hasNext() ? scanner.next() : "";
                }
                System.err.println("Profee scrape failed: HTTP " + responseCode + " → " + response);
                return Optional.empty();
            }

            // Extract the numeric rate from the JSON
            String rateStr = response.replaceAll(".*\"rate\"\\s*:\\s*([0-9.]+).*", "$1");
            BigDecimal rate = new BigDecimal(rateStr);

            return Optional.of(new RateDto("Profee", rate, Instant.now(),
                    "1 GBP = " + rate + " UZS"));

        } catch (Exception e) {
            System.err.println("Profee scrape failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
