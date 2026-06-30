document.addEventListener('DOMContentLoaded', () => {
    const transferId = document.getElementById('transferId').value;
    let currentTransfer = null;

    const fields = {
        transferCode: document.getElementById('transferCode'),
        fromBranchIdText: document.getElementById('fromBranchIdText'),
        toBranchIdText: document.getElementById('toBranchIdText'),
        statusText: document.getElementById('statusText'),
        totalValueText: document.getElementById('totalValueText'),
        requestedByText: document.getElementById('requestedByText'),
        requestedAtText: document.getElementById('requestedAtText'),
        noteText: document.getElementById('noteText'),
        detailBody: document.getElementById('transferDetailBody'),
        sendItems: document.getElementById('sendItems'),
        receiveItems: document.getElementById('receiveItems')
    };

    function canApprove(status) {
        return ['SUBMITTED', 'PENDING_ADMIN_APPROVAL'].includes(status);
    }

    function updateButtons(status) {
        document.getElementById('approveBtn').disabled = !canApprove(status);
        document.getElementById('rejectBtn').disabled = !canApprove(status);
        document.getElementById('sendBtn').disabled = status !== 'APPROVED';
        document.getElementById('receiveBtn').disabled = status !== 'IN_TRANSIT';
        document.getElementById('cancelBtn').disabled = ['RECEIVED', 'CANCELLED', 'REJECTED'].includes(status);
        document.getElementById('sendPanel').style.display = status === 'APPROVED' ? 'block' : 'none';
        document.getElementById('receivePanel').style.display = status === 'IN_TRANSIT' ? 'block' : 'none';
    }

    function renderDetailRows(details) {
        if (!details || details.length === 0) {
            fields.detailBody.innerHTML = '<tr><td colspan="6" class="empty-state">Không có chi tiết thuốc</td></tr>';
            return;
        }
        fields.detailBody.innerHTML = details.map(d => `
            <tr>
                <td>${d.stockTransferDetailId}</td>
                <td>${d.medicineId}</td>
                <td>${d.fromInventoryBatchId ?? '-'}</td>
                <td>${d.requestedQuantity ?? 0}</td>
                <td>${d.sentQuantity ?? '-'}</td>
                <td>${d.receivedQuantity ?? '-'}</td>
            </tr>
        `).join('');
    }

    function renderSendItems(details) {
        fields.sendItems.innerHTML = (details || []).map((d, index) => `
            <div class="item-box">
                <div class="item-header"><strong>Thuốc ID ${d.medicineId}</strong><span>Chi tiết #${d.stockTransferDetailId}</span></div>
                <div class="form-grid three">
                    <div class="form-group">
                        <label>ID chi tiết</label>
                        <input type="number" value="${d.stockTransferDetailId}" data-field="send.items[${index}].stockTransferDetailId" readonly>
                    </div>
                    <div class="form-group">
                        <label>ID lô xuất <span style="color:#dc2626">*</span></label>
                        <input type="number" min="1" data-field="send.items[${index}].fromInventoryBatchId" placeholder="VD: 1">
                        <div class="field-error" data-error-for="items[${index}].fromInventoryBatchId"></div>
                    </div>
                    <div class="form-group">
                        <label>Số lượng gửi <span style="color:#dc2626">*</span></label>
                        <input type="number" min="1" data-field="send.items[${index}].sentQuantity" value="${d.requestedQuantity ?? 1}">
                        <div class="field-error" data-error-for="items[${index}].sentQuantity"></div>
                    </div>
                </div>
            </div>
        `).join('');
    }

    function renderReceiveItems(details) {
        fields.receiveItems.innerHTML = (details || []).map((d, index) => `
            <div class="item-box">
                <div class="item-header"><strong>Thuốc ID ${d.medicineId}</strong><span>Chi tiết #${d.stockTransferDetailId}</span></div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>ID chi tiết</label>
                        <input type="number" value="${d.stockTransferDetailId}" data-field="receive.items[${index}].stockTransferDetailId" readonly>
                    </div>
                    <div class="form-group">
                        <label>Số lượng nhận <span style="color:#dc2626">*</span></label>
                        <input type="number" min="0" data-field="receive.items[${index}].receivedQuantity" value="${d.sentQuantity ?? d.requestedQuantity ?? 0}">
                        <div class="field-error" data-error-for="items[${index}].receivedQuantity"></div>
                    </div>
                </div>
            </div>
        `).join('');
    }

    async function loadTransfer() {
        try {
            const data = await Flow4.api(`/api/stock-transfers/${transferId}`);
            currentTransfer = data;
            fields.transferCode.textContent = data.transferCode || `Phiếu #${transferId}`;
            fields.fromBranchIdText.textContent = data.fromBranchId ?? '-';
            fields.toBranchIdText.textContent = data.toBranchId ?? '-';
            fields.statusText.innerHTML = Flow4.statusBadge(data.status);
            fields.totalValueText.textContent = Flow4.toVnd(data.totalValueAmount);
            fields.requestedByText.textContent = data.requestedBy ?? '-';
            fields.requestedAtText.textContent = Flow4.toDateTime(data.requestedAt);
            fields.noteText.textContent = data.note || '-';
            renderDetailRows(data.details);
            renderSendItems(data.details);
            renderReceiveItems(data.details);
            updateButtons(data.status);
        } catch (error) {
            Flow4.handleError(error);
        }
    }

    async function approveOrReject(type) {
        const isApprove = type === 'approve';
        const result = await Flow4.confirm({
            title: isApprove ? 'Duyệt phiếu điều chuyển' : 'Từ chối phiếu điều chuyển',
            message: isApprove ? 'Xác nhận duyệt phiếu điều chuyển này?' : 'Xác nhận từ chối phiếu điều chuyển này?',
            reasonVisible: true,
            okText: isApprove ? 'Duyệt' : 'Từ chối'
        });
        if (!result.ok) return;
        try {
            await Flow4.api(`/api/stock-transfers/${transferId}/${type}`, {
                method: 'POST',
                body: JSON.stringify({ approvedBy: 1, reason: result.reason })
            });
            Flow4.showToast('Thành công', isApprove ? 'Đã duyệt phiếu' : 'Đã từ chối phiếu');
            loadTransfer();
        } catch (error) {
            Flow4.handleError(error);
        }
    }

    function validateSendForm() {
        const form = document.getElementById('sendForm');
        Flow4.clearFieldErrors(form);
        let ok = Flow4.validateRequiredNumber('sentBy', 'Vui lòng nhập người gửi', form);
        (currentTransfer.details || []).forEach((d, index) => {
            const batch = form.querySelector(`[data-field="send.items[${index}].fromInventoryBatchId"]`);
            const qty = form.querySelector(`[data-field="send.items[${index}].sentQuantity"]`);
            if (!batch.value || Number(batch.value) <= 0) {
                Flow4.setFieldError(`items[${index}].fromInventoryBatchId`, 'Vui lòng nhập ID lô xuất', form);
                batch.classList.add('invalid'); ok = false;
            }
            if (!qty.value || Number(qty.value) <= 0) {
                Flow4.setFieldError(`items[${index}].sentQuantity`, 'Số lượng gửi phải lớn hơn 0', form);
                qty.classList.add('invalid'); ok = false;
            }
        });
        return ok;
    }

    function validateReceiveForm() {
        const form = document.getElementById('receiveForm');
        Flow4.clearFieldErrors(form);
        let ok = Flow4.validateRequiredNumber('receivedBy', 'Vui lòng nhập người nhận', form);
        (currentTransfer.details || []).forEach((d, index) => {
            const qty = form.querySelector(`[data-field="receive.items[${index}].receivedQuantity"]`);
            if (qty.value === '' || Number(qty.value) < 0) {
                Flow4.setFieldError(`items[${index}].receivedQuantity`, 'Số lượng nhận không được âm', form);
                qty.classList.add('invalid'); ok = false;
            }
        });
        return ok;
    }

    document.getElementById('approveBtn').addEventListener('click', () => approveOrReject('approve'));
    document.getElementById('rejectBtn').addEventListener('click', () => approveOrReject('reject'));
    document.getElementById('cancelBtn').addEventListener('click', async () => {
        const result = await Flow4.confirm({ title: 'Hủy phiếu', message: 'Bạn có chắc muốn hủy phiếu này?', reasonVisible: true, okText: 'Hủy phiếu' });
        if (!result.ok) return;
        try {
            await Flow4.api(`/api/stock-transfers/${transferId}/cancel?userId=1&reason=${encodeURIComponent(result.reason || '')}`, { method: 'POST' });
            Flow4.showToast('Thành công', 'Đã hủy phiếu');
            loadTransfer();
        } catch (error) { Flow4.handleError(error); }
    });

    document.getElementById('sendForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!validateSendForm()) return;
        const form = event.currentTarget;
        const items = (currentTransfer.details || []).map((d, index) => ({
            stockTransferDetailId: d.stockTransferDetailId,
            fromInventoryBatchId: Number(form.querySelector(`[data-field="send.items[${index}].fromInventoryBatchId"]`).value),
            sentQuantity: Number(form.querySelector(`[data-field="send.items[${index}].sentQuantity"]`).value)
        }));
        try {
            await Flow4.api(`/api/stock-transfers/${transferId}/send`, {
                method: 'POST',
                body: JSON.stringify({ sentBy: Number(document.getElementById('sentBy').value), items })
            });
            Flow4.showToast('Thành công', 'Đã xác nhận gửi hàng');
            loadTransfer();
        } catch (error) { Flow4.handleError(error, form); }
    });

    document.getElementById('receiveForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!validateReceiveForm()) return;
        const form = event.currentTarget;
        const items = (currentTransfer.details || []).map((d, index) => ({
            stockTransferDetailId: d.stockTransferDetailId,
            receivedQuantity: Number(form.querySelector(`[data-field="receive.items[${index}].receivedQuantity"]`).value)
        }));
        try {
            await Flow4.api(`/api/stock-transfers/${transferId}/receive`, {
                method: 'POST',
                body: JSON.stringify({ receivedBy: Number(document.getElementById('receivedBy').value), items })
            });
            Flow4.showToast('Thành công', 'Đã xác nhận nhận hàng');
            loadTransfer();
        } catch (error) { Flow4.handleError(error, form); }
    });

    loadTransfer();
});
