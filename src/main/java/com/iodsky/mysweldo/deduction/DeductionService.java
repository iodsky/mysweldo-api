package com.iodsky.mysweldo.deduction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeductionService {

    private final DeductionRepository repository;

    @Transactional
    public Deduction createDeduction(DeductionRequest request) {
        if (repository.existsById(request.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Deduction already exists with code: " + request.getCode()
            );
        }

        Deduction deduction = Deduction.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .build();

        return repository.save(deduction);
    }

    public Page<Deduction> getAllDeductions(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);

        return repository.findAll(
                pageable
        );
    }

    public Deduction getDeductionByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Deduction not found with code: " + code
                ));
    }

    @Transactional
    public Deduction updateDeduction(String code, DeductionRequest request) {
        Deduction deduction = getDeductionByCode(code);
        deduction.setDescription(request.getDescription());
        return repository.save(deduction);
    }

    @Transactional
    public void deleteDeduction(String code) {
        Deduction deduction = getDeductionByCode(code);
        deduction.setDeletedAt(Instant.now());
        repository.save(deduction);
    }
}
