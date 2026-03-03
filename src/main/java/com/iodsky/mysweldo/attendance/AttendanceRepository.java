package com.iodsky.mysweldo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEmployee_IdAndDate(Long employeeId, LocalDate date);

    Page<Attendance> findAllByDate(LocalDate date, Pageable pageable);

    Page<Attendance> findAllByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Attendance> findByEmployee_IdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Attendance> findByEmployee_IdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    @Query("""
    SELECT COALESCE(SUM(a.totalHours), 0)
    FROM Attendance a
    WHERE a.employee.id = :employeeId
    AND a.date BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumTotalHoursByEmployee_IdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

}