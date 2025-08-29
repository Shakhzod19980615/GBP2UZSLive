package dasturlash.uz.cons;

import java.math.BigDecimal;
import java.util.List;

public class Amounts {
    private Amounts() {}
    public static final List<BigDecimal> SUPPORTED_AMOUNTS = List.of(
            BigDecimal.ONE,
            BigDecimal.TEN,
            new BigDecimal("50"),
            new BigDecimal("100"),
            new BigDecimal("200"),
            new BigDecimal("500"),
            new BigDecimal("1000")
    );
}
