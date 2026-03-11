package com.iodsky.mysweldo.pagIbig;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagibigRateService {

    private final PagibigRateRepository pagibigRateTableRepository;

    @Transactional
    public PagibigRate createPagibigRateTable(PagibigRateRequest request) {
        if (pagibigRateTableRepository.findLatestByEffectiveDate(request.getEffectiveDate()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pag-IBIG rate table already exists for effective date: " + request.getEffectiveDate()
            );
        }

        PagibigRate rateTable = PagibigRate.builder()
                .employeeRate(request.getEmployeeRate())
                .employerRate(request.getEmployerRate())
                .lowIncomeThreshold(request.getLowIncomeThreshold())
                .lowIncomeEmployeeRate(request.getLowIncomeEmployeeRate())
                .maxSalaryCap(request.getMaxSalaryCap())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return pagibigRateTableRepository.save(rateTable);
    }

    public Page<PagibigRate> getAllPagibigRateTables(int page, int limit, LocalDate effectiveDate) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "effectiveDate"));

        if (effectiveDate != null) {
            return pagibigRateTableRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.lessThanOrEqualTo(root.get("effectiveDate"), effectiveDate),
                            cb.isNull(root.get("deletedAt"))
                    ),
                    pageable
            );
        }

        return pagibigRateTableRepository.findAll(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")),
                pageable
        );
    }

    public PagibigRate getPagibigRateTableById(UUID id) {
        return pagibigRateTableRepository.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pag-IBIG rate table not found with ID: " + id
                ));
    }

    public PagibigRate getLatestPagibigRateTable(LocalDate date) {
        return pagibigRateTableRepository.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No Pag-IBIG rate table found for date: " + date
                ));
    }

    @Transactional
    public PagibigRate updatePagibigRateTable(UUID id, PagibigRateRequest request) {
        PagibigRate rateTable = getPagibigRateTableById(id);

        rateTable.setEmployeeRate(request.getEmployeeRate());
        rateTable.setEmployerRate(request.getEmployerRate());
        rateTable.setLowIncomeThreshold(request.getLowIncomeThreshold());
        rateTable.setLowIncomeEmployeeRate(request.getLowIncomeEmployeeRate());
        rateTable.setMaxSalaryCap(request.getMaxSalaryCap());
        rateTable.setEffectiveDate(request.getEffectiveDate());

        return pagibigRateTableRepository.save(rateTable);
    }

    @Transactional
    public void deletePagibigRateTable(UUID id) {
        PagibigRate rateTable = getPagibigRateTableById(id);
        rateTable.setDeletedAt(Instant.now());
        pagibigRateTableRepository.save(rateTable);
    }
}
