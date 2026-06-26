package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByGatewayTransactionCode(String gatewayTransactionCode);

    boolean existsByGatewayTransactionCode(String gatewayTransactionCode);

    List<PaymentTransaction> findByPaymentId(Integer paymentId);

    List<PaymentTransaction> findByTransactionStatus(String transactionStatus);

    List<PaymentTransaction> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}