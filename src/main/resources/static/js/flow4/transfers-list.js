document.addEventListener('DOMContentLoaded', () => {
    const body = document.getElementById('transferTableBody');
    const branchFilter = document.getElementById('branchFilter');
    const reloadBtn = document.getElementById('reloadTransfersBtn');

    function renderRows(items) {
        if (!items || items.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="empty-state">Chưa có phiếu điều chuyển</td></tr>';
            return;
        }

        body.innerHTML = items.map(item => `
            <tr>
                <td>${Flow4.escapeHtml(item.transferCode)}</td>
                <td>${item.fromBranchId ?? '-'}</td>
                <td>${item.toBranchId ?? '-'}</td>
                <td>${Flow4.statusBadge(item.status)}</td>
                <td>${Flow4.toVnd(item.totalValueAmount)}</td>
                <td>${Flow4.toDateTime(item.requestedAt)}</td>
                <td>
                    <a class="btn btn-light" href="/flow4/transfers/${item.stockTransferId}">Chi tiết</a>
                </td>
            </tr>
        `).join('');
    }

    async function loadTransfers() {
        try {
            const query = Flow4.getQuery({ branchId: branchFilter.value });
            const data = await Flow4.api(`/api/stock-transfers${query}`);
            renderRows(data);
        } catch (error) {
            body.innerHTML = '<tr><td colspan="7" class="empty-state">Không tải được dữ liệu</td></tr>';
            Flow4.handleError(error);
        }
    }

    reloadBtn.addEventListener('click', loadTransfers);
    loadTransfers();
});
