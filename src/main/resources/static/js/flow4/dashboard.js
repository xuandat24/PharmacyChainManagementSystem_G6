document.addEventListener('DOMContentLoaded', () => {
    const branchIdInput = document.getElementById('branchId');
    const reloadBtn = document.getElementById('reloadDashboardBtn');

    async function loadDashboard() {
        try {
            const query = Flow4.getQuery({ branchId: branchIdInput.value });
            const data = await Flow4.api(`/api/dashboard/today${query}`);
            document.getElementById('todayRevenue').textContent = Flow4.toVnd(data.todayRevenue);
            document.getElementById('todayCashRevenue').textContent = Flow4.toVnd(data.todayCashRevenue);
            document.getElementById('todayOnlineRevenue').textContent = Flow4.toVnd(data.todayOnlineRevenue);
            document.getElementById('todaySalesCount').textContent = data.todaySalesCount ?? 0;
            document.getElementById('todayGoodsReceiptAmount').textContent = Flow4.toVnd(data.todayGoodsReceiptAmount);
            document.getElementById('pendingTransferCount').textContent = data.pendingTransferCount ?? 0;
            document.getElementById('pendingExpenseCount').textContent = data.pendingExpenseCount ?? 0;
            document.getElementById('lowStockCount').textContent = data.lowStockCount ?? 0;
            document.getElementById('nearExpiryCount').textContent = data.nearExpiryCount ?? 0;
        } catch (error) {
            Flow4.handleError(error);
        }
    }

    reloadBtn.addEventListener('click', loadDashboard);
    loadDashboard();
});
