package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.StockTransferCreateRequest;
import fu.se.pharmacy.dto.StockTransferReceiveRequest;
import fu.se.pharmacy.dto.StockTransferSendRequest;
import fu.se.pharmacy.entity.StockTransfer;

import java.util.List;

public interface StockTransferService {
    StockTransfer createTransfer(StockTransferCreateRequest request);
    StockTransfer approveTransfer(Integer transferId, ApprovalRequest request);
    StockTransfer rejectTransfer(Integer transferId, ApprovalRequest request);
    StockTransfer confirmSent(Integer transferId, StockTransferSendRequest request);
    StockTransfer confirmReceived(Integer transferId, StockTransferReceiveRequest request);
    StockTransfer cancelTransfer(Integer transferId, Integer userId, String reason);
    StockTransfer getById(Integer transferId);
    List<StockTransfer> getAll();
    List<StockTransfer> getByBranch(Integer branchId);
}
