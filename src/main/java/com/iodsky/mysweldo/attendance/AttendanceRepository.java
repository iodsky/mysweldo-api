package com.iodsky.mysweldo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findFirstByEmployee_IdAndTimeInBetween(Long employeeId, LocalDateTime timeInStart, LocalDateTime timeInEnd);

    Page<AttendanceView> findAllByTimeInBetween(LocalDateTime timeInStart, LocalDateTime timeInEnd, Pageable pageable);

    Page<AttendanceView> findAllByEmployee_Id(Long employeeId, Pageable pageable);

    Page<AttendanceView> findByEmployee_IdAndTimeInBetween(Long employeeId, LocalDateTime timeInStart, LocalDateTime timeInEnd, Pageable pageable);

    List<Attendance> findByEmployee_IdAndTimeInBetween(Long employeeId, LocalDateTime timeInStart, LocalDateTime timeInEnd);

    Page<AttendanceView> findAllBy (Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(a.totalHours), 0)
    FROM Attendance a
    WHERE a.employee.id = :employeeId
    AND a.timeIn >= :timeInStart
    AND a.timeIn < :timeInEnd
    """)
    BigDecimal sumTotalHoursByEmployee_IdAndTimeInBetween(Long employeeId, LocalDateTime timeInStart, LocalDateTime timeInEnd);

    boolean existsByEmployee_IdAndTimeOutIsNull(Long employeeId);

    Optional<Attendance> findFirstByEmployee_IdAndTimeOutIsNullOrderByTimeInDesc(Long employeeId);
}