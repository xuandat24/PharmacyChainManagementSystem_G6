package fu.se2033.pharmacy.service;

import fu.se2033.pharmacy.dto.flow4.ApprovalRequest;
import fu.se2033.pharmacy.dto.flow4.StockTransferCreateRequest;
import fu.se2033.pharmacy.dto.flow4.StockTransferReceiveRequest;
import fu.se2033.pharmacy.dto.flow4.StockTransferSendRequest;
import fu.se2033.pharmacy.entity.StockTransfer;

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
