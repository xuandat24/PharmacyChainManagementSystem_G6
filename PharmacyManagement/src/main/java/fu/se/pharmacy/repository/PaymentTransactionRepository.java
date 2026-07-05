package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;  // BUG FIX 7: thiếu @Repository

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository  // BUG FIX 7: không có annotation này → Spring không tạo bean → NPE khi inject
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByGatewayTransactionCode(String gatewayTransactionCode);

    boolean existsByGatewayTransactionCode(String gatewayTransactionCode);

    List<PaymentTransaction> findByPaymentId(Integer paymentId);

    List<PaymentTransaction> findByTransactionStatus(String transactionStatus);

    List<PaymentTransaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}