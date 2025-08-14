package dasturlash.uz.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasturlash.uz.dto.RateDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

@Service("paysendScraper")
public class PaysendScraper implements Scraper {

    private static final String URL =
            "https://paysend.com/api/en-gb/send-money/from-united-kingdom-to-uzbekistan?fromCurrId=826&toCurrId=860&isFrom=true";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<RateDto> scrape() {
        try {
            String payload = """
                {
                    "fromCurrId": 826,
                    "toCurrId": 860,
                    "isFrom": true
                }
                """;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://paysend.com")
                    .header("Referer", "https://paysend.com/en-gb/send-money/from-united-kingdom-to-uzbekistan")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Paysend scrape failed: HTTP " + response.statusCode());
                return Optional.empty();
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode commissionNode = root.path("commission");

            if (commissionNode.isMissingNode()) {
                System.err.println("Paysend scrape failed: commission section missing");
                return Optional.empty();
            }

            BigDecimal rate = commissionNode.path("convertRate").decimalValue();

            if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                System.err.println("Paysend scrape failed: invalid rate");
                return Optional.empty();
            }

            // Create a human-readable format like "1 GBP = 16620.0970 UZS"
            String rateText = String.format("1 GBP = %s UZS", rate);

            return Optional.of(new RateDto("Paysend", rate, Instant.now(), rateText));

        } catch (Exception e) {
            System.err.println("Paysend scrape failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
