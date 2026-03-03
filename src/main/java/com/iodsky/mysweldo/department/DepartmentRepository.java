package com.iodsky.mysweldo.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, String> {

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId AND e.deletedAt IS NULL")
    long countEmployeesByDepartmentId(@Param("departmentId") String departmentId);

}
