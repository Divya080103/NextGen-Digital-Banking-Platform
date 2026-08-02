import React, { useState } from 'react';
import { HorizonCard } from './HorizonCard.jsx';
import { StatusBadge } from './StatusBadge.jsx';
import { Button } from './Button.jsx';
import { DataTable } from './DataTable.jsx';
import { FormField } from './FormField.jsx';
import { AskAIBar } from './AskAIBar.jsx';
import '../styles/design-system.css';

export const ComponentShowcase = () => {
  const [formVal, setFormVal] = useState('9876543210');

  const tableColumns = [
    { label: 'Reference ID', key: 'ref', isMono: true },
    { label: 'Transaction Type', key: 'type' },
    { label: 'Status', key: 'status', render: (val) => <StatusBadge status={val} label={val.toUpperCase()} /> },
    { label: 'Amount (INR)', key: 'amount', alignRight: true, isAmount: true }
  ];

  const tableData = [
    { id: '1', ref: 'TXN-908124', type: 'NEFT Transfer', status: 'success', amount: '+ ₹ 45,000.00' },
    { id: '2', ref: 'TXN-908125', type: 'UPI Payment', status: 'warning', amount: '- ₹ 1,250.00' },
    { id: '3', ref: 'TXN-908126', type: 'ATM Withdrawal', status: 'danger', amount: '- ₹ 5,000.00' }
  ];

  return (
    <div style={{ padding: '32px', maxWidth: '1000px', margin: '0 auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2.25rem', marginBottom: '8px' }}>
        NextGen Banking — Component Showcase
      </h1>
      <p style={{ color: 'var(--color-text-secondary)', marginBottom: '32px' }}>
        Refreshed dark-nav / warm-canvas design system showcase.
      </p>

      {/* Signature Element 1: HorizonCard */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px', color: 'var(--color-pine-950)' }}>
          Signature Element: HorizonCard (Hero & Compact)
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px' }}>
          <HorizonCard
            accountType="Savings Account"
            accountNumber="•••• 3456"
            amount="₹ 2,45,678.90"
            subtitle="Across 4 active accounts"
            variant="hero"
            actions={
              <>
                <Button variant="primary">Transfer Money</Button>
                <Button variant="secondary" style={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}>View Statements</Button>
              </>
            }
          />
          <HorizonCard
            accountType="Credit Card"
            accountNumber="•••• 4321"
            amount="₹ 45,320.00"
            subtitle="Due in 12 days"
            variant="compact"
          />
        </div>
      </section>

      {/* Signature Element 2: AskAIBar */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px', color: 'var(--color-pine-950)' }}>
          Signature Element: Ask NextGen AI Bar (Glass Surface)
        </h2>
        <AskAIBar activeModule="loan" />
      </section>

      {/* Status Badges */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Status Badges (Semantic States)</h2>
        <div style={{ display: 'flex', gap: '12px' }}>
          <StatusBadge status="success" label="Active / Approved" />
          <StatusBadge status="warning" label="Pending / Review" />
          <StatusBadge status="danger" label="Blocked / Failed" />
          <StatusBadge status="info" label="Initiated / Info" />
        </div>
      </section>

      {/* Buttons */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Button Variants</h2>
        <div style={{ display: 'flex', gap: '12px' }}>
          <Button variant="primary">Primary Action</Button>
          <Button variant="secondary">Secondary Action</Button>
          <Button variant="destructive">Destructive Action</Button>
          <Button variant="primary" disabled>Disabled Action</Button>
        </div>
      </section>

      {/* Form Fields */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Form Fields</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <FormField
            label="Mobile Number"
            id="mobile"
            value={formVal}
            onChange={(e) => setFormVal(e.target.value)}
            required
          />
          <FormField
            label="PAN Number"
            id="pan"
            value="ABCDE1234"
            error="Enter a valid 10-character PAN number"
          />
        </div>
      </section>

      {/* Data Table */}
      <section style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Data Table (Tabular Numerals)</h2>
        <DataTable columns={tableColumns} data={tableData} />
      </section>
    </div>
  );
};

export default ComponentShowcase;
