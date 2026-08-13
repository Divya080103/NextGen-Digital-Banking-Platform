const API_BASE = '/api/v1';

/**
 * Shared request helper.
 *
 * The Transaction Engine communicates business-rule rejections through the ApiError body
 * ({ statusCode, errorCode, message }), not just the HTTP status. Those rejections are the
 * interesting part of this module — daily ceilings, duplicate shields, idempotency — so the
 * thrown Error carries `errorCode` and `status` through to the UI instead of being flattened
 * into a generic "request failed".
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

export function fetchAccounts(customerId) {
  return request(`/accounts?customerId=${encodeURIComponent(customerId)}`);
}

export function fetchBalance(accountId) {
  return request(`/accounts/${accountId}/balance`);
}

export function openAccount({ customerId, accountType, currency = 'INR' }) {
  return request('/accounts', {
    method: 'POST',
    body: { customerId, accountType, currency }
  });
}

export function fetchHistory(accountId) {
  return request(`/transactions/history?accountId=${encodeURIComponent(accountId)}`);
}

export function deposit({ destinationAccountId, amount, narrative }) {
  return request('/transactions/deposit', {
    method: 'POST',
    body: { destinationAccountId, amount, narrative }
  });
}

export function withdraw({ sourceAccountId, amount, narrative }) {
  return request('/transactions/withdraw', {
    method: 'POST',
    body: { sourceAccountId, amount, narrative }
  });
}

/**
 * BR-TXN-001: the backend deduplicates on X-Idempotency-Key, so replaying a key returns the
 * original transaction rather than moving money twice.
 */
export function transfer({ sourceAccountId, destinationAccountId, amount, narrative }, idempotencyKey) {
  return request('/transactions/transfer', {
    method: 'POST',
    headers: idempotencyKey ? { 'X-Idempotency-Key': idempotencyKey } : {},
    body: { sourceAccountId, destinationAccountId, amount, narrative }
  });
}

export function newIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `key-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
