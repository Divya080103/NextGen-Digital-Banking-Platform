import React from 'react';
import './HorizonCard.css';

/**
 * HorizonCard Signature Component
 * Hero account balance card with dark pine background, General Sans bold numerals,
 * and abstract vector landscape horizon graphic.
 */
export const HorizonCard = ({
  accountType = 'Savings Account',
  accountNumber = '•••• 3456',
  amount = '₹ 2,45,678.90',
  subtitle = 'Across 4 active accounts',
  variant = 'hero', // 'hero' | 'compact'
  actions = null
}) => {
  const isCompact = variant === 'compact';

  return (
    <div className={`horizon-card ${isCompact ? 'horizon-card--compact' : ''}`}>
      {!isCompact && (
        <svg
          className="horizon-card__illustration"
          viewBox="0 0 300 200"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Gold Sun */}
          <circle cx="220" cy="70" r="28" fill="#C9974B" fillOpacity="0.85" />
          <circle cx="220" cy="70" r="38" stroke="#C9974B" strokeWidth="1.5" strokeOpacity="0.4" strokeDasharray="4 4" />
          
          {/* Abstract Horizon Waves */}
          <path
            d="M 100 200 C 150 140, 220 160, 300 110 L 300 200 Z"
            fill="#1B3A2C"
            fillOpacity="0.9"
          />
          <path
            d="M 140 200 C 190 120, 250 130, 300 90 L 300 200 Z"
            fill="#0F241C"
            fillOpacity="0.95"
          />
          <path
            d="M 80 200 C 160 170, 210 120, 300 140"
            stroke="#C9974B"
            strokeWidth="1.5"
            strokeOpacity="0.6"
          />
        </svg>
      )}

      <div className="horizon-card__header">
        <span className="horizon-card__label">{accountType}</span>
        <span className="horizon-card__account-number">{accountNumber}</span>
      </div>

      <div className="horizon-card__amount-container">
        <div className="horizon-card__amount">{amount}</div>
        {subtitle && <div className="horizon-card__subtitle">{subtitle}</div>}
      </div>

      {actions && <div className="horizon-card__actions">{actions}</div>}
    </div>
  );
};

export default HorizonCard;
