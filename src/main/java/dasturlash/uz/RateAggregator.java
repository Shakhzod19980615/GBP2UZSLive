package dasturlash.uz;

import dasturlash.uz.dto.RateDto;
import dasturlash.uz.scrapers.Scraper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RateAggregator {
    private final List<Scraper> scrapers;

    public RateAggregator(List<Scraper> scrapers) {
        this.scrapers = scrapers;
    }

    public List<RateDto> collectRates() {
        return scrapers.stream()
                .map(s -> s.scrape().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
