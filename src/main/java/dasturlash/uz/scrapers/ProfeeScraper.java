package dasturlash.uz.scrapers;

import dasturlash.uz.cons.Amounts;
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
import java.util.Scanner;

@Service("profeeScraper")
public class ProfeeScraper implements Scraper {

    private static final String API_URL = "https://terminal.profee.com/api/v2/transfer/terminal/calculation";

    @Override
    public String getProviderName() {
        return "Profee";
    }

    @Override
    public List<RateDto> scrape() {
        List<RateDto> results = new ArrayList<>();
        BigDecimal tenRate = null; // to derive £1

        for (BigDecimal amount : Amounts.SUPPORTED_AMOUNTS) {
            try {
                if (amount.compareTo(BigDecimal.ONE) == 0) {
                    continue; // derive later from £10
                }

                String jsonBody = """
                        {
                          "from": {
                            "currency": "GBP",
                            "amount": %s,
                            "country": 826
                          },
                          "to": {
                            "currency": "UZS",
                            "amount": null,
                            "country": 860
                          },
                          "skipLimitValidation": true
                        }
                        """.formatted(amount.stripTrailingZeros().toPlainString());

                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
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

                String response;
                try (Scanner scanner = new Scanner(
                        (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                                ? conn.getInputStream()
                                : conn.getErrorStream(),
                        StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    response = scanner.hasNext() ? scanner.next() : "";
                }

                // Extract rate from currencyRate
                String rateStr = response.replaceAll(
                        ".*\"currencyRate\"\\s*:\\s*\\{[^}]*\"rate\"\\s*:\\s*([0-9.]+).*", "$1");

                BigDecimal rate = null;
                if (!rateStr.equals(response)) {
                    rate = new BigDecimal(rateStr);
                }

                if (rate != null) {
                    results.add(new RateDto(
                            getProviderName(),
                            amount.setScale(2),
                            rate,
                            LocalDateTime.now().withSecond(0).withNano(0),
                            "GBP " + amount + " = " + rate.multiply(amount) + " UZS | Status: " +
                                    extractStatus(response)
                    ));
                    if (amount.compareTo(BigDecimal.TEN) == 0) {
                        tenRate = rate; // save for £1 derivation
                    }
                } else {
                    results.add(new RateDto(
                            getProviderName(),
                            amount.setScale(2),
                            null,
                            LocalDateTime.now().withSecond(0).withNano(0),
                            "GBP " + amount + " = UNAVAILABLE"
                    ));
                }

            } catch (Exception e) {
                System.err.println("Profee scrape failed for amount " + amount + ": " + e.getMessage());
            }
        }

        // Derive £1 from £10 rate
        if (tenRate != null) {
            results.add(new RateDto(
                    getProviderName(),
                    BigDecimal.ONE.setScale(2),
                    tenRate,
                    LocalDateTime.now().withSecond(0).withNano(0),
                    "GBP 1 = " + tenRate + " UZS (derived from £10 rate)"
            ));
        }

        return results;
    }

    private String extractStatus(String response) {
        String statusMsg = response.replaceAll(".*\"status\"\\s*:\\s*\\{[^}]*\"code\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        return statusMsg.equals(response) ? "UNKNOWN" : statusMsg;
    }
}
