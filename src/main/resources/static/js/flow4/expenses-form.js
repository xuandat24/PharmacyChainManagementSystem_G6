document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('expenseCreateForm');
    document.getElementById('expenseDate').value = Flow4.today();

    function numberOrNull(id) {
        const value = document.getElementById(id).value;
        return value ? Number(value) : null;
    }

    function getPayload() {
        return {
            branchId: numberOrNull('branchId'),
            createdBy: numberOrNull('createdBy'),
            expenseType: document.getElementById('expenseType').value || null,
            amount: numberOrNull('amount'),
            expenseDate: document.getElementById('expenseDate').value || null,
            documentNo: document.getElementById('documentNo').value.trim(),
            description: document.getElementById('description').value.trim()
        };
    }

    function clientValidate() {
        Flow4.clearFieldErrors(form);
        let ok = true;
        if (!Flow4.validateRequiredNumber('branchId', 'Vui lòng nhập chi nhánh', form)) ok = false;
        if (!Flow4.validateRequiredNumber('createdBy', 'Vui lòng nhập người tạo phiếu', form)) ok = false;
        if (!document.getElementById('expenseType').value) {
            Flow4.setFieldError('expenseType', 'Vui lòng chọn loại chi phí', form); ok = false;
        }
        if (!Flow4.validateRequiredNumber('amount', 'Số tiền phải lớn hơn 0', form)) ok = false;
        if (!document.getElementById('expenseDate').value) {
            Flow4.setFieldError('expenseDate', 'Vui lòng chọn ngày chi', form); ok = false;
        }
        if (!document.getElementById('description').value.trim()) {
            Flow4.setFieldError('description', 'Vui lòng nhập nội dung chi', form); ok = false;
        }
        return ok;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!clientValidate()) {
            Flow4.showToast('Dữ liệu chưa hợp lệ', 'Vui lòng kiểm tra lại các ô nhập liệu màu đỏ.', 'warning');
            return;
        }
        try {
            await Flow4.api('/api/expenses', {
                method: 'POST',
                body: JSON.stringify(getPayload())
            });
            Flow4.showToast('Thành công', 'Đã tạo phiếu chi');
            setTimeout(() => window.location.href = '/flow4/expenses', 700);
        } catch (error) {
            Flow4.handleError(error, form);
        }
    });
});
