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

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagibigRateService {

    private final PagibigRateRepository repository;

    @Transactional
    public PagibigRate createPagibigRate(PagibigRateRequest request) {
        if (repository.findLatestByEffectiveDate(request.getEffectiveDate()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pag-IBIG rate already exists for effective date: " + request.getEffectiveDate()
            );
        }

        PagibigRate pagibigRate = PagibigRate.builder()
                .employeeRate(request.getEmployeeRate())
                .employerRate(request.getEmployerRate())
                .lowIncomeThreshold(request.getLowIncomeThreshold())
                .lowIncomeEmployeeRate(request.getLowIncomeEmployeeRate())
                .maxSalaryCap(request.getMaxSalaryCap())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repository.save(pagibigRate);
    }

    public Page<PagibigRate> getAllPagibigRates(int page, int limit, LocalDate effectiveDate) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "effectiveDate"));

        if (effectiveDate != null) {
            return repository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.lessThanOrEqualTo(root.get("effectiveDate"), effectiveDate),
                            cb.isNull(root.get("deletedAt"))
                    ),
                    pageable
            );
        }

        return repository.findAll(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")),
                pageable
        );
    }

    public PagibigRate getPagibigRateById(UUID id) {
        return repository.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pag-IBIG rate not found with ID: " + id
                ));
    }

    public PagibigRate getLatestPagibigRate(LocalDate date) {
        return repository.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No Pag-IBIG rate found for date: " + date
                ));
    }

    @Transactional
    public PagibigRate updatePagibigRate(UUID id, PagibigRateRequest request) {
        PagibigRate pagibigRate = getPagibigRateById(id);

        pagibigRate.setEmployeeRate(request.getEmployeeRate());
        pagibigRate.setEmployerRate(request.getEmployerRate());
        pagibigRate.setLowIncomeThreshold(request.getLowIncomeThreshold());
        pagibigRate.setLowIncomeEmployeeRate(request.getLowIncomeEmployeeRate());
        pagibigRate.setMaxSalaryCap(request.getMaxSalaryCap());
        pagibigRate.setEffectiveDate(request.getEffectiveDate());

        return repository.save(pagibigRate);
    }
}
