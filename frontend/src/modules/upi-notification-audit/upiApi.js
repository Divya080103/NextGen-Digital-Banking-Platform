const API_BASE = '/api/v1';

/**
 * Shared request helper.
 *
 * UPI leans on the ApiError body far more than the HTTP status: INVALID_UPI_PIN, UPI_PIN_LOCKED,
 * UPI_DAILY_COUNT_EXCEEDED, QR_NOT_RECOGNIZED and QR_AMOUNT_MISMATCH all carry the meaning the
 * screen needs to show, so the thrown Error keeps `errorCode` and `status` intact.
 */
async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...headers },
    body: body ? JSON.stringify(body) : undefined
  });

  const raw = await res.text();
  let data = null;
  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = { message: raw };
    }
  }

  if (!res.ok) {
    const error = new Error(data?.message || `Request failed with status ${res.status}`);
    error.errorCode = data?.errorCode || `HTTP_${res.status}`;
    error.status = res.status;
    throw error;
  }
  return data;
}

export function fetchProfiles(customerId) {
  return request(`/upi/profile?customerId=${encodeURIComponent(customerId)}`);
}

export function createProfile({ customerId, vpa, defaultAccountId, upiPin }) {
  return request('/upi/profile', {
    method: 'POST',
    body: { customerId, vpa, defaultAccountId, upiPin }
  });
}

export function changePin({ vpa, currentUpiPin, newUpiPin }) {
  return request('/upi/pin', {
    method: 'POST',
    body: { vpa, currentUpiPin, newUpiPin }
  });
}

export function pay({ payerVpa, payeeVpa, amount, upiPin, qrPayload }, idempotencyKey) {
  return request('/upi/pay', {
    method: 'POST',
    headers: idempotencyKey ? { 'X-Idempotency-Key': idempotencyKey } : {},
    body: { payerVpa, payeeVpa, amount, upiPin, qrPayload: qrPayload || null }
  });
}

export function generateQr({ vpa, merchantName, fixedAmount, dynamic }) {
  return request('/upi/qr', {
    method: 'POST',
    body: {
      vpa,
      merchantName,
      fixedAmount: fixedAmount === '' || fixedAmount == null ? null : Number(fixedAmount),
      dynamic
    }
  });
}

export function scanQr(qrPayload) {
  return request('/upi/qr/scan', {
    method: 'POST',
    body: { qrPayload }
  });
}

export function newIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `key-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
