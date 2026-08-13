import React, { useCallback, useEffect, useState } from 'react';
import HorizonCard from '../../components/HorizonCard';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import StatusBadge from '../../components/StatusBadge';
import DataTable from '../../components/DataTable';
import {
  deposit,
  fetchAccounts,
  fetchHistory,
  newIdempotencyKey,
  transfer,
  withdraw
} from './transactionApi';
import './TransactionWorkspace.css';

const formatMoney = (value) =>
  `₹ ${Number(value ?? 0).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;

const statusTone = (status) => {
  if (status === 'COMPLETED') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'REVERSED') return 'warning';
  return 'info';
};

/**
 * Operator console for the Transaction Engine: deposit, withdraw, transfer and history.
 *
 * The transfer form deliberately exposes the idempotency key. Reusing a key is how BR-TXN-001
 * is demonstrated — the backend returns the original transaction instead of debiting twice.
 */
export default function TransactionWorkspace({ customerId }) {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [history, setHistory] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [busy, setBusy] = useState(false);

  const [depositAmount, setDepositAmount] = useState('5000.00');
  const [withdrawAmount, setWithdrawAmount] = useState('1000.00');
  const [transferAmount, setTransferAmount] = useState('2500.00');
  const [transferTarget, setTransferTarget] = useState('');
  const [narrative, setNarrative] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey());

  const selectedAccount = accounts.find((a) => a.accountId === selectedAccountId) || null;

  const loadAccounts = useCallback(async () => {
    if (!customerId) return;
    try {
      const list = await fetchAccounts(customerId);
      setAccounts(list);
      setSelectedAccountId((current) => {
        if (current && list.some((a) => a.accountId === current)) return current;
        return list[0]?.accountId || '';
      });
    } catch (err) {
      setFeedback({ tone: 'danger', code: err.errorCode, message: err.message });
    }
  }, [customerId]);

  const loadHistory = useCallback(async (accountId) => {
    if (!accountId) {
      setHistory([]);
      return;
    }
    try {
      setHistory(await fetchHistory(accountId));
    } catch (err) {
      setFeedback({ tone: 'danger', code: err.errorCode, message: err.message });
    }
  }, []);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    loadHistory(selectedAccountId);
  }, [selectedAccountId, loadHistory]);

  const run = async (label, action) => {
    setBusy(true);
    setFeedback(null);
    try {
      const result = await action();
      setFeedback({
        tone: 'success',
        code: result?.status || 'OK',
        message: `${label} succeeded — reference ${result?.referenceNumber || 'n/a'}`
      });
      await loadAccounts();
      await loadHistory(selectedAccountId);
      return result;
    } catch (err) {
      // Business-rule rejections land here; the errorCode is the point of interest.
      setFeedback({ tone: 'danger', code: err.errorCode, message: err.message });
      await loadHistory(selectedAccountId);
      return null;
    } finally {
      setBusy(false);
    }
  };

  const columns = [
    { key: 'referenceNumber', label: 'Reference', isMono: true },
    { key: 'transactionType', label: 'Type' },
    {
      key: 'status',
      label: 'Status',
      render: (value) => <StatusBadge status={statusTone(value)} label={value} />
    },
    {
      key: 'amount',
      label: 'Amount',
      alignRight: true,
      isAmount: true,
      render: (value) => formatMoney(value)
    },
    {
      key: 'failureReason',
      label: 'Detail',
      render: (value, row) => value || row.narrative || '—'
    }
  ];

  if (!accounts.length) {
    return (
      <div className="txn-workspace">
        <div className="txn-empty">
          <h3>No accounts for this customer</h3>
          <p>
            Open an account from the Accounts tab first — transactions are FK-bound to a real
            account row.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="txn-workspace">
      <div className="txn-workspace__top">
        <HorizonCard
          accountType={`${selectedAccount?.accountType || 'ACCOUNT'} · ${selectedAccount?.status || ''}`}
          accountNumber={selectedAccount?.accountNumber || '—'}
          amount={formatMoney(selectedAccount?.balance)}
          subtitle={`Available ${formatMoney(selectedAccount?.availableBalance)}`}
        />

        <div className="txn-panel">
          <h3 className="txn-panel__title">Working account</h3>
          <select
            className="txn-select"
            value={selectedAccountId}
            onChange={(e) => setSelectedAccountId(e.target.value)}
          >
            {accounts.map((a) => (
              <option key={a.accountId} value={a.accountId}>
                {a.accountNumber} · {a.accountType} · {formatMoney(a.balance)}
              </option>
            ))}
          </select>

          <div className="txn-rules">
            <span>Single transfer cap ₹50,000</span>
            <span>Daily ceiling ₹1,00,000</span>
            <span>Duplicate shield 120s</span>
          </div>
        </div>
      </div>

      {feedback && (
        <div className={`txn-feedback txn-feedback--${feedback.tone}`}>
          <StatusBadge status={feedback.tone} label={feedback.code} />
          <span>{feedback.message}</span>
        </div>
      )}

      <div className="txn-forms">
        <div className="txn-panel">
          <h3 className="txn-panel__title">Deposit</h3>
          <FormField
            label="Amount"
            id="dep-amount"
            value={depositAmount}
            onChange={(e) => setDepositAmount(e.target.value)}
          />
          <Button
            disabled={busy}
            onClick={() =>
              run('Deposit', () =>
                deposit({
                  destinationAccountId: selectedAccountId,
                  amount: Number(depositAmount),
                  narrative: narrative || 'Counter deposit'
                })
              )
            }
          >
            Deposit
          </Button>
        </div>

        <div className="txn-panel">
          <h3 className="txn-panel__title">Withdraw</h3>
          <FormField
            label="Amount"
            id="wd-amount"
            value={withdrawAmount}
            onChange={(e) => setWithdrawAmount(e.target.value)}
          />
          <Button
            variant="secondary"
            disabled={busy}
            onClick={() =>
              run('Withdrawal', () =>
                withdraw({
                  sourceAccountId: selectedAccountId,
                  amount: Number(withdrawAmount),
                  narrative: narrative || 'ATM withdrawal'
                })
              )
            }
          >
            Withdraw
          </Button>
        </div>

        <div className="txn-panel txn-panel--wide">
          <h3 className="txn-panel__title">Transfer</h3>
          <div className="txn-panel__row">
            <div className="txn-panel__col">
              <label className="txn-label" htmlFor="txn-target">
                Destination account
              </label>
              <select
                id="txn-target"
                className="txn-select"
                value={transferTarget}
                onChange={(e) => setTransferTarget(e.target.value)}
              >
                <option value="">Select or paste an account ID below</option>
                {accounts
                  .filter((a) => a.accountId !== selectedAccountId)
                  .map((a) => (
                    <option key={a.accountId} value={a.accountId}>
                      {a.accountNumber} · {a.accountType}
                    </option>
                  ))}
              </select>
              <FormField
                label="…or destination account ID"
                id="txn-target-manual"
                value={transferTarget}
                onChange={(e) => setTransferTarget(e.target.value)}
                placeholder="Paste another customer's account UUID"
              />
            </div>

            <div className="txn-panel__col">
              <FormField
                label="Amount"
                id="txn-amount"
                value={transferAmount}
                onChange={(e) => setTransferAmount(e.target.value)}
              />
              <FormField
                label="Narrative"
                id="txn-narrative"
                value={narrative}
                onChange={(e) => setNarrative(e.target.value)}
                placeholder="Rent, bill split, …"
              />
            </div>
          </div>

          <div className="txn-idem">
            <FormField
              label="X-Idempotency-Key (reuse it to prove BR-TXN-001)"
              id="txn-key"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
            />
            <Button variant="secondary" onClick={() => setIdempotencyKey(newIdempotencyKey())}>
              New key
            </Button>
          </div>

          <Button
            disabled={busy || !transferTarget}
            onClick={() =>
              run('Transfer', () =>
                transfer(
                  {
                    sourceAccountId: selectedAccountId,
                    destinationAccountId: transferTarget,
                    amount: Number(transferAmount),
                    narrative: narrative || 'Internal transfer'
                  },
                  idempotencyKey
                )
              )
            }
          >
            Send transfer
          </Button>
          <p className="txn-hint">
            Send twice without pressing “New key” — the second call returns the same transaction ID
            and the balance moves only once.
          </p>
        </div>
      </div>

      <div className="txn-panel">
        <div className="txn-panel__header">
          <h3 className="txn-panel__title">
            History · {selectedAccount?.accountNumber || ''}
          </h3>
          <Button variant="secondary" onClick={() => loadHistory(selectedAccountId)}>
            Refresh
          </Button>
        </div>
        <DataTable columns={columns} data={history.map((t) => ({ ...t, id: t.transactionId }))} />
      </div>
    </div>
  );
}
