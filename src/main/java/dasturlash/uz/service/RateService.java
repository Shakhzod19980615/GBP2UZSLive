package dasturlash.uz.service;

import dasturlash.uz.dto.RateDto;
import dasturlash.uz.entity.RateEntity;
import dasturlash.uz.repository.RateRepository;
import dasturlash.uz.scrapers.MoneffScraper;
import dasturlash.uz.scrapers.Scraper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RateService {
    private final RateRepository rateRepository;
    private final List<Scraper> scrapers;

    public RateService(RateRepository rateRepository, List<Scraper> scrapers) {
        this.rateRepository = rateRepository;
        this.scrapers = scrapers;
    }

    public void saveRates(List<RateDto> rateDtos) {
        for (RateDto dto : rateDtos) {
            RateEntity entity = new RateEntity();
            entity.setProvider(dto.provider());
            entity.setAmount(dto.amount());
            entity.setRate(dto.rate());
            entity.setFetchedAt(dto.fetchedAt());
            entity.setRawValue(dto.rawValue());
            rateRepository.save(entity);
        }
    }
    public void saveAllProviders(){
        for (Scraper scraper : scrapers) {
            try {
                List<RateDto> rates = scraper.scrape();
                saveRates(rates);
            } catch (Exception e) {
                System.err.println("Failed to fetch rates from " + scraper.getProviderName());
                e.printStackTrace();
            }
        }
        System.out.println("DB saved, now count=" + rateRepository.count());
    }
   /* public List<RateDto> getLatestOnePoundRates() {
        return rateRepository.findLatestByAmount(BigDecimal.ONE)
                .stream()
                .map(this::toDto)
                .toList();
    }*/
   public List<RateDto> getLatestOnePoundRates() {
       BigDecimal onePound = BigDecimal.ONE.setScale(2); // 1.00
       var entities = rateRepository.findLatestByAmount(onePound);

       System.out.println("=== DB Fetch Results ===");
       if (entities == null || entities.isEmpty()) {
           System.out.println("No rows found for amount=1");
       } else {
           entities.forEach(e -> System.out.println(
                   "Provider=" + e.getProvider() +
                           ", Amount=" + e.getAmount() +
                           ", Rate=" + e.getRate() +
                           ", FetchedAt=" + e.getFetchedAt() +
                           ", Raw=" + e.getRawValue()
           ));
       }
       System.out.println("========================");

       return entities.stream()
               .map(this::toDto)
               .toList();
   }

   public Map<String, BigDecimal> getPreviousRate(){
       List<RateDto> latestRates = getLatestOnePoundRates();
       Map<String, BigDecimal> previousRates = new HashMap<>();
       for (RateDto r : latestRates) {
           RateEntity prev = rateRepository.findFirstByAmountAndProviderAndFetchedAtBeforeOrderByFetchedAtDesc(
                   BigDecimal.ONE.setScale(2),
                   r.provider(),
                   r.fetchedAt()
           );
           previousRates.put(r.provider(), prev != null ? prev.getRate() : r.rate());
       }
       return previousRates;
   }


    private RateDto toDto(RateEntity entity) {
        return new RateDto(
                entity.getProvider(),
                entity.getAmount(),
                entity.getRate(),
                entity.getFetchedAt(),
                entity.getRawValue()
        );
    }

}
