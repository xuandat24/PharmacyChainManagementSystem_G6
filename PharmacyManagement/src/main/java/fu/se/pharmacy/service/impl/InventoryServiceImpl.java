package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FIX QUAN TRỌNG: CK_inventory_transactions_quantity CHECK (quantity > 0)
 * Schema yêu cầu cột quantity LUÔN dương (> 0).
 * Trước đây deductForSale() insert quantity = -deducted (âm) → vi phạm constraint.
 * Tương tự transferOut() và applyStockCount() (diff âm) cũng bị lỗi.
 *
 * Giải pháp: luôn lưu Math.abs(value) vào quantity.
 * transaction_type đã đủ để phân biệt xuất/nhập:
 *   RECEIPT, SALE_RETURN, TRANSFER_IN, ADJUSTMENT_IN  → nhập (dương)
 *   SALE, TRANSFER_OUT, ADJUSTMENT_OUT, EXPIRED_DISPOSAL → xuất (cũng dương vì constraint)
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private InventoryTransactionRepository transactionRepository;
    @Autowired private GoodsReceiptRepository goodsReceiptRepository;
    @Autowired private GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleDetailRepository saleDetailRepository;
    @Autowired private StockTransferRepository stockTransferRepository;
    @Autowired private StockCountRepository stockCountRepository;
    @Autowired private StockCountDetailRepository stockCountDetailRepository;

    @Override
    public int getAvailableQuantity(Integer branchId, Integer medicineId) {
        Integer total = inventoryBatchRepository.sumStock(branchId, medicineId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional
    public void postGoodsReceipt(Integer receiptId) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhận hàng: " + receiptId));

        boolean alreadyPosted = !transactionRepository
                .findByReferenceIdAndTransactionType(receiptId, "RECEIPT").isEmpty();
        if (alreadyPosted) return;

        List<GoodsReceiptDetail> details = goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId);
        for (GoodsReceiptDetail detail : details) {
            List<InventoryBatch> existing = inventoryBatchRepository
                    .findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(
                            receipt.getBranchId(), detail.getMedicineId(), "AVAILABLE")
                    .stream()
                    .filter(b -> b.getBatchNumber().equals(detail.getBatchNumber()))
                    .toList();

            InventoryBatch batch;
            if (!existing.isEmpty()) {
                batch = existing.get(0);
                batch.setQuantityOnHand(batch.getQuantityOnHand() + detail.getAcceptedQuantity());
            } else {
                batch = new InventoryBatch();
                batch.setBranchId(receipt.getBranchId());
                batch.setMedicineId(detail.getMedicineId());
                batch.setBatchNumber(detail.getBatchNumber());
                batch.setQuantityOnHand(detail.getAcceptedQuantity());
                batch.setExpiryDate(detail.getExpiryDate());
                batch.setUnitCost(detail.getActualUnitPrice() != null ? detail.getActualUnitPrice() : 0);
                batch.setStatus("AVAILABLE");
            }
            inventoryBatchRepository.save(batch);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranchId(receipt.getBranchId());
            tx.setMedicineId(detail.getMedicineId());
            tx.setInventoryBatchId(batch.getInventoryBatchId());
            tx.setTransactionType("RECEIPT");
            tx.setReferenceType("GOODS_RECEIPT");
            tx.setReferenceId(receiptId);
            // quantity > 0: RECEIPT luôn là số dương → đúng constraint
            tx.setQuantityChange(detail.getAcceptedQuantity());
            tx.setUnitCost(detail.getActualUnitPrice());
            tx.setCreatedBy(receipt.getReceivedBy());
            transactionRepository.save(tx);
        }

        receipt.setStatus("POSTED");
        receipt.setPostedAt(LocalDateTime.now());
        goodsReceiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void deductForSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng: " + saleId));

        List<SaleDetail> details = saleDetailRepository.findBySaleId(saleId);
        for (SaleDetail detail : details) {
            int qtyToDeduct = detail.getQuantity();

            List<InventoryBatch> batches = inventoryBatchRepository
                    .findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(
                            sale.getBranchId(), detail.getMedicineId(), "AVAILABLE");

            int total = batches.stream()
                    .mapToInt(b -> b.getQuantityOnHand() != null ? b.getQuantityOnHand() : 0).sum();
            if (total < qtyToDeduct)
                throw new IllegalStateException("Tồn kho không đủ cho thuốc ID: " + detail.getMedicineId());

            for (InventoryBatch batch : batches) {
                if (qtyToDeduct <= 0) break;
                int avail = batch.getQuantityOnHand() != null ? batch.getQuantityOnHand() : 0;
                if (avail <= 0) continue;
                int deducted = Math.min(avail, qtyToDeduct);
                batch.setQuantityOnHand(avail - deducted);
                if (batch.getQuantityOnHand() == 0) batch.setStatus("DISPOSED");
                inventoryBatchRepository.save(batch);
                qtyToDeduct -= deducted;

                InventoryTransaction tx = new InventoryTransaction();
                tx.setBranchId(sale.getBranchId());
                tx.setMedicineId(detail.getMedicineId());
                tx.setInventoryBatchId(batch.getInventoryBatchId());
                tx.setTransactionType("SALE");
                tx.setReferenceType("SALE");
                tx.setReferenceId(saleId);
                // FIX: quantity phải > 0 theo CHECK constraint.
                // Trước đây: tx.setQuantityChange(-deducted) → âm → vi phạm constraint → lỗi 500
                // Sau fix: lưu Math.abs(deducted). transaction_type = "SALE" đã biểu thị xuất kho.
                tx.setQuantityChange(deducted);
                tx.setCreatedBy(sale.getPharmacistId());
                transactionRepository.save(tx);
            }
        }
    }

    @Override
    @Transactional
    public void restoreForCancelledSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng: " + saleId));

        List<InventoryTransaction> saleTxs = transactionRepository
                .findByReferenceIdAndTransactionType(saleId, "SALE");

        for (InventoryTransaction saleTx : saleTxs) {
            // FIX: quantityChange giờ luôn dương nên lấy trực tiếp, không cần Math.abs()
            int restoredQty = saleTx.getQuantityChange();
            if (saleTx.getInventoryBatchId() != null) {
                inventoryBatchRepository.findById(saleTx.getInventoryBatchId()).ifPresent(batch -> {
                    batch.setQuantityOnHand(batch.getQuantityOnHand() + restoredQty);
                    if ("DISPOSED".equals(batch.getStatus())) batch.setStatus("AVAILABLE");
                    inventoryBatchRepository.save(batch);
                });
            }

            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranchId(saleTx.getBranchId());
            tx.setMedicineId(saleTx.getMedicineId());
            tx.setInventoryBatchId(saleTx.getInventoryBatchId());
            tx.setTransactionType("SALE_RETURN");
            tx.setReferenceType("SALE");
            tx.setReferenceId(saleId);
            // SALE_RETURN là nhập lại → dương → đúng constraint
            tx.setQuantityChange(restoredQty);
            tx.setCreatedBy(sale.getPharmacistId());
            transactionRepository.save(tx);
        }
    }

    @Override
    @Transactional
    public void transferOut(Integer transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển: " + transferId));

        transfer.getDetails().forEach(detail -> {
            if (detail.getFromInventoryBatchId() != null) {
                inventoryBatchRepository.findById(detail.getFromInventoryBatchId()).ifPresent(batch -> {
                    if (batch.getQuantityOnHand() < detail.getRequestedQuantity())
                        throw new IllegalStateException("Tồn kho lô không đủ để điều chuyển");
                    batch.setQuantityOnHand(batch.getQuantityOnHand() - detail.getRequestedQuantity());
                    inventoryBatchRepository.save(batch);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setBranchId(transfer.getFromBranchId());
                    tx.setMedicineId(detail.getMedicineId());
                    tx.setInventoryBatchId(batch.getInventoryBatchId());
                    tx.setTransactionType("TRANSFER_OUT");
                    tx.setReferenceType("STOCK_TRANSFER");
                    tx.setReferenceId(transferId);
                    // FIX: lưu dương, transaction_type = "TRANSFER_OUT" biểu thị xuất
                    tx.setQuantityChange(detail.getRequestedQuantity());
                    transactionRepository.save(tx);
                });
            }
        });
    }

    @Override
    @Transactional
    public void transferIn(Integer transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển: " + transferId));

        transfer.getDetails().forEach(detail -> {
            int qty = detail.getReceivedQuantity() != null
                    ? detail.getReceivedQuantity() : detail.getRequestedQuantity();

            String batchNum = detail.getFromInventoryBatchId() != null
                    ? inventoryBatchRepository.findById(detail.getFromInventoryBatchId())
                    .map(InventoryBatch::getBatchNumber).orElse("TRF-" + transferId)
                    : "TRF-" + transferId;

            List<InventoryBatch> existing = inventoryBatchRepository
                    .findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(
                            transfer.getToBranchId(), detail.getMedicineId(), "AVAILABLE")
                    .stream().filter(b -> b.getBatchNumber().equals(batchNum)).toList();

            InventoryBatch toInv;
            if (!existing.isEmpty()) {
                toInv = existing.get(0);
                toInv.setQuantityOnHand(toInv.getQuantityOnHand() + qty);
            } else {
                toInv = new InventoryBatch();
                toInv.setBranchId(transfer.getToBranchId());
                toInv.setMedicineId(detail.getMedicineId());
                toInv.setBatchNumber(batchNum);
                toInv.setQuantityOnHand(qty);
                toInv.setStatus("AVAILABLE");
                if (detail.getFromInventoryBatchId() != null) {
                    inventoryBatchRepository.findById(detail.getFromInventoryBatchId())
                            .ifPresent(b -> toInv.setExpiryDate(b.getExpiryDate()));
                }
            }
            inventoryBatchRepository.save(toInv);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranchId(transfer.getToBranchId());
            tx.setMedicineId(detail.getMedicineId());
            tx.setInventoryBatchId(toInv.getInventoryBatchId());
            tx.setTransactionType("TRANSFER_IN");
            tx.setReferenceType("STOCK_TRANSFER");
            tx.setReferenceId(transferId);
            // TRANSFER_IN luôn dương → đúng constraint
            tx.setQuantityChange(qty);
            transactionRepository.save(tx);
        });
    }

    @Override
    @Transactional
    public void applyStockCount(Integer stockCountId) {
        StockCount count = stockCountRepository.findById(stockCountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê: " + stockCountId));

        List<StockCountDetail> details = stockCountDetailRepository.findByStockCountId(stockCountId);
        for (StockCountDetail detail : details) {
            inventoryBatchRepository.findById(detail.getInventoryBatchId()).ifPresent(batch -> {
                int diff = detail.getActualQuantity() - detail.getSystemQuantity();
                batch.setQuantityOnHand(detail.getActualQuantity());
                if ("DISPOSED".equals(batch.getStatus()) && detail.getActualQuantity() > 0)
                    batch.setStatus("AVAILABLE");
                inventoryBatchRepository.save(batch);

                if (diff != 0) {
                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setBranchId(count.getBranchId());
                    tx.setMedicineId(batch.getMedicineId());
                    tx.setInventoryBatchId(batch.getInventoryBatchId());
                    tx.setTransactionType(diff > 0 ? "ADJUSTMENT_IN" : "ADJUSTMENT_OUT");
                    tx.setReferenceType("STOCK_COUNT");
                    tx.setReferenceId(stockCountId);
                    // FIX: lưu Math.abs(diff) → luôn dương, transaction_type phân biệt tăng/giảm
                    tx.setQuantityChange(Math.abs(diff));
                    tx.setCreatedBy(count.getCreatedBy());
                    transactionRepository.save(tx);
                }
            });
        }
    }
}
