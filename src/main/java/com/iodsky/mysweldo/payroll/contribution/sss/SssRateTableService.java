package com.iodsky.mysweldo.payroll.contribution.sss;

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
public class SssRateTableService {

    private final SssRateTableRepository repositiry;

    @Transactional
    public SssRateTable createSssRateTable(SssRateTableRequest request) {
        List<SssRateTable.SalaryBracket> brackets = request.getSalaryBrackets().stream()
                .map(req -> SssRateTable.SalaryBracket.builder()
                        .minSalary(req.getMinSalary())
                        .maxSalary(req.getMaxSalary())
                        .msc(req.getMsc())
                        .build())
                .collect(Collectors.toList());

        SssRateTable rateTable = SssRateTable.builder()
                .totalSss(request.getTotalSss())
                .employeeRate(request.getEmployeeRate())
                .employerRate(request.getEmployerRate())
                .salaryBrackets(brackets)
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repositiry.save(rateTable);
    }

    public Page<SssRateTable> getAllSssRateTables(
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

    public SssRateTable getSssRateTableById(UUID id) {
        return repositiry.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SSS rate table not found with ID: " + id
                ));
    }

    public SssRateTable getSssRateTableBySalaryAndDate(BigDecimal salary, LocalDate date) {
        SssRateTable config = repositiry.findLatestByEffectiveDate(date)
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
    public SssRateTable updateSssRateTable(UUID id, SssRateTableRequest request) {
        SssRateTable rateTable = getSssRateTableById(id);

        List<SssRateTable.SalaryBracket> brackets = request.getSalaryBrackets().stream()
                .map(req -> SssRateTable.SalaryBracket.builder()
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
        SssRateTable rateTable = getSssRateTableById(id);
        rateTable.setDeletedAt(Instant.now());
        repositiry.save(rateTable);
    }
}
