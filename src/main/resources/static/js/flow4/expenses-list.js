document.addEventListener('DOMContentLoaded', () => {
    const body = document.getElementById('expenseTableBody');
    const branchFilter = document.getElementById('branchFilter');
    const reloadBtn = document.getElementById('reloadExpensesBtn');

    async function loadExpenses() {
        try {
            const query = Flow4.getQuery({ branchId: branchFilter.value });
            const data = await Flow4.api(`/api/expenses${query}`);
            renderRows(data);
        } catch (error) {
            body.innerHTML = '<tr><td colspan="7" class="empty-state">Không tải được dữ liệu</td></tr>';
            Flow4.handleError(error);
        }
    }

    function renderRows(items) {
        if (!items || items.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="empty-state">Chưa có phiếu chi</td></tr>';
            return;
        }
        body.innerHTML = items.map(item => `
            <tr>
                <td>${Flow4.escapeHtml(item.expenseCode)}</td>
                <td>${item.branchId ?? '-'}</td>
                <td>${Flow4.escapeHtml(item.expenseType)}</td>
                <td>${Flow4.toVnd(item.amount)}</td>
                <td>${Flow4.toDate(item.expenseDate)}</td>
                <td>${Flow4.statusBadge(item.status)}</td>
                <td>
                    <div class="action-row">
                        <button class="btn btn-light" data-action="submit" data-id="${item.expenseVoucherId}" ${item.status !== 'DRAFT' ? 'disabled' : ''}>Gửi duyệt</button>
                        <button class="btn btn-primary" data-action="approve" data-id="${item.expenseVoucherId}" ${!['SUBMITTED'].includes(item.status) ? 'disabled' : ''}>Duyệt</button>
                        <button class="btn btn-danger" data-action="reject" data-id="${item.expenseVoucherId}" ${!['SUBMITTED'].includes(item.status) ? 'disabled' : ''}>Từ chối</button>
                        <button class="btn btn-blue" data-action="paid" data-id="${item.expenseVoucherId}" ${item.status !== 'APPROVED' ? 'disabled' : ''}>Đã chi</button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    body.addEventListener('click', async (event) => {
        const btn = event.target.closest('button[data-action]');
        if (!btn || btn.disabled) return;
        const id = btn.dataset.id;
        const action = btn.dataset.action;
        try {
            if (action === 'submit') {
                await Flow4.api(`/api/expenses/${id}/submit`, { method: 'POST' });
                Flow4.showToast('Thành công', 'Đã gửi duyệt phiếu chi');
            }
            if (action === 'approve' || action === 'reject') {
                const isApprove = action === 'approve';
                const result = await Flow4.confirm({
                    title: isApprove ? 'Duyệt phiếu chi' : 'Từ chối phiếu chi',
                    message: isApprove ? 'Xác nhận duyệt phiếu chi này?' : 'Xác nhận từ chối phiếu chi này?',
                    reasonVisible: true,
                    okText: isApprove ? 'Duyệt' : 'Từ chối'
                });
                if (!result.ok) return;
                await Flow4.api(`/api/expenses/${id}/${action}`, {
                    method: 'POST',
                    body: JSON.stringify({ approvedBy: 1, reason: result.reason })
                });
                Flow4.showToast('Thành công', isApprove ? 'Đã duyệt phiếu chi' : 'Đã từ chối phiếu chi');
            }
            if (action === 'paid') {
                await Flow4.api(`/api/expenses/${id}/mark-paid?userId=1`, { method: 'POST' });
                Flow4.showToast('Thành công', 'Đã đánh dấu phiếu chi đã thanh toán');
            }
            loadExpenses();
        } catch (error) {
            Flow4.handleError(error);
        }
    });

    reloadBtn.addEventListener('click', loadExpenses);
    loadExpenses();
});
