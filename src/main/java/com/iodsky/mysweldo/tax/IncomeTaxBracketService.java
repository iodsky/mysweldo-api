package com.iodsky.mysweldo.tax;

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

@Service
@RequiredArgsConstructor
public class IncomeTaxBracketService {

    private final IncomeTaxBracketRepository repository;

    @Transactional
    public IncomeTaxBracket createIncomeTaxBracket(IncomeTaxBracketRequest request) {
        IncomeTaxBracket bracket = IncomeTaxBracket.builder()
                .minIncome(request.getMinIncome())
                .maxIncome(request.getMaxIncome())
                .baseTax(request.getBaseTax())
                .marginalRate(request.getMarginalRate())
                .threshold(request.getThreshold())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return repository.save(bracket);
    }

    public Page<IncomeTaxBracket> getAllIncomeTaxBrackets(
            int page, int limit, LocalDate effectiveDate, BigDecimal minIncome, BigDecimal maxIncome) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "minIncome"));

        return repository.findAll((root, query, cb) -> {
            var predicates = cb.conjunction();

            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));

            if (effectiveDate != null) {
                predicates = cb.and(predicates, cb.equal(root.get("effectiveDate"), effectiveDate));
            }

            if (minIncome != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("minIncome"), minIncome));
            }

            if (maxIncome != null && maxIncome.compareTo(BigDecimal.ZERO) > 0) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("maxIncome"), maxIncome));
            }

            return predicates;
        }, pageable);
    }

    public List<IncomeTaxBracket> getAllIncomeTaxBracketsByDate(LocalDate effectiveDate) {
        return repository.findAllByEffectiveDate(effectiveDate);
    }

    public IncomeTaxBracket getIncomeTaxBracketById(UUID id) {
        return repository.findById(id)
                .filter(bracket -> bracket.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Income tax bracket not found with ID: " + id
                ));
    }

    public IncomeTaxBracket getIncomeTaxBracketByIncomeAndDate(BigDecimal income, LocalDate date) {
        IncomeTaxBracket bracket = repository.findByIncomeAndEffectiveDate(income, date);
        if (bracket == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No income tax bracket found for income: " + income + " and date: " + date
            );
        }
        return bracket;
    }

    @Transactional
    public IncomeTaxBracket updateIncomeTaxBracket(UUID id, IncomeTaxBracketRequest request) {
        IncomeTaxBracket bracket = getIncomeTaxBracketById(id);

        bracket.setMinIncome(request.getMinIncome());
        bracket.setMaxIncome(request.getMaxIncome());
        bracket.setBaseTax(request.getBaseTax());
        bracket.setMarginalRate(request.getMarginalRate());
        bracket.setThreshold(request.getThreshold());
        bracket.setEffectiveDate(request.getEffectiveDate());

        return repository.save(bracket);
    }

    @Transactional
    public void deleteIncomeTaxBracket(UUID id) {
        IncomeTaxBracket bracket = getIncomeTaxBracketById(id);
        bracket.setDeletedAt(Instant.now());
        repository.save(bracket);
    }
}
