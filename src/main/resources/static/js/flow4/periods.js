document.addEventListener('DOMContentLoaded', () => {
    const now = new Date();
    document.getElementById('year').value = now.getFullYear();
    document.getElementById('month').value = now.getMonth() + 1;

    const lockForm = document.getElementById('periodLockForm');
    const unlockForm = document.getElementById('periodUnlockForm');

    lockForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        Flow4.clearFieldErrors(lockForm);
        let ok = true;
        const year = Number(document.getElementById('year').value);
        const month = Number(document.getElementById('month').value);
        if (!year || year < 2020) { Flow4.setFieldError('year', 'Năm không hợp lệ', lockForm); ok = false; }
        if (!month || month < 1 || month > 12) { Flow4.setFieldError('month', 'Tháng phải từ 1 đến 12', lockForm); ok = false; }
        if (!Flow4.validateRequiredNumber('lockedBy', 'Vui lòng nhập người khóa kỳ', lockForm)) ok = false;
        if (!ok) return;

        const result = await Flow4.confirm({ title: 'Khóa kỳ', message: `Xác nhận khóa kỳ ${month}/${year}?`, reasonVisible: false, okText: 'Khóa kỳ' });
        if (!result.ok) return;

        try {
            await Flow4.api('/api/accounting-periods/lock', {
                method: 'POST',
                body: JSON.stringify({ year, month, lockedBy: Number(document.getElementById('lockedBy').value) })
            });
            Flow4.showToast('Thành công', 'Đã khóa kỳ kế toán');
        } catch (error) { Flow4.handleError(error, lockForm); }
    });

    unlockForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        Flow4.clearFieldErrors(unlockForm);
        let ok = true;
        if (!Flow4.validateRequiredNumber('periodId', 'Vui lòng nhập ID kỳ', unlockForm)) ok = false;
        if (!Flow4.validateRequiredNumber('unlockedBy', 'Vui lòng nhập người mở khóa', unlockForm)) ok = false;
        if (!document.getElementById('reason').value.trim()) {
            Flow4.setFieldError('reason', 'Vui lòng nhập lý do mở khóa', unlockForm); ok = false;
        }
        if (!ok) return;

        try {
            await Flow4.api(`/api/accounting-periods/${document.getElementById('periodId').value}/unlock`, {
                method: 'POST',
                body: JSON.stringify({
                    unlockedBy: Number(document.getElementById('unlockedBy').value),
                    reason: document.getElementById('reason').value.trim()
                })
            });
            Flow4.showToast('Thành công', 'Đã mở khóa kỳ');
        } catch (error) { Flow4.handleError(error, unlockForm); }
    });
});
