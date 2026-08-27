package com.iodsky.mysweldo.payroll.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployerContributionRepository extends JpaRepository<EmployerContribution, UUID> {

    boolean existsByContribution_Code(String code);

}