package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.GoodsReceipt;
import java.util.List;

public interface GoodsReceiptService {
    GoodsReceipt getReceiptById(Integer id);
    List<GoodsReceipt> getReceiptsByBranch(Integer branchId);
    List<GoodsReceipt> getPendingApprovalReceipts();
    GoodsReceipt saveReceipt(GoodsReceipt receipt);
    void submitAndCheckVariance(Integer receiptId);
    void approveVariance(Integer receiptId, Integer adminId, String adminNotes);
    void rejectReceipt(Integer receiptId, Integer adminId, String adminNotes);
}
