import React from 'react';
import './StatusBadge.css';

/**
 * StatusBadge Component
 * Accepts the four semantic states: 'success', 'warning', 'danger', 'info'.
 */
export const StatusBadge = ({ status = 'info', label }) => {
  const normalizedStatus = ['success', 'warning', 'danger', 'info'].includes(status) ? status : 'info';

  return (
    <span className={`status-badge status-badge--${normalizedStatus}`}>
      {label || normalizedStatus.toUpperCase()}
    </span>
  );
};

export default StatusBadge;
