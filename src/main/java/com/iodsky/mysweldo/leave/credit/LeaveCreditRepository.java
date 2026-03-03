package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.leave.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveCreditRepository extends JpaRepository<LeaveCredit, UUID> {

    List<LeaveCredit> findAllByEmployee_Id(Long employeeId);

    Optional<LeaveCredit> findByEmployee_IdAndType(Long employeeId, LeaveType type);

    boolean existsByEmployee_IdAndEffectiveDate(Long employeeId,  LocalDate effectiveDate);

}
