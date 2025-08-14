package dasturlash.uz.scrapers;

import dasturlash.uz.dto.RateDto;

import java.util.Optional;

public interface Scraper {
    Optional<RateDto> scrape();

   // Optional<BigDecimal> scrapeTransferFee();
}
