package com.iodsky.mysweldo.philhealth;

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
public class PhilhealthRateService {

    private final PhilhealthRateRepository repository;

    @Transactional
    public PhilhealthRate createPhilhealthRate(PhilhealthRateRequest request) {
        // Check if configuration already exists for this effective date
        if (repository.findLatestByEffectiveDate(request.getEffectiveDate()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "PhilHealth rate already exists for effective date: " + request.getEffectiveDate()
            );
        }

        PhilhealthRate philhealthRate = PhilhealthRate.builder()
                .premiumRate(request.getPremiumRate())
                .maxSalaryCap(request.getMaxSalaryCap())
                .minSalaryFloor(request.getMinSalaryFloor())
                .fixedContribution(request.getFixedContribution())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repository.save(philhealthRate);
    }

    public Page<PhilhealthRate> getAllPhilhealthRates(int page, int limit, LocalDate effectiveDate) {
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

    public PhilhealthRate getPhilhealthRateById(UUID id) {
        return repository.findById(id)
                .filter(config -> config.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PhilHealth rate not found with ID: " + id
                ));
    }

    public PhilhealthRate getLatestPhilhealthRate(LocalDate date) {
        return repository.findLatestByEffectiveDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No PhilHealth rate found for date: " + date
                ));
    }

    @Transactional
    public PhilhealthRate updatePhilhealthRate(UUID id, PhilhealthRateRequest request) {
        PhilhealthRate philhealthRate = getPhilhealthRateById(id);

        philhealthRate.setPremiumRate(request.getPremiumRate());
        philhealthRate.setMaxSalaryCap(request.getMaxSalaryCap());
        philhealthRate.setEffectiveDate(request.getEffectiveDate());

        return repository.save(philhealthRate);
    }
}
