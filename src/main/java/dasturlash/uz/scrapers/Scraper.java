package dasturlash.uz.scrapers;

import dasturlash.uz.dto.RateDto;

import java.util.List;
import java.util.Optional;

public interface Scraper {
    List<RateDto> scrape();
    String getProviderName();
   // Optional<BigDecimal> scrapeTransferFee();
}
