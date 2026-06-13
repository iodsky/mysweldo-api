package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.core.PayrollComputationResult;
import com.iodsky.mysweldo.payroll.core.StatutoryRateSnapshot;
import com.iodsky.mysweldo.payroll.run.PayrollRun;

public interface PayrollComputationStrategy {
    PayrollComputationResult compute(Employee employee, PayrollRun payrollRun, StatutoryRateSnapshot rates);
}
