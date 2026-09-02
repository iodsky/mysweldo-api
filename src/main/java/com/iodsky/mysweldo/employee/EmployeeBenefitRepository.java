package com.iodsky.mysweldo.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployeeBenefitRepository extends JpaRepository<EmployeeBenefit, UUID> {

    boolean existsByBenefit_Code(String code);

}