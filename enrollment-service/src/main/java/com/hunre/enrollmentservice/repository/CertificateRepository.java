package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    Optional<Certificate> findByCertificateCode(String certificateCode);

    boolean existsByEnrollmentId(Long enrollmentId);
}
