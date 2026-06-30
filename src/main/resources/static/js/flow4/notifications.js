document.addEventListener('DOMContentLoaded', () => {
    const body = document.getElementById('notificationBody');
    const userId = document.getElementById('userId');
    const branchId = document.getElementById('branchId');
    const reloadBtn = document.getElementById('reloadNotificationsBtn');

    async function loadNotifications() {
        try {
            const query = Flow4.getQuery({ userId: userId.value, branchId: branchId.value });
            const data = await Flow4.api(`/api/notifications${query}`);
            if (!data || data.length === 0) {
                body.innerHTML = '<tr><td colspan="6" class="empty-state">Chưa có thông báo</td></tr>';
                return;
            }
            body.innerHTML = data.map(item => `
                <tr>
                    <td>${Flow4.toDateTime(item.createdAt)}</td>
                    <td>${Flow4.escapeHtml(item.notificationType)}</td>
                    <td>${Flow4.escapeHtml(item.title)}</td>
                    <td>${Flow4.escapeHtml(item.message)}</td>
                    <td>${item.read ? Flow4.statusBadge('READ') : Flow4.statusBadge('NEW')}</td>
                    <td><button class="btn btn-light" data-id="${item.notificationId}" ${item.read ? 'disabled' : ''}>Đánh dấu đã đọc</button></td>
                </tr>
            `).join('');
        } catch (error) {
            body.innerHTML = '<tr><td colspan="6" class="empty-state">Không tải được dữ liệu</td></tr>';
            Flow4.handleError(error);
        }
    }

    body.addEventListener('click', async (event) => {
        const btn = event.target.closest('button[data-id]');
        if (!btn || btn.disabled) return;
        try {
            await Flow4.api(`/api/notifications/${btn.dataset.id}/read`, { method: 'POST' });
            Flow4.showToast('Thành công', 'Đã đánh dấu thông báo đã đọc');
            loadNotifications();
        } catch (error) { Flow4.handleError(error); }
    });

    reloadBtn.addEventListener('click', loadNotifications);
    loadNotifications();
});
