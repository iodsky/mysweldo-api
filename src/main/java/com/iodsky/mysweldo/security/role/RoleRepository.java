package com.iodsky.mysweldo.security.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByName(String name);

    Optional<Role> findByName(String name);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.role.id = :roleId")
    boolean isRoleUsedById(@Param("roleId") Long roleId);

}
