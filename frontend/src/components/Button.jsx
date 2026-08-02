import React from 'react';
import './Button.css';

/**
 * Button Component
 * Supports 'primary', 'secondary', and 'destructive' variants.
 */
export const Button = ({
  children,
  variant = 'primary',
  disabled = false,
  onClick,
  type = 'button',
  className = ''
}) => {
  const normalizedVariant = ['primary', 'secondary', 'destructive'].includes(variant) ? variant : 'primary';

  return (
    <button
      type={type}
      className={`bank-btn bank-btn--${normalizedVariant} ${className}`}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
};

export default Button;
