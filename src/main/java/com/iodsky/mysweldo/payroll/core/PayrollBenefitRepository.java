package com.iodsky.mysweldo.payroll.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayrollBenefitRepository extends JpaRepository<PayrollBenefit, UUID> {

    boolean existsByBenefit_Code(String code);

}