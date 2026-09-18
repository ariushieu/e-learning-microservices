package com.hunre.authservice.repository;

import com.hunre.authservice.domain.Role;
import com.hunre.authservice.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
