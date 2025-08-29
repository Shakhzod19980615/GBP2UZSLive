package dasturlash.uz.repository;

import dasturlash.uz.dto.RateDto;
import dasturlash.uz.entity.RateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RateRepository extends JpaRepository<RateEntity, Long> {
    @Query("""
        SELECT r FROM RateEntity r
        WHERE r.amount BETWEEN :amount AND :amount
          AND r.fetchedAt = (
              SELECT MAX(r2.fetchedAt)
              FROM RateEntity r2
              WHERE r2.provider = r.provider
                AND r2.amount BETWEEN :amount AND :amount
          )
    """)
    List<RateEntity> findLatestByAmount(@Param("amount") BigDecimal amount);

    RateEntity findFirstByAmountAndProviderAndFetchedAtBeforeOrderByFetchedAtDesc(
            @Param("amount") BigDecimal amount,
            @Param("provider") String provider,
            @Param("fetchedAt") LocalDateTime fetchedAt
    );



}
