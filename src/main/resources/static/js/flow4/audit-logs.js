document.addEventListener('DOMContentLoaded', () => {
    const body = document.getElementById('auditBody');

    async function loadAuditLogs() {
        try {
            const data = await Flow4.api('/api/audit-logs');
            if (!data || data.length === 0) {
                body.innerHTML = '<tr><td colspan="6" class="empty-state">Chưa có audit log</td></tr>';
                return;
            }
            body.innerHTML = data.map(item => `
                <tr>
                    <td>${Flow4.toDateTime(item.createdAt)}</td>
                    <td>${item.userId ?? '-'}</td>
                    <td>${item.branchId ?? '-'}</td>
                    <td>${Flow4.escapeHtml(item.action)}</td>
                    <td>${Flow4.escapeHtml(item.targetType)} #${item.targetId ?? '-'}</td>
                    <td>${Flow4.escapeHtml(item.reason || '-')}</td>
                </tr>
            `).join('');
        } catch (error) {
            body.innerHTML = '<tr><td colspan="6" class="empty-state">Không tải được dữ liệu</td></tr>';
            Flow4.handleError(error);
        }
    }

    document.getElementById('reloadAuditBtn').addEventListener('click', loadAuditLogs);
    loadAuditLogs();
});
