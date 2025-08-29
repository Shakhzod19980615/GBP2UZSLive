//package dasturlash.uz.scrapers;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import dasturlash.uz.cons.Amounts;
//import dasturlash.uz.dto.RateDto;
//import org.springframework.stereotype.Service;
//
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service("transferGoScraper")
//public class TransferGoScraper implements Scraper {
//
//    private static final String API_URL =
//            "https://my.transfergo.com/api/booking/quotes?fromCurrencyCode=GBP&toCurrencyCode=UZS&fromCountryCode=GB&toCountryCode=UZ&amount=%s&calculationBase=sendAmount&business=0";
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    @Override
//    public String getProviderName() {
//        return "TransferGo";
//    }
//
//    @Override
//    public List<RateDto> scrape() {
//        List<RateDto> results = new ArrayList<>();
//
//        for (BigDecimal amount : Amounts.SUPPORTED_AMOUNTS) {
//            try {
//                String url = String.format(API_URL, amount);
//                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//                conn.setRequestMethod("GET");
//                conn.setRequestProperty("Accept", "application/json");
//                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
//
//                int responseCode = conn.getResponseCode();
//
//                if (responseCode == 429) {
//                    // Too many requests → handle Retry-After header
//                    String retryAfter = conn.getHeaderField("Retry-After");
//                    long waitSeconds = retryAfter != null ? Long.parseLong(retryAfter) : 60;
//                    System.err.println("TransferGo API rate limited. Waiting " + waitSeconds + "s...");
//                    Thread.sleep(waitSeconds * 1000);
//                    continue; // skip this amount, will try next loop
//                }
//
//                if (responseCode != 200) {
//                    System.err.println("TransferGo API error: HTTP " + responseCode);
//                    continue;
//                }
//
//                try (InputStream is = conn.getInputStream()) {
//                    JsonNode root = mapper.readTree(is);
//                    JsonNode option = root.path("options").get(0);
//
//                    if (option.isMissingNode()) {
//                        System.err.println("No options found for amount " + amount);
//                        continue;
//                    }
//
//                    BigDecimal rate = new BigDecimal(option.path("rate").path("value").asText()); // per GBP
//                    BigDecimal receiving = new BigDecimal(option.path("receivingAmount").path("value").asText()); // UZS total
//
//                    results.add(new RateDto(
//                            getProviderName(),
//                            amount.setScale(2),
//                            rate,
//                            LocalDateTime.now().withSecond(0).withNano(0),
//                            "GBP " + amount + " = " + receiving + " UZS (rate = " + rate + ")"
//                    ));
//                }
//
//                // Sleep 2 seconds between requests to avoid hitting API too fast
//                Thread.sleep(2000);
//
//            } catch (Exception e) {
//                System.err.println("TransferGo scrape failed for amount " + amount + ": " + e.getMessage());
//            }
//        }
//
//        return results;
//    }
//}
