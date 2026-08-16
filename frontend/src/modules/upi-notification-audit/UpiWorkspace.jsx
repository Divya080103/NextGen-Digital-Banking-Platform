import React, { useCallback, useEffect, useState } from 'react';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import StatusBadge from '../../components/StatusBadge';
import DataTable from '../../components/DataTable';
import {
  changePin,
  createProfile,
  fetchProfiles,
  generateQr,
  newIdempotencyKey,
  pay,
  scanQr
} from './upiApi';
import { fetchAccounts } from '../transaction/transactionApi';
import './UpiWorkspace.css';

const formatMoney = (value) =>
  value == null
    ? '—'
    : `₹ ${Number(value).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })}`;

/**
 * UPI console: profile onboarding, PIN management, VPA-to-VPA payment and QR generate/scan.
 *
 * Every rejection is rendered with its errorCode, because the lockout and QR-integrity rules are
 * only observable through those codes (UPI_PIN_LOCKED, QR_NOT_RECOGNIZED, QR_AMOUNT_MISMATCH).
 */
export default function UpiWorkspace({ customerId }) {
  const [profiles, setProfiles] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [busy, setBusy] = useState(false);

  const [newVpa, setNewVpa] = useState('');
  const [newPin, setNewPin] = useState('1234');
  const [linkAccountId, setLinkAccountId] = useState('');

  const [payerVpa, setPayerVpa] = useState('');
  const [payeeVpa, setPayeeVpa] = useState('');
  const [payAmount, setPayAmount] = useState('250.00');
  const [payPin, setPayPin] = useState('1234');
  const [payQrPayload, setPayQrPayload] = useState('');

  const [pinVpa, setPinVpa] = useState('');
  const [currentPin, setCurrentPin] = useState('');
  const [nextPin, setNextPin] = useState('');

  const [qrVpa, setQrVpa] = useState('');
  const [qrMerchant, setQrMerchant] = useState('Ankit Store');
  const [qrAmount, setQrAmount] = useState('99.00');
  const [qrDynamic, setQrDynamic] = useState(true);
  const [lastQr, setLastQr] = useState(null);
  const [scanInput, setScanInput] = useState('');
  const [scanResult, setScanResult] = useState(null);

  const load = useCallback(async () => {
    if (!customerId) return;
    try {
      const [profileList, accountList] = await Promise.all([
        fetchProfiles(customerId),
        fetchAccounts(customerId)
      ]);
      setProfiles(profileList);
      setAccounts(accountList);

      // Keep the current pick only if it still belongs to this customer. Switching customers
      // otherwise leaves a stale VPA selected, which renders as an empty <select> and silently
      // disables the buttons.
      const keepVpa = (current) =>
        current && profileList.some((p) => p.vpa === current)
          ? current
          : profileList[0]?.vpa || '';

      setLinkAccountId((current) =>
        current && accountList.some((a) => a.accountId === current)
          ? current
          : accountList[0]?.accountId || ''
      );
      setPayerVpa(keepVpa);
      setPinVpa(keepVpa);
      setQrVpa(keepVpa);
    } catch (err) {
      setFeedback({ tone: 'danger', code: err.errorCode, message: err.message });
    }
  }, [customerId]);

  useEffect(() => {
    load();
  }, [load]);

  const run = async (label, action) => {
    setBusy(true);
    setFeedback(null);
    try {
      const result = await action();
      setFeedback({ tone: 'success', code: 'OK', message: `${label} succeeded` });
      await load();
      return result;
    } catch (err) {
      setFeedback({ tone: 'danger', code: err.errorCode, message: err.message });
      await load();
      return null;
    } finally {
      setBusy(false);
    }
  };

  const profileColumns = [
    { key: 'vpa', label: 'VPA', isMono: true },
    {
      key: 'status',
      label: 'Status',
      render: (value) => (
        <StatusBadge status={value === 'ACTIVE' ? 'success' : 'warning'} label={value} />
      )
    },
    { key: 'defaultAccountId', label: 'Linked account', isMono: true }
  ];

  return (
    <div className="upi-workspace">
      {feedback && (
        <div className={`upi-feedback upi-feedback--${feedback.tone}`}>
          <StatusBadge status={feedback.tone} label={feedback.code} />
          <span>{feedback.message}</span>
        </div>
      )}

      <div className="upi-grid">
        <div className="upi-panel">
          <h3 className="upi-panel__title">Create UPI profile</h3>
          <FormField
            label="VPA (must end in @nextgen)"
            id="upi-new-vpa"
            value={newVpa}
            onChange={(e) => setNewVpa(e.target.value)}
            placeholder="divya@nextgen"
          />
          <label className="upi-label" htmlFor="upi-link-account">
            Link to account
          </label>
          <select
            id="upi-link-account"
            className="upi-select"
            value={linkAccountId}
            onChange={(e) => setLinkAccountId(e.target.value)}
          >
            {accounts.map((a) => (
              <option key={a.accountId} value={a.accountId}>
                {a.accountNumber} · {a.accountType}
              </option>
            ))}
          </select>
          <FormField
            label="UPI PIN (4 or 6 digits)"
            id="upi-new-pin"
            value={newPin}
            onChange={(e) => setNewPin(e.target.value)}
          />
          <Button
            disabled={busy || !newVpa || !linkAccountId}
            onClick={() =>
              run('Profile creation', () =>
                createProfile({
                  customerId,
                  vpa: newVpa,
                  defaultAccountId: linkAccountId,
                  upiPin: newPin
                })
              )
            }
          >
            Create profile
          </Button>
          <p className="upi-hint">
            BR-UPI-001 — anything other than an <code>@nextgen</code> handle is rejected.
          </p>
        </div>

        <div className="upi-panel">
          <h3 className="upi-panel__title">Send money</h3>
          <label className="upi-label" htmlFor="upi-payer">
            Payer VPA
          </label>
          <select
            id="upi-payer"
            className="upi-select"
            value={payerVpa}
            onChange={(e) => setPayerVpa(e.target.value)}
          >
            <option value="">Select a profile</option>
            {profiles.map((p) => (
              <option key={p.upiId} value={p.vpa}>
                {p.vpa}
              </option>
            ))}
          </select>
          <FormField
            label="Payee VPA"
            id="upi-payee"
            value={payeeVpa}
            onChange={(e) => setPayeeVpa(e.target.value)}
            placeholder="ankit@nextgen"
          />
          <FormField
            label="Amount"
            id="upi-amount"
            value={payAmount}
            onChange={(e) => setPayAmount(e.target.value)}
          />
          <FormField
            label="UPI PIN"
            id="upi-pin"
            type="password"
            value={payPin}
            onChange={(e) => setPayPin(e.target.value)}
          />
          <FormField
            label="QR payload (optional)"
            id="upi-qr-payload"
            value={payQrPayload}
            onChange={(e) => setPayQrPayload(e.target.value)}
            placeholder="Attach a scanned QR to bind the payment to it"
          />
          <Button
            disabled={busy || !payerVpa || !payeeVpa}
            onClick={() =>
              run('Payment', () =>
                pay(
                  {
                    payerVpa,
                    payeeVpa,
                    amount: Number(payAmount),
                    upiPin: payPin,
                    qrPayload: payQrPayload
                  },
                  newIdempotencyKey()
                )
              )
            }
          >
            Pay
          </Button>
          <p className="upi-hint">
            BR-UPI-002 — three wrong PINs lock the profile for 24 hours. BR-UPI-003 — ₹1,00,000 and
            10 payments per day.
          </p>
        </div>

        <div className="upi-panel">
          <h3 className="upi-panel__title">Change PIN</h3>
          <label className="upi-label" htmlFor="upi-pin-vpa">
            VPA
          </label>
          <select
            id="upi-pin-vpa"
            className="upi-select"
            value={pinVpa}
            onChange={(e) => setPinVpa(e.target.value)}
          >
            <option value="">Select a profile</option>
            {profiles.map((p) => (
              <option key={p.upiId} value={p.vpa}>
                {p.vpa}
              </option>
            ))}
          </select>
          <FormField
            label="Current PIN"
            id="upi-current-pin"
            type="password"
            value={currentPin}
            onChange={(e) => setCurrentPin(e.target.value)}
          />
          <FormField
            label="New PIN"
            id="upi-next-pin"
            type="password"
            value={nextPin}
            onChange={(e) => setNextPin(e.target.value)}
          />
          <Button
            variant="secondary"
            disabled={busy || !pinVpa}
            onClick={() =>
              run('PIN change', () =>
                changePin({ vpa: pinVpa, currentUpiPin: currentPin, newUpiPin: nextPin })
              )
            }
          >
            Change PIN
          </Button>
          <p className="upi-hint">
            Wrong attempts here feed the same lockout counter, so this is not a brute-force bypass.
          </p>
        </div>

        <div className="upi-panel">
          <h3 className="upi-panel__title">QR code</h3>
          <label className="upi-label" htmlFor="upi-qr-vpa">
            Collect into VPA
          </label>
          <select
            id="upi-qr-vpa"
            className="upi-select"
            value={qrVpa}
            onChange={(e) => setQrVpa(e.target.value)}
          >
            <option value="">Select a profile</option>
            {profiles.map((p) => (
              <option key={p.upiId} value={p.vpa}>
                {p.vpa}
              </option>
            ))}
          </select>
          <FormField
            label="Merchant name"
            id="upi-merchant"
            value={qrMerchant}
            onChange={(e) => setQrMerchant(e.target.value)}
          />
          <FormField
            label="Fixed amount (blank for open amount)"
            id="upi-qr-amount"
            value={qrAmount}
            onChange={(e) => setQrAmount(e.target.value)}
          />
          <label className="upi-checkbox">
            <input
              type="checkbox"
              checked={qrDynamic}
              onChange={(e) => setQrDynamic(e.target.checked)}
            />
            Dynamic (expires in 15 minutes)
          </label>
          <Button
            disabled={busy || !qrVpa}
            onClick={async () => {
              const result = await run('QR generation', () =>
                generateQr({
                  vpa: qrVpa,
                  merchantName: qrMerchant,
                  fixedAmount: qrAmount,
                  dynamic: qrDynamic
                })
              );
              if (result) {
                setLastQr(result);
                setScanInput(result.qrPayloadString);
              }
            }}
          >
            Generate QR
          </Button>

          {lastQr && (
            <div className="upi-qr-result">
              <code>{lastQr.qrPayloadString}</code>
              <span>
                {lastQr.expiresAt ? `Expires ${new Date(lastQr.expiresAt).toLocaleTimeString()}` : 'Static QR'}
              </span>
              <Button variant="secondary" onClick={() => setPayQrPayload(lastQr.qrPayloadString)}>
                Use in payment
              </Button>
            </div>
          )}

          <FormField
            label="Scan payload"
            id="upi-scan"
            value={scanInput}
            onChange={(e) => setScanInput(e.target.value)}
            placeholder="upi://pay?pa=…"
          />
          <Button
            variant="secondary"
            disabled={busy || !scanInput}
            onClick={async () => {
              setScanResult(null);
              const result = await run('Scan', () => scanQr(scanInput));
              if (result) setScanResult(result);
            }}
          >
            Scan
          </Button>
          {scanResult && (
            <div className="upi-scan-result">
              <strong>{scanResult.merchantName}</strong>
              <span>{scanResult.payeeVpa}</span>
              <span>{formatMoney(scanResult.amount)}</span>
            </div>
          )}
          <p className="upi-hint">
            BR-UPI-004 — edit the payload by hand and it is rejected as QR_NOT_RECOGNIZED, because
            only QR codes this platform issued are trusted.
          </p>
        </div>
      </div>

      <div className="upi-panel">
        <h3 className="upi-panel__title">Profiles for this customer</h3>
        <DataTable
          columns={profileColumns}
          data={profiles.map((p) => ({ ...p, id: p.upiId }))}
        />
      </div>
    </div>
  );
}
