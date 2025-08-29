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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service("paysendScraper")
public class PaysendScraper implements Scraper {

    private static final String URL =
            "https://paysend.com/api/en-gb/send-money/from-united-kingdom-to-uzbekistan?fromCurrId=826&toCurrId=860&isFrom=true";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<RateDto> scrape() {
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
                return Collections.emptyList();
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode commissionNode = root.path("commission");

            if (commissionNode.isMissingNode()) {
                System.err.println("Paysend scrape failed: commission section missing");
                return Collections.emptyList();
            }

            BigDecimal rate = commissionNode.path("convertRate").decimalValue();

            if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                System.err.println("Paysend scrape failed: invalid rate");
                return Collections.emptyList();
            }


            RateDto dto = new RateDto(
                    "Paysend",
                    BigDecimal.ONE, // always 1 GBP
                    rate,
                    LocalDateTime.now(),
                    String.format("1 GBP = %s UZS", rate)
            );
            return List.of(dto);

        } catch (Exception e) {
            System.err.println("Paysend scrape failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getProviderName() {
        return "Paysend";
    }
}
