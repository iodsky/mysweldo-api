package com.iodsky.mysweldo.benefit;

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

@Service
@RequiredArgsConstructor
public class BenefitTypeService {

    private final BenefitTypeRepository benefitTypeRepository;

    @Transactional
    public BenefitType createBenefitType(BenefitTypeRequest request) {
        // Check if benefit type with this ID already exists
        if (benefitTypeRepository.existsById(request.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Benefit type already exists with ID: " + request.getId()
            );
        }

        BenefitType benefitType = BenefitType.builder()
                .id(request.getId())
                .type(request.getType())
                .build();

        return benefitTypeRepository.save(benefitType);
    }

    public Page<BenefitType> getAllBenefitTypes(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "id"));

        return benefitTypeRepository.findAll(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")),
                pageable
        );
    }

    public BenefitType getBenefitTypeById(String id) {
        return benefitTypeRepository.findById(id)
                .filter(type -> type.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benefit type not found with ID: " + id
                ));
    }

    @Transactional
    public BenefitType updateBenefitType(String id, BenefitTypeRequest request) {
        BenefitType benefitType = getBenefitTypeById(id);
        benefitType.setType(request.getType());
        return benefitTypeRepository.save(benefitType);
    }

    @Transactional
    public void deleteBenefitType(String id) {
        BenefitType benefitType = getBenefitTypeById(id);
        benefitType.setDeletedAt(Instant.now());
        benefitTypeRepository.save(benefitType);
    }
}
