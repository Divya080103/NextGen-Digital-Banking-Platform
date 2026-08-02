import React from 'react';
import './DataTable.css';

/**
 * DataTable Component
 * Features right-aligned tabular-numeral amount columns, zebra striping, and clean typography.
 */
export const DataTable = ({ columns = [], data = [] }) => {
  return (
    <div className="data-table-container">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col, index) => (
              <th
                key={col.key || index}
                className={col.alignRight ? 'align-right' : ''}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} style={{ textAlign: 'center', padding: '24px' }}>
                No records found.
              </td>
            </tr>
          ) : (
            data.map((row, rowIndex) => (
              <tr key={row.id || rowIndex}>
                {columns.map((col, colIndex) => {
                  const val = row[col.key];
                  const isRight = col.alignRight;
                  const isAmount = col.isAmount;
                  const isMono = col.isMono;

                  return (
                    <td
                      key={col.key || colIndex}
                      className={`${isRight ? 'align-right' : ''} ${isAmount ? 'is-amount' : ''} ${isMono ? 'is-mono' : ''}`}
                    >
                      {col.render ? col.render(val, row) : val}
                    </td>
                  );
                })}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default DataTable;
