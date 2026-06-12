package cl.nextech.dashboard.repository;

import cl.nextech.dashboard.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailAndActiveTrue(String email);
    boolean existsByEmail(String email);
}
