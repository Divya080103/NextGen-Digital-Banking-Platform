import React, { useEffect, useState } from 'react';
import StatusBadge from './components/StatusBadge';
import FormField from './components/FormField';
import { ComponentShowcase } from './components/ComponentShowcase.jsx';
import { TransactionWorkspace } from './modules/transaction';
import { UpiWorkspace } from './modules/upi-notification-audit';
import './App.css';

/**
 * `modules/account-card` is deliberately not imported here yet: AccountDashboard.jsx does
 * `import LedgerCard from '../../components/LedgerCard'`, but LedgerCard.jsx only has named
 * exports. esbuild fails the whole dependency scan on that, which would take the Transaction and
 * UPI screens down with it. The fix belongs to that module's owner.
 */
const TABS = [
  { id: 'transactions', label: 'Transactions' },
  { id: 'upi', label: 'UPI' },
  { id: 'design', label: 'Design system' }
];

/**
 * Customers seeded directly into auth_users/cust_profiles for local work. The auth and customer
 * modules are not built yet, so there is no login to derive an identity from — the acting customer
 * is chosen here instead and passed down to every module.
 */
const SEEDED_CUSTOMERS = [
  { id: 'aaaaaaaa-0000-0000-0000-000000000001', label: 'Divya Payer' },
  { id: 'bbbbbbbb-0000-0000-0000-000000000002', label: 'Ankit Payee' }
];

export function App() {
  const [tab, setTab] = useState('transactions');
  const [customerId, setCustomerId] = useState(SEEDED_CUSTOMERS[0].id);
  const [apiUp, setApiUp] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const ping = async () => {
      try {
        const res = await fetch('/actuator/health');
        if (!cancelled) setApiUp(res.ok);
      } catch {
        if (!cancelled) setApiUp(false);
      }
    };
    ping();
    const timer = setInterval(ping, 15000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__brand">
          <h1 className="app-header__title">NextGen Banking</h1>
          <span className="app-header__subtitle">Transaction &amp; UPI console</span>
        </div>
        <StatusBadge
          status={apiUp === false ? 'danger' : apiUp ? 'success' : 'warning'}
          label={apiUp === false ? 'API DOWN' : apiUp ? 'API UP' : 'CHECKING'}
        />
      </header>

      {apiUp === false && (
        <div className="app-banner">
          The backend on <code>localhost:8080</code> is not answering. Start it with{' '}
          <code>mvn spring-boot:run</code> in <code>backend/</code>; screens will stay empty until
          it is up.
        </div>
      )}

      <nav className="app-nav">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            className={`app-nav__tab${tab === t.id ? ' app-nav__tab--active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </nav>

      {tab !== 'design' && (
        <section className="app-context">
          <span className="app-context__label">Acting as</span>
          {SEEDED_CUSTOMERS.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`app-context__chip${
                customerId === c.id ? ' app-context__chip--active' : ''
              }`}
              onClick={() => setCustomerId(c.id)}
            >
              {c.label}
            </button>
          ))}
          <div className="app-context__field">
            <FormField
              label="Customer ID"
              id="app-customer-id"
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
            />
          </div>
        </section>
      )}

      <main className="app-main">
        {tab === 'transactions' && <TransactionWorkspace customerId={customerId} />}
        {tab === 'upi' && <UpiWorkspace customerId={customerId} />}
        {tab === 'design' && <ComponentShowcase />}
      </main>
    </div>
  );
}

export default App;
