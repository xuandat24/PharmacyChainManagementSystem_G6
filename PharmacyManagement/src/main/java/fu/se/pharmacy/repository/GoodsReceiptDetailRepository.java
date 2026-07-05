package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.GoodsReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptDetailRepository extends JpaRepository<GoodsReceiptDetail, Integer> {
    List<GoodsReceiptDetail> findByGoodsReceiptId(Integer goodsReceiptId);
}
