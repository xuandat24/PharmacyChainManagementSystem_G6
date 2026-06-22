package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StockTransfer")
@Data
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransferID")
    private Integer transferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FromBranchID", nullable = false)
    private Branch fromBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToBranchID", nullable = false)
    private Branch toBranch;

    @Column(name = "TransferDate", nullable = false)
    private LocalDateTime transferDate = LocalDateTime.now();

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Pending"; // Pending, Approved, Rejected, Completed

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockTransferDetail> details = new ArrayList<>();
}
