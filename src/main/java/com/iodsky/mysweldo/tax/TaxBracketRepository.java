package com.iodsky.mysweldo.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaxBracketRepository extends JpaRepository<TaxBracket, UUID>, JpaSpecificationExecutor<TaxBracket> {

    @Query("""
            SELECT b FROM TaxBracket b
            WHERE b.effectiveDate = (
                SELECT MAX(b2.effectiveDate)
                FROM TaxBracket b2
                WHERE b2.effectiveDate <= :date AND b2.deletedAt IS NULL
            )
            AND b.deletedAt IS NULL
            ORDER BY b.minIncome ASC
            """)
    List<TaxBracket> findAllByLatestEffectiveDate(@Param("date") LocalDate date);

    @Query("SELECT i FROM TaxBracket i WHERE i.minIncome <= :income AND (i.maxIncome IS NULL OR i.maxIncome >= :income) AND i.effectiveDate <= :date AND i.deletedAt IS NULL ORDER BY i.effectiveDate DESC LIMIT 1")
    TaxBracket findByIncomeAndEffectiveDate(
            @Param("income") BigDecimal income,
            @Param("date") LocalDate date
    );

}
