package com.iodsky.mysweldo.position;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, String> {

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.position.id = :positionId AND e.deletedAt IS NULL")
    long countEmployeesByPositionId(@Param("positionId") String positionId);

    boolean existsByDepartmentId(String departmentId);
}
