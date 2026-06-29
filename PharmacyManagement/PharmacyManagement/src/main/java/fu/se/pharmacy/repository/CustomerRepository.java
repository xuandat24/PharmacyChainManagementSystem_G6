package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByPhone(String phone);

    // FIX: CustomerServiceImpl.searchByName() gọi method này nhưng chưa có
    List<Customer> findByFullNameContainingIgnoreCase(String name);

    @Query("SELECT c FROM Customer c WHERE c.phone LIKE %:kw% OR c.fullName LIKE %:kw%")
    List<Customer> searchByKeyword(@Param("kw") String keyword);
}