package dasturlash.uz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record RateDto(String provider,
                      BigDecimal amount,
                      BigDecimal rate,
                      LocalDateTime fetchedAt,
                      String rawValue) {}
