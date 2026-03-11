package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.tax.TaxBracket;
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
    private PhilhealthRate philhealthRateTable;
    private PagibigRate pagibigRateTable;
    private SssRate sssRateTable;
    private List<TaxBracket> incomeTaxBrackets;
}
