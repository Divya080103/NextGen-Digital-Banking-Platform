import React from 'react';
import './LedgerCard.css';

/**
 * LedgerCard Component — Signature UI Element
 * Represents balances, transaction summaries, EMI amounts, or financial metrics.
 * 
 * Features:
 * - 3px solid brass top-rule (`--color-brass-500`)
 * - Paper-100 surface (`--color-paper-100`) with subtle directional shadow
 * - IBM Plex Mono tabular numerals for precision alignment
 * - Uppercase caption label with 0.04em letter-spacing
 */
export function LedgerCard({
  label,
  amount,
  currency = '₹',
  subtitle,
  badge,
  action,
  align = 'left',
  className = '',
  ariaLabel,
  children
}) {
  const formattedAmount = typeof amount === 'number' 
    ? amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : amount;

  const accessibleText = ariaLabel || `${label}: ${currency}${formattedAmount}`;

  return (
    <article 
      className={`ledger-card ledger-card--align-${align} ${className}`} 
      aria-label={accessibleText}
    >
      <div className="ledger-card__brass-rule" aria-hidden="true" />
      
      <div className="ledger-card__header">
        {label && <span className="ledger-card__label">{label}</span>}
        {badge && <div className="ledger-card__badge">{badge}</div>}
      </div>

      {amount !== undefined && amount !== null && (
        <div className="ledger-card__amount-row">
          <span className="ledger-card__currency">{currency}</span>
          <span className="ledger-card__amount">{formattedAmount}</span>
        </div>
      )}

      {subtitle && <p className="ledger-card__subtitle">{subtitle}</p>}
      {children}
      {action && <div className="ledger-card__action">{action}</div>}
    </article>
  );
}

/**
 * Storybook-Style Example Usage Component
 * Pure presentational demonstration showcasing all LedgerCard variants.
 */
export function LedgerCardExample() {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', padding: '16px' }}>
      <LedgerCard 
        label="Total Available Balance"
        amount={248550.75}
        subtitle="Savings Account •••• 4321"
      />

      <LedgerCard 
        label="Monthly EMI Dues"
        amount={18450.00}
        subtitle="Home Loan • Due 15th Aug 2026"
        badge={<span style={{ background: '#F3E7CD', color: '#9C6B14', padding: '2px 8px', borderRadius: '999px', fontSize: '12px', fontWeight: 600 }}>PENDING</span>}
      />

      <LedgerCard 
        label="Total Disbursed Principal"
        amount={1500000.00}
        subtitle="Sanctioned Rate: 8.5% p.a."
        align="right"
      />
    </div>
  );
}
