package com.iodsky.mysweldo.batch.employee;

import com.iodsky.mysweldo.employee.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Skip listener for employee import job to track and log skipped records
 * due to duplicate data violations or other errors.
 */
@Component
@Slf4j
public class EmployeeImportSkipListener implements SkipListener<EmployeeImportRecord, Employee> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Skipped record during read phase due to: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(EmployeeImportRecord item, Throwable t) {
        log.warn("Skipped employee {} {} during processing due to: {}",
                item.getFirstName(), item.getLastName(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(@NonNull Employee item, @NonNull Throwable t) {
        String reason = "Unknown error";

        if (t instanceof DataIntegrityViolationException) {
            String message = t.getMessage();
            if (message != null) {
                if (message.contains("sss_no")) {
                    reason = "Duplicate SSS Number: " + (item.getGovernmentId() != null ? item.getGovernmentId().getSssNumber() : "N/A");
                } else if (message.contains("tin_no")) {
                    reason = "Duplicate TIN Number: " + (item.getGovernmentId() != null ? item.getGovernmentId().getTinNumber() : "N/A");
                } else if (message.contains("philhealth_no")) {
                    reason = "Duplicate PhilHealth Number: " + (item.getGovernmentId() != null ? item.getGovernmentId().getPhilhealthNumber() : "N/A");
                } else if (message.contains("pagibig_no")) {
                    reason = "Duplicate PagIbig Number: " + (item.getGovernmentId() != null ? item.getGovernmentId().getPagIbigNumber() : "N/A");
                } else if (message.contains("phone_number")) {
                    reason = "Duplicate Phone Number: " + item.getPhoneNumber();
                } else if (message.contains("address")) {
                    reason = "Duplicate Address: " + item.getAddress();
                } else {
                    reason = "Duplicate constraint violation";
                }
            }
        } else {
            reason = t.getMessage();
        }

        log.warn("Skipped employee {} {} during write phase. Reason: {}",
                item.getFirstName(), item.getLastName(), reason);
    }
}
