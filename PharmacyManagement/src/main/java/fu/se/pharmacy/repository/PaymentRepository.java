package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /**
     * FIX: Đổi từ Optional → List để tránh lỗi
     * "Query did not return a unique result: 2 results were returned"
     * khi một sale có nhiều payment (tạo online rồi chuyển cash).
     * Luôn lấy get(0) từ kết quả (cái mới nhất theo createdAt DESC).
     */
    List<Payment> findBySaleIdOrderByCreatedAtDesc(Integer saleId);

    /**
     * Helper: lấy payment mới nhất của sale.
     * Dùng thay cho findBySaleId().ifPresent() ở các chỗ cũ.
     */
    default Optional<Payment> findLatestBySaleId(Integer saleId) {
        List<Payment> list = findBySaleIdOrderByCreatedAtDesc(saleId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}