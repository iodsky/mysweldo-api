package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.tax.TaxBracket;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Preloaded statutory rates for a payroll period, shared across all employees in a run. */
@Getter
@Builder
public class StatutoryRateSnapshot {
    private PhilhealthRate philhealthRateTable;
    private PagibigRate pagibigRateTable;
    private SssRate sssRateTable;
    private List<TaxBracket> incomeTaxBrackets;
}
