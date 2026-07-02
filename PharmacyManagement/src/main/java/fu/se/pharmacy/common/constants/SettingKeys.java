package fu.se.pharmacy.common.constants;

public final class SettingKeys {
    private SettingKeys() {}

    public static final String PURCHASE_APPROVAL_LIMIT = "PURCHASE_APPROVAL_LIMIT";
    public static final String STOCK_VARIANCE_LIMIT = "STOCK_VARIANCE_LIMIT";
    public static final String TRANSFER_APPROVAL_LIMIT = "TRANSFER_APPROVAL_LIMIT";
    public static final String EXPENSE_APPROVAL_LIMIT = "EXPENSE_APPROVAL_LIMIT";
    public static final String REFUND_APPROVAL_LIMIT = "REFUND_APPROVAL_LIMIT";
    public static final String EXPIRY_WARNING_DAYS = "EXPIRY_WARNING_DAYS";
    // Không có trong bảng seed gốc nhưng cần cho luồng chốt ca tiền mặt (Người 3);
    // dùng chung cơ chế đọc từ system_settings, mặc định 500_000 nếu Admin chưa cấu hình.
    public static final String CASH_SHIFT_VARIANCE_LIMIT = "CASH_SHIFT_VARIANCE_LIMIT";
}
