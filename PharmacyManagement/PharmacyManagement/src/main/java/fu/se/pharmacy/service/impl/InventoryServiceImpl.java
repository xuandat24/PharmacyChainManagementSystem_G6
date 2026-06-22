package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private GoodsReceiptRepository goodsReceiptRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private StockCountRepository stockCountRepository;

    @Override
    public int getAvailableQuantity(Integer branchId, Integer medicineId) {
        List<Inventory> stockList = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineId(branchId, medicineId);
        LocalDate today = LocalDate.now();
        return stockList.stream()
                .filter(i -> i.getQuantity() > 0 && (i.getExpiryDate() == null || i.getExpiryDate().isAfter(today)))
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    @Override
    @Transactional
    public void postGoodsReceipt(Integer receiptId) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhận hàng ID: " + receiptId));

        if ("POSTED".equals(receipt.getStatus())) {
            return; // Đã nhận kho rồi
        }

        Employee currentUser = receipt.getReceivedBy();

        for (GoodsReceiptDetail detail : receipt.getDetails()) {
            Optional<Inventory> invOpt = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    receipt.getBranch().getBranchId(),
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            );

            Inventory inventory;
            if (invOpt.isPresent()) {
                inventory = invOpt.get();
                inventory.setQuantity(inventory.getQuantity() + detail.getQuantityReceived());
            } else {
                inventory = new Inventory();
                inventory.setBranch(receipt.getBranch());
                inventory.setMedicine(detail.getMedicine());
                inventory.setBatchNumber(detail.getBatchNumber());
                inventory.setQuantity(detail.getQuantityReceived());
                inventory.setManufactureDate(detail.getManufactureDate());
                inventory.setExpiryDate(detail.getExpiryDate());
                inventory.setPurchasePrice(detail.getPurchasePrice());
            }
            inventoryRepository.save(inventory);

            // Ghi nhận biến động kho
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranch(receipt.getBranch());
            tx.setMedicine(detail.getMedicine());
            tx.setBatchNumber(detail.getBatchNumber());
            tx.setTransactionType("RECEIPT");
            tx.setReferenceId(receiptId);
            tx.setQuantityChange(detail.getQuantityReceived());
            tx.setTransactionDate(LocalDateTime.now());
            tx.setCreatedBy(currentUser);
            transactionRepository.save(tx);
        }

        receipt.setStatus("POSTED");
        goodsReceiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void deductForSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng ID: " + saleId));

        Integer branchId = sale.getBranch().getBranchId();
        Employee currentUser = sale.getEmployee();

        for (SaleDetail detail : sale.getDetails()) {
            Integer medicineId = detail.getMedicine().getMedicineId();
            int qtyToDeduct = detail.getQuantity();

            // Lấy danh sách các lô của thuốc tại chi nhánh, sắp xếp theo hạn sử dụng tăng dần (FEFO)
            List<Inventory> stockList = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineId(branchId, medicineId);
            stockList.sort(Comparator.comparing(Inventory::getExpiryDate));

            int totalAvailable = stockList.stream().mapToInt(Inventory::getQuantity).sum();
            if (totalAvailable < qtyToDeduct) {
                throw new IllegalStateException("Số lượng tồn kho không đủ bán cho thuốc: " + detail.getMedicine().getMedicineName());
            }

            for (Inventory inv : stockList) {
                if (qtyToDeduct <= 0) break;
                if (inv.getQuantity() <= 0) continue;

                int deductedAmt = Math.min(inv.getQuantity(), qtyToDeduct);
                inv.setQuantity(inv.getQuantity() - deductedAmt);
                inventoryRepository.save(inv);

                qtyToDeduct -= deductedAmt;

                // Ghi nhận biến động kho
                InventoryTransaction tx = new InventoryTransaction();
                tx.setBranch(sale.getBranch());
                tx.setMedicine(detail.getMedicine());
                tx.setBatchNumber(inv.getBatchNumber());
                tx.setTransactionType("SALE");
                tx.setReferenceId(saleId);
                tx.setQuantityChange(-deductedAmt); // Số lượng âm
                tx.setTransactionDate(LocalDateTime.now());
                tx.setCreatedBy(currentUser);
                transactionRepository.save(tx);
            }
        }
    }

    @Override
    @Transactional
    public void restoreForCancelledSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng ID: " + saleId));

        // Tìm tất cả transaction liên quan tới Sale này để biết đã trừ từ lô nào
        List<InventoryTransaction> txs = transactionRepository.findByBranch_BranchId(sale.getBranch().getBranchId());
        List<InventoryTransaction> saleTxs = txs.stream()
                .filter(t -> "SALE".equals(t.getTransactionType()) && saleId.equals(t.getReferenceId()))
                .toList();

        for (InventoryTransaction saleTx : saleTxs) {
            Optional<Inventory> invOpt = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    sale.getBranch().getBranchId(),
                    saleTx.getMedicine().getMedicineId(),
                    saleTx.getBatchNumber()
            );

            int restoredQty = Math.abs(saleTx.getQuantityChange());

            if (invOpt.isPresent()) {
                Inventory inv = invOpt.get();
                inv.setQuantity(inv.getQuantity() + restoredQty);
                inventoryRepository.save(inv);
            } else {
                // Nếu lô hàng đã bị xóa khỏi kho bằng cách nào đó, tạo lại
                Inventory inv = new Inventory();
                inv.setBranch(sale.getBranch());
                inv.setMedicine(saleTx.getMedicine());
                inv.setBatchNumber(saleTx.getBatchNumber());
                inv.setQuantity(restoredQty);
                inv.setExpiryDate(LocalDate.now().plusYears(1)); // Mặc định hạn dùng tạm thời
                inventoryRepository.save(inv);
            }

            // Ghi nhận giao dịch hoàn kho
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranch(sale.getBranch());
            tx.setMedicine(saleTx.getMedicine());
            tx.setBatchNumber(saleTx.getBatchNumber());
            tx.setTransactionType("SALE_RETURN");
            tx.setReferenceId(saleId);
            tx.setQuantityChange(restoredQty);
            tx.setTransactionDate(LocalDateTime.now());
            tx.setCreatedBy(sale.getEmployee());
            transactionRepository.save(tx);
        }
    }

    @Override
    @Transactional
    public void transferOut(Integer transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));

        Integer fromBranchId = transfer.getFromBranch().getBranchId();

        for (StockTransferDetail detail : transfer.getDetails()) {
            Inventory inv = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    fromBranchId,
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            ).orElseThrow(() -> new IllegalStateException("Không tìm thấy lô hàng " + detail.getBatchNumber() + " tại chi nhánh xuất."));

            if (inv.getQuantity() < detail.getQuantity()) {
                throw new IllegalStateException("Số lượng tồn kho của lô " + detail.getBatchNumber() + " không đủ để điều chuyển.");
            }

            inv.setQuantity(inv.getQuantity() - detail.getQuantity());
            inventoryRepository.save(inv);

            // Ghi nhận biến động kho xuất
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranch(transfer.getFromBranch());
            tx.setMedicine(detail.getMedicine());
            tx.setBatchNumber(detail.getBatchNumber());
            tx.setTransactionType("TRANSFER_OUT");
            tx.setReferenceId(transferId);
            tx.setQuantityChange(-detail.getQuantity());
            tx.setTransactionDate(LocalDateTime.now());
            // Lấy tạm một nhân viên bất kỳ hoặc gắn người dùng thao tác
            tx.setCreatedBy(transfer.getFromBranch().getPhone() != null ? null : null); // Sẽ gán sau
            transactionRepository.save(tx);
        }
    }

    @Override
    @Transactional
    public void transferIn(Integer transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));

        Integer fromBranchId = transfer.getFromBranch().getBranchId();
        Integer toBranchId = transfer.getToBranch().getBranchId();

        for (StockTransferDetail detail : transfer.getDetails()) {
            // Lấy thông tin lô từ chi nhánh xuất để copy hạn dùng, giá mua
            Inventory fromInv = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    fromBranchId,
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            ).orElseThrow(() -> new IllegalStateException("Không tìm thấy lô hàng nguồn " + detail.getBatchNumber()));

            Optional<Inventory> toInvOpt = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    toBranchId,
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            );

            Inventory toInv;
            if (toInvOpt.isPresent()) {
                toInv = toInvOpt.get();
                toInv.setQuantity(toInv.getQuantity() + detail.getQuantity());
            } else {
                toInv = new Inventory();
                toInv.setBranch(transfer.getToBranch());
                toInv.setMedicine(detail.getMedicine());
                toInv.setBatchNumber(detail.getBatchNumber());
                toInv.setQuantity(detail.getQuantity());
                toInv.setManufactureDate(fromInv.getManufactureDate());
                toInv.setExpiryDate(fromInv.getExpiryDate());
                toInv.setPurchasePrice(fromInv.getPurchasePrice());
            }
            inventoryRepository.save(toInv);

            // Ghi nhận biến động kho nhận
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBranch(transfer.getToBranch());
            tx.setMedicine(detail.getMedicine());
            tx.setBatchNumber(detail.getBatchNumber());
            tx.setTransactionType("TRANSFER_IN");
            tx.setReferenceId(transferId);
            tx.setQuantityChange(detail.getQuantity());
            tx.setTransactionDate(LocalDateTime.now());
            transactionRepository.save(tx);
        }
    }

    @Override
    @Transactional
    public void applyStockCount(Integer stockCountId) {
        StockCount count = stockCountRepository.findById(stockCountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + stockCountId));

        Employee currentUser = count.getCreatedBy();

        for (StockCountDetail detail : count.getDetails()) {
            Optional<Inventory> invOpt = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    count.getBranch().getBranchId(),
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            );

            Inventory inv;
            if (invOpt.isPresent()) {
                inv = invOpt.get();
                inv.setQuantity(detail.getActualQuantity());
            } else {
                inv = new Inventory();
                inv.setBranch(count.getBranch());
                inv.setMedicine(detail.getMedicine());
                inv.setBatchNumber(detail.getBatchNumber());
                inv.setQuantity(detail.getActualQuantity());
                inv.setExpiryDate(LocalDate.now().plusYears(1)); // default temp expiry
            }
            inventoryRepository.save(inv);

            // Ghi nhận giao dịch điều chỉnh kho
            if (detail.getDifference() != 0) {
                InventoryTransaction tx = new InventoryTransaction();
                tx.setBranch(count.getBranch());
                tx.setMedicine(detail.getMedicine());
                tx.setBatchNumber(detail.getBatchNumber());
                tx.setTransactionType(detail.getDifference() > 0 ? "ADJUSTMENT_IN" : "ADJUSTMENT_OUT");
                tx.setReferenceId(stockCountId);
                tx.setQuantityChange(detail.getDifference());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setCreatedBy(currentUser);
                transactionRepository.save(tx);
            }
        }
    }
}
