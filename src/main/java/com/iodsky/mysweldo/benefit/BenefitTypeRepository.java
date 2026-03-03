package com.iodsky.mysweldo.benefit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitTypeRepository extends JpaRepository<BenefitType, String>, JpaSpecificationExecutor<BenefitType> {
}
