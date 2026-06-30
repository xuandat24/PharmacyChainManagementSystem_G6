document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('transferCreateForm');
    const itemContainer = document.getElementById('transferItems');
    const addBtn = document.getElementById('addTransferItemBtn');

    function createItemRow(index) {
        const wrapper = document.createElement('div');
        wrapper.className = 'item-box';
        wrapper.innerHTML = `
            <div class="item-header">
                <strong>Thuốc #${index + 1}</strong>
                <button type="button" class="btn btn-light remove-item-btn">Xóa</button>
            </div>
            <div class="form-grid">
                <div class="form-group">
                    <label>ID thuốc <span style="color:#dc2626">*</span></label>
                    <input type="number" min="1" data-field="items[${index}].medicineId" placeholder="VD: 1">
                    <div class="field-error" data-error-for="items[${index}].medicineId"></div>
                </div>
                <div class="form-group">
                    <label>Số lượng yêu cầu <span style="color:#dc2626">*</span></label>
                    <input type="number" min="1" data-field="items[${index}].requestedQuantity" placeholder="VD: 10">
                    <div class="field-error" data-error-for="items[${index}].requestedQuantity"></div>
                </div>
            </div>
        `;
        wrapper.querySelector('.remove-item-btn').addEventListener('click', () => {
            wrapper.remove();
            renumberItems();
        });
        return wrapper;
    }

    function renumberItems() {
        [...itemContainer.children].forEach((box, index) => {
            box.querySelector('.item-header strong').textContent = `Thuốc #${index + 1}`;
            const inputs = box.querySelectorAll('input');
            inputs[0].setAttribute('data-field', `items[${index}].medicineId`);
            inputs[1].setAttribute('data-field', `items[${index}].requestedQuantity`);
            const errors = box.querySelectorAll('.field-error');
            errors[0].setAttribute('data-error-for', `items[${index}].medicineId`);
            errors[1].setAttribute('data-error-for', `items[${index}].requestedQuantity`);
        });
    }

    function addItem() {
        itemContainer.appendChild(createItemRow(itemContainer.children.length));
    }

    function getPayload() {
        const items = [...itemContainer.children].map((box, index) => {
            const medicineId = box.querySelector(`[data-field="items[${index}].medicineId"]`).value;
            const requestedQuantity = box.querySelector(`[data-field="items[${index}].requestedQuantity"]`).value;
            return {
                medicineId: medicineId ? Number(medicineId) : null,
                requestedQuantity: requestedQuantity ? Number(requestedQuantity) : null
            };
        });

        return {
            fromBranchId: numberOrNull('fromBranchId'),
            toBranchId: numberOrNull('toBranchId'),
            requestedBy: numberOrNull('requestedBy'),
            note: document.getElementById('note').value.trim(),
            items
        };
    }

    function numberOrNull(id) {
        const value = document.getElementById(id).value;
        return value ? Number(value) : null;
    }

    function clientValidate() {
        Flow4.clearFieldErrors(form);
        let ok = true;
        if (!Flow4.validateRequiredNumber('fromBranchId', 'Vui lòng nhập chi nhánh gửi', form)) ok = false;
        if (!Flow4.validateRequiredNumber('toBranchId', 'Vui lòng nhập chi nhánh nhận', form)) ok = false;
        if (!Flow4.validateRequiredNumber('requestedBy', 'Vui lòng nhập người tạo yêu cầu', form)) ok = false;
        if (document.getElementById('fromBranchId').value && document.getElementById('toBranchId').value && document.getElementById('fromBranchId').value === document.getElementById('toBranchId').value) {
            Flow4.setFieldError('toBranchId', 'Chi nhánh nhận không được trùng chi nhánh gửi', form);
            ok = false;
        }
        if (itemContainer.children.length === 0) {
            Flow4.setFieldError('items', 'Phiếu điều chuyển phải có ít nhất một thuốc', form);
            ok = false;
        }
        [...itemContainer.children].forEach((box, index) => {
            const medicine = box.querySelector(`[data-field="items[${index}].medicineId"]`);
            const quantity = box.querySelector(`[data-field="items[${index}].requestedQuantity"]`);
            if (!medicine.value || Number(medicine.value) <= 0) {
                Flow4.setFieldError(`items[${index}].medicineId`, 'Vui lòng nhập ID thuốc hợp lệ', form);
                medicine.classList.add('invalid');
                ok = false;
            }
            if (!quantity.value || Number(quantity.value) <= 0) {
                Flow4.setFieldError(`items[${index}].requestedQuantity`, 'Số lượng phải lớn hơn 0', form);
                quantity.classList.add('invalid');
                ok = false;
            }
        });
        return ok;
    }

    addBtn.addEventListener('click', addItem);

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!clientValidate()) {
            Flow4.showToast('Dữ liệu chưa hợp lệ', 'Vui lòng kiểm tra lại các ô nhập liệu màu đỏ.', 'warning');
            return;
        }
        try {
            const data = await Flow4.api('/api/stock-transfers', {
                method: 'POST',
                body: JSON.stringify(getPayload())
            });
            Flow4.showToast('Thành công', 'Đã tạo phiếu điều chuyển');
            setTimeout(() => window.location.href = `/flow4/transfers/${data.stockTransferId}`, 700);
        } catch (error) {
            Flow4.handleError(error, form);
        }
    });

    addItem();
});
