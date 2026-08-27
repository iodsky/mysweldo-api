package com.iodsky.mysweldo.contribution;

import com.iodsky.mysweldo.payroll.core.EmployerContributionRepository;
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
public class ContributionService {

    private final ContributionRepository repository;
    private final EmployerContributionRepository employerContributionRepository;

    @Transactional
    public Contribution createContribution(ContributionRequest request) {
        if (repository.existsById(request.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contribution already exists with ID: " + request.getCode()
            );
        }

        Contribution contribution = Contribution.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .build();

        return repository.save(contribution);
    }

    public Page<Contribution> getAllContributions(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        return repository.findAll(pageable);
    }

    public Contribution getContributionByCode(String code) {
        return repository.findById(code)
                .filter(contribution -> contribution.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Contribution not found with code: " + code
                ));
    }

    @Transactional
    public Contribution updateContribution(String code, ContributionRequest request) {
        Contribution contribution = getContributionByCode(code);
        contribution.setDescription(request.getDescription());
        return repository.save(contribution);
    }

    @Transactional
    public void deleteContribution(String id) {
        Contribution contribution = getContributionByCode(id);

        if (employerContributionRepository.existsByContribution_Code(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete contribution '" + id + "'. It is referenced by payroll items"
            );
        }

        contribution.setDeletedAt(Instant.now());
        repository.save(contribution);
    }
}
