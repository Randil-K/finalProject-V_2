import React from 'react';
import { Field } from './Field.jsx';
import { Icon } from '../core/Icon.jsx';

function controlStyle({ focus, invalid, disabled, size }) {
  const heights = { sm: 34, md: 42, lg: 48 };
  return {
    width: '100%',
    height: heights[size] || heights.md,
    padding: '0 12px',
    font: 'var(--text-body)',
    color: 'var(--text-strong)',
    background: disabled ? 'var(--surface-sunken)' : 'var(--surface-card)',
    border: '1px solid ' + (invalid ? 'var(--danger)' : focus ? 'var(--border-accent)' : 'var(--border-default)'),
    borderRadius: 'var(--radius-md)',
    boxShadow: focus ? 'var(--shadow-focus)' : 'none',
    outline: 'none',
    transition: 'var(--transition-control)',
    cursor: disabled ? 'not-allowed' : 'text',
  };
}

export function Input({ label, hint, error, required, size = 'md', iconLeft, suffix, trailing, disabled, id, style, ...rest }) {
  const [focus, setFocus] = React.useState(false);
  const generatedId = React.useId();
  const inputId = id || generatedId;
  return (
    <Field label={label} hint={hint} error={error} required={required} htmlFor={inputId} style={style}>
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        {iconLeft ? (
          <span style={{ position: 'absolute', left: 12, display: 'flex', pointerEvents: 'none' }}>
            <Icon name={iconLeft} size="sm" color="var(--text-muted)" />
          </span>
        ) : null}
        <input
          id={inputId}
          disabled={disabled}
          onFocus={() => setFocus(true)}
          onBlur={() => setFocus(false)}
          style={{
            ...controlStyle({ focus, invalid: Boolean(error), disabled, size }),
            paddingLeft: iconLeft ? 36 : 12,
            paddingRight: trailing ? 44 : suffix ? 56 : 12,
          }}
          {...rest}
        />
        {suffix ? (
          <span style={{ position: 'absolute', right: 12, font: 'var(--text-caption)', color: 'var(--text-muted)' }}>{suffix}</span>
        ) : null}
        {trailing ? (
          <span style={{ position: 'absolute', right: 6, display: 'flex' }}>{trailing}</span>
        ) : null}
      </div>
    </Field>
  );
}
