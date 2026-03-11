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

    @Query("SELECT i FROM TaxBracket i WHERE i.effectiveDate <= :date AND i.deletedAt IS NULL ORDER BY i.minIncome ASC")
    List<TaxBracket> findAllByEffectiveDate(@Param("date") LocalDate date);

    @Query("SELECT i FROM TaxBracket i WHERE i.minIncome <= :income AND (i.maxIncome IS NULL OR i.maxIncome >= :income) AND i.effectiveDate <= :date AND i.deletedAt IS NULL ORDER BY i.effectiveDate DESC LIMIT 1")
    TaxBracket findByIncomeAndEffectiveDate(
            @Param("income") BigDecimal income,
            @Param("date") LocalDate date
    );

}
