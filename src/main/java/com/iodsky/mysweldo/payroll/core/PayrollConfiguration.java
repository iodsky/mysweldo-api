package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.payroll.contribution.pagIbig.PagibigRateTable;
import com.iodsky.mysweldo.payroll.contribution.philhealth.PhilhealthRateTable;
import com.iodsky.mysweldo.payroll.contribution.sss.SssRateTable;
import com.iodsky.mysweldo.payroll.tax.IncomeTaxBracket;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Holds preloaded payroll configuration data to avoid repeated database queries
 * when calculating multiple payrolls for the same period.
 */
@Getter
@Builder
public class PayrollConfiguration {
    private PhilhealthRateTable philhealthRateTable;
    private PagibigRateTable pagibigRateTable;
    private SssRateTable sssRateTable;
    private List<IncomeTaxBracket> incomeTaxBrackets;
}
