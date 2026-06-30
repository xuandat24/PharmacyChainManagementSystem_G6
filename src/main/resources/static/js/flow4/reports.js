document.addEventListener('DOMContentLoaded', () => {
    const fromDate = document.getElementById('fromDate');
    const toDate = document.getElementById('toDate');
    const branchId = document.getElementById('branchId');
    const reportTitle = document.getElementById('reportTitle');
    const reportHead = document.getElementById('reportHead');
    const reportBody = document.getElementById('reportBody');

    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().substring(0, 10);
    fromDate.value = firstDay;
    toDate.value = Flow4.today();

    function validateDate() {
        Flow4.clearFieldErrors(document);
        let ok = true;
        if (!fromDate.value) { Flow4.setFieldError('fromDate', 'Vui lòng chọn ngày bắt đầu'); ok = false; }
        if (!toDate.value) { Flow4.setFieldError('toDate', 'Vui lòng chọn ngày kết thúc'); ok = false; }
        if (fromDate.value && toDate.value && fromDate.value > toDate.value) {
            Flow4.setFieldError('toDate', 'Ngày kết thúc phải sau ngày bắt đầu'); ok = false;
        }
        return ok;
    }

    function renderGeneric(data) {
        if (!data || data.length === 0) {
            reportHead.innerHTML = '';
            reportBody.innerHTML = '<tr><td class="empty-state">Không có dữ liệu</td></tr>';
            return;
        }
        const keys = Object.keys(data[0]);
        reportHead.innerHTML = `<tr>${keys.map(k => `<th>${Flow4.escapeHtml(k)}</th>`).join('')}</tr>`;
        reportBody.innerHTML = data.map(row => `
            <tr>${keys.map(k => `<td>${formatValue(row[k])}</td>`).join('')}</tr>
        `).join('');
    }

    function formatValue(value) {
        if (value === null || value === undefined) return '-';
        if (typeof value === 'number') return value.toLocaleString('vi-VN');
        return Flow4.escapeHtml(value);
    }

    async function loadReport(type) {
        if (!validateDate()) return;
        const endpointMap = {
            revenue: '/api/reports/revenue',
            expenses: '/api/reports/expenses',
            inventory: '/api/reports/inventory-movements'
        };
        const titleMap = {
            revenue: 'Báo cáo doanh thu',
            expenses: 'Báo cáo chi phí',
            inventory: 'Báo cáo nhập xuất tồn'
        };
        try {
            const query = Flow4.getQuery({ fromDate: fromDate.value, toDate: toDate.value, branchId: branchId.value });
            const data = await Flow4.api(`${endpointMap[type]}${query}`);
            reportTitle.textContent = titleMap[type];
            renderGeneric(data);
        } catch (error) {
            Flow4.handleError(error);
        }
    }

    document.getElementById('loadRevenueBtn').addEventListener('click', () => loadReport('revenue'));
    document.getElementById('loadExpenseBtn').addEventListener('click', () => loadReport('expenses'));
    document.getElementById('loadInventoryBtn').addEventListener('click', () => loadReport('inventory'));
});
