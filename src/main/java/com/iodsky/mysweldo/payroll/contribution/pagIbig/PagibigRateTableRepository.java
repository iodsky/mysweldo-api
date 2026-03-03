package com.iodsky.mysweldo.payroll.contribution.pagIbig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagibigRateTableRepository extends JpaRepository<PagibigRateTable, UUID>, JpaSpecificationExecutor<PagibigRateTable> {

    @Query("SELECT p FROM PagibigRateTable p WHERE p.effectiveDate <= :date AND p.deletedAt IS NULL ORDER BY p.effectiveDate DESC LIMIT 1")
    Optional<PagibigRateTable> findLatestByEffectiveDate(@Param("date") LocalDate date);

}
