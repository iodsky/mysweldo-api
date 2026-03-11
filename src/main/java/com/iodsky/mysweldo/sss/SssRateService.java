package com.iodsky.mysweldo.sss;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SssRateService {

    private final SssRateRepository repositiry;

    @Transactional
    public SssRate createSssRateTable(SssRateRequest request) {
        List<SssRate.SalaryBracket> brackets = request.getSalaryBrackets().stream()
                .map(req -> SssRate.SalaryBracket.builder()
                        .minSalary(req.getMinSalary())
                        .maxSalary(req.getMaxSalary())
                        .msc(req.getMsc())
                        .build())
                .collect(Collectors.toList());

        SssRate rateTable = SssRate.builder()
                .totalSss(request.getTotalSss())
                .employeeRate(request.getEmployeeRate())
                .employerRate(request.getEmployerRate())
                .salaryBrackets(brackets)
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repositiry.save(rateTable);
    }

    public Page<SssRate> getAllSssRateTables(
            int page, int limit, LocalDate effectiveDate) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "effectiveDate"));

        return repositiry.findAll((root, query, cb) -> {
            var predicates = cb.conjunction();

            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));

            if (effectiveDate != null) {
                predicates = cb.and(predicates, cb.equal(root.get("effectiveDate"), effectiveDate));
            }

            return predicates;
        }, pageable);
    }

    public SssRate getSssRateTableById(UUID id) {
        return repositiry.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SSS rate table not found with ID: " + id
                ));
    }

    public SssRate getSssRateTableBySalaryAndDate(BigDecimal salary, LocalDate date) {
        SssRate config = repositiry.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No SSS rate table found for date: " + date
                ));

        // Verify the salary falls within one of the brackets
        try {
            config.findBracket(salary);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No SSS bracket found for salary: " + salary + " on date: " + date
            );
        }

        return config;
    }

    @Transactional
    public SssRate updateSssRateTable(UUID id, SssRateRequest request) {
        SssRate rateTable = getSssRateTableById(id);

        List<SssRate.SalaryBracket> brackets = request.getSalaryBrackets().stream()
                .map(req -> SssRate.SalaryBracket.builder()
                        .minSalary(req.getMinSalary())
                        .maxSalary(req.getMaxSalary())
                        .msc(req.getMsc())
                        .build())
                .collect(Collectors.toList());

        rateTable.setTotalSss(request.getTotalSss());
        rateTable.setEmployeeRate(request.getEmployeeRate());
        rateTable.setEmployerRate(request.getEmployerRate());
        rateTable.setSalaryBrackets(brackets);
        rateTable.setEffectiveDate(request.getEffectiveDate());

        return repositiry.save(rateTable);
    }

    @Transactional
    public void deleteSssRateTable(UUID id) {
        SssRate rateTable = getSssRateTableById(id);
        rateTable.setDeletedAt(Instant.now());
        repositiry.save(rateTable);
    }
}
