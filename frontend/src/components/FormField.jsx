import React from 'react';
import './FormField.css';

/**
 * FormField Component
 * Label + input + error state styling.
 */
export const FormField = ({
  label,
  id,
  type = 'text',
  value,
  onChange,
  placeholder,
  error,
  required = false
}) => {
  return (
    <div className={`form-field ${error ? 'form-field--error' : ''}`}>
      {label && (
        <label htmlFor={id} className="form-field__label">
          {label} {required && <span style={{ color: 'var(--color-danger-600)' }}>*</span>}
        </label>
      )}
      <input
        id={id}
        type={type}
        className="form-field__input"
        value={value}
        onChange={onChange}
        placeholder={placeholder}
      />
      {error && <span className="form-field__error-message">{error}</span>}
    </div>
  );
};

export default FormField;
