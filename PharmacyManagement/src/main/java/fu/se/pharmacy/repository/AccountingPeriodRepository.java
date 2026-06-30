package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Integer> {
    Optional<AccountingPeriod> findByPeriodYearAndPeriodMonth(Integer periodYear, Integer periodMonth);
}
