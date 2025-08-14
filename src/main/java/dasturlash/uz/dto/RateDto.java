package dasturlash.uz.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RateDto(String provider, BigDecimal rate, Instant fetchedAt, String rawValue) {}
