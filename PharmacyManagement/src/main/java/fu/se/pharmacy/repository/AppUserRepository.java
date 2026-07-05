package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByStatus(String status);
    long countByStatus(String status);
    List<AppUser> findByBranchId(Integer branchId);
    List<AppUser> findByRoleId(Integer roleId);
}
