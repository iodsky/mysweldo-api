package com.iodsky.mysweldo.payroll.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, UUID> {

    boolean existsByDeduction_Code(String code);

}