package fu.se2033.pharmacy.dto.flow4;

public class DashboardSummaryResponse {
    private Integer todayRevenue;
    private Integer todayCashRevenue;
    private Integer todayOnlineRevenue;
    private Integer todaySalesCount;
    private Integer todayGoodsReceiptAmount;
    private Integer todayExpenseAmount;
    private Integer lowStockCount;
    private Integer nearExpiryCount;
    private Integer pendingTransferCount;
    private Integer pendingExpenseCount;

    public Integer getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(Integer todayRevenue) { this.todayRevenue = todayRevenue; }
    public Integer getTodayCashRevenue() { return todayCashRevenue; }
    public void setTodayCashRevenue(Integer todayCashRevenue) { this.todayCashRevenue = todayCashRevenue; }
    public Integer getTodayOnlineRevenue() { return todayOnlineRevenue; }
    public void setTodayOnlineRevenue(Integer todayOnlineRevenue) { this.todayOnlineRevenue = todayOnlineRevenue; }
    public Integer getTodaySalesCount() { return todaySalesCount; }
    public void setTodaySalesCount(Integer todaySalesCount) { this.todaySalesCount = todaySalesCount; }
    public Integer getTodayGoodsReceiptAmount() { return todayGoodsReceiptAmount; }
    public void setTodayGoodsReceiptAmount(Integer todayGoodsReceiptAmount) { this.todayGoodsReceiptAmount = todayGoodsReceiptAmount; }
    public Integer getTodayExpenseAmount() { return todayExpenseAmount; }
    public void setTodayExpenseAmount(Integer todayExpenseAmount) { this.todayExpenseAmount = todayExpenseAmount; }
    public Integer getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(Integer lowStockCount) { this.lowStockCount = lowStockCount; }
    public Integer getNearExpiryCount() { return nearExpiryCount; }
    public void setNearExpiryCount(Integer nearExpiryCount) { this.nearExpiryCount = nearExpiryCount; }
    public Integer getPendingTransferCount() { return pendingTransferCount; }
    public void setPendingTransferCount(Integer pendingTransferCount) { this.pendingTransferCount = pendingTransferCount; }
    public Integer getPendingExpenseCount() { return pendingExpenseCount; }
    public void setPendingExpenseCount(Integer pendingExpenseCount) { this.pendingExpenseCount = pendingExpenseCount; }
}
