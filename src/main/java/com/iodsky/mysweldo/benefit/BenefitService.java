package com.iodsky.mysweldo.benefit;

import com.iodsky.mysweldo.employee.EmployeeBenefitRepository;
import com.iodsky.mysweldo.payroll.item.PayrollBenefitRepository;
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
public class BenefitService {

    private final BenefitRepository repository;
    private final PayrollBenefitRepository payrollBenefitRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;

    @Transactional
    public Benefit createBenefit(BenefitRequest request) {
        if (repository.existsById(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Benefit already exists with ID: " + request.getCode());
        }

        if (request.isTaxable() && request.getNonTaxableLimit() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxable benefit cannot have non-taxable limit");
        }

        Benefit benefit = Benefit.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .taxable(request.isTaxable())
                .nonTaxableLimit(request.getNonTaxableLimit())
                .build();

        return repository.save(benefit);
    }

    public Page<Benefit> getAllBenefits(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);

        return repository.findAll(
                pageable
        );
    }

    public Benefit getBenefitByCode(String code) {
        return repository.findById(code)
                .filter(benefit -> benefit.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit not found with code: " + code));
    }

    @Transactional
    public Benefit updateBenefit(String code, BenefitRequest request) {
        Benefit benefit = getBenefitByCode(code);
        benefit.setDescription(request.getDescription());
        return repository.save(benefit);
    }

    @Transactional
    public void deleteBenefit(String id) {
        Benefit benefit = getBenefitByCode(id);

        if (payrollBenefitRepository.existsByBenefit_Code(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete benefit '" + id + "'. It is referenced by payroll items"
            );
        }

        if (employeeBenefitRepository.existsByBenefit_Code(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete benefit '" + id + "'. It is assigned to employees"
            );
        }

        benefit.setDeletedAt(Instant.now());
        repository.save(benefit);
    }
}
