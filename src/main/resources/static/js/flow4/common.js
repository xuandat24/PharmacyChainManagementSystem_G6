(function () {
    const Flow4 = {};

    Flow4.getCsrfHeaders = function () {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (token && header) {
            return { [header]: token };
        }
        return {};
    };

    Flow4.api = async function (url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...Flow4.getCsrfHeaders(),
            ...(options.headers || {})
        };

        const response = await fetch(url, {
            ...options,
            headers
        });

        let data = null;
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            data = text ? { message: text } : null;
        }

        if (!response.ok) {
            const error = data || { message: 'Không thể xử lý yêu cầu' };
            error.httpStatus = response.status;
            throw error;
        }

        return data;
    };

    Flow4.toVnd = function (value) {
        const n = Number(value || 0);
        return n.toLocaleString('vi-VN') + 'đ';
    };

    Flow4.toDate = function (value) {
        if (!value) return '-';
        return String(value).substring(0, 10);
    };

    Flow4.toDateTime = function (value) {
        if (!value) return '-';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
        return date.toLocaleString('vi-VN');
    };

    Flow4.escapeHtml = function (value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    };

    Flow4.statusBadge = function (status) {
        const s = status || 'UNKNOWN';
        let cls = 'gray';
        if (['APPROVED', 'RECEIVED', 'PAID', 'LOCKED'].includes(s)) cls = 'green';
        if (['SUBMITTED', 'IN_TRANSIT'].includes(s)) cls = 'blue';
        if (['PENDING_ADMIN_APPROVAL', 'DRAFT'].includes(s)) cls = 'orange';
        if (['REJECTED', 'CANCELLED'].includes(s)) cls = 'red';
        if (['REFUNDED', 'VOIDED'].includes(s)) cls = 'purple';
        return `<span class="badge ${cls}">${Flow4.escapeHtml(s)}</span>`;
    };

    Flow4.clearFieldErrors = function (scope = document) {
        scope.querySelectorAll('.field-error').forEach(el => el.textContent = '');
        scope.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
    };

    Flow4.setFieldError = function (field, message, scope = document) {
        let errorElement = null;
        scope.querySelectorAll('[data-error-for]').forEach(el => {
            if (el.getAttribute('data-error-for') === field) {
                errorElement = el;
            }
        });
        if (errorElement) {
            errorElement.textContent = message;
        }

        let inputElement = null;
        scope.querySelectorAll('input, select, textarea').forEach(el => {
            if (el.name === field || el.id === field || el.getAttribute('data-field') === field) {
                inputElement = el;
            }
        });
        if (inputElement) {
            inputElement.classList.add('invalid');
        }
    };

    Flow4.setFieldErrors = function (fieldErrors, scope = document) {
        Flow4.clearFieldErrors(scope);
        if (!fieldErrors) return;
        Object.keys(fieldErrors).forEach(key => Flow4.setFieldError(key, fieldErrors[key], scope));
    };

    Flow4.validateRequiredNumber = function (fieldId, message, scope = document) {
        const input = scope.getElementById ? scope.getElementById(fieldId) : document.getElementById(fieldId);
        if (!input || input.value === '' || Number(input.value) <= 0) {
            Flow4.setFieldError(fieldId, message, scope);
            return false;
        }
        return true;
    };

    Flow4.showToast = function (title, message, type = 'success') {
        const wrap = document.querySelector('.toast-wrap') || document.body.appendChild(document.createElement('div'));
        wrap.classList.add('toast-wrap');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<h4>${Flow4.escapeHtml(title)}</h4><p>${Flow4.escapeHtml(message)}</p>`;
        wrap.appendChild(toast);
        setTimeout(() => toast.remove(), 4200);
    };

    Flow4.handleError = function (error, scope = document) {
        if (error && error.fieldErrors) {
            Flow4.setFieldErrors(error.fieldErrors, scope);
            Flow4.showToast('Dữ liệu chưa hợp lệ', error.message || 'Vui lòng kiểm tra lại các ô nhập liệu màu đỏ.', 'warning');
            return;
        }
        const message = error?.message || 'Lỗi hệ thống, vui lòng kiểm tra lại.';
        Flow4.showToast('Có lỗi xảy ra', message, 'error');
    };

    Flow4.confirm = function ({ title = 'Xác nhận', message = 'Bạn có chắc chắn?', reasonVisible = true, okText = 'Xác nhận' }) {
        return new Promise(resolve => {
            const modal = document.getElementById('confirmModal');
            const titleEl = document.getElementById('confirmTitle');
            const messageEl = document.getElementById('confirmMessage');
            const reasonGroup = document.getElementById('confirmReasonGroup');
            const reasonInput = document.getElementById('confirmReason');
            const okBtn = document.getElementById('confirmOkBtn');
            const cancelBtn = document.getElementById('confirmCancelBtn');

            if (!modal || !okBtn || !cancelBtn) {
                const ok = window.confirm(message);
                resolve({ ok, reason: '' });
                return;
            }

            titleEl.textContent = title;
            messageEl.textContent = message;
            okBtn.textContent = okText;
            reasonInput.value = '';
            reasonGroup.style.display = reasonVisible ? 'flex' : 'none';
            modal.classList.add('show');

            const cleanup = () => {
                modal.classList.remove('show');
                okBtn.removeEventListener('click', onOk);
                cancelBtn.removeEventListener('click', onCancel);
            };
            const onOk = () => {
                const reason = reasonInput.value.trim();
                cleanup();
                resolve({ ok: true, reason });
            };
            const onCancel = () => {
                cleanup();
                resolve({ ok: false, reason: '' });
            };

            okBtn.addEventListener('click', onOk);
            cancelBtn.addEventListener('click', onCancel);
        });
    };

    Flow4.getQuery = function (params) {
        const query = new URLSearchParams();
        Object.keys(params).forEach(key => {
            const value = params[key];
            if (value !== null && value !== undefined && value !== '') {
                query.append(key, value);
            }
        });
        const qs = query.toString();
        return qs ? `?${qs}` : '';
    };

    Flow4.today = function () {
        return new Date().toISOString().substring(0, 10);
    };

    window.Flow4 = Flow4;
})();
