import React from 'react';
import { Button, IconButton, Input } from '../design-system';

const emptyLine = () => ({ name: '', quantity: '' });

/** A blank plan, or the one a project already has. */
export function resourcePlanFrom(resources) {
  return {
    volunteersNeeded: resources?.volunteersNeeded ?? '',
    diversNeeded: resources?.diversNeeded ?? '',
    equipment: resources?.equipment?.length
      ? resources.equipment.map((item) => ({ name: item.name, quantity: String(item.quantity) }))
      : [emptyLine()],
  };
}

/** Turns the typed plan into what the API expects, or an error to show instead. */
export function buildResources(plan) {
  const equipment = plan.equipment
    .filter((line) => line.name.trim() || line.quantity)
    .map((line) => ({ name: line.name.trim(), quantity: Number(line.quantity) }));
  if (equipment.some((line) => !line.name || !(line.quantity >= 1))) {
    return { error: 'Give every equipment item a name and a quantity of at least 1.' };
  }
  const volunteersNeeded = plan.volunteersNeeded === '' ? 0 : Number(plan.volunteersNeeded);
  const diversNeeded = plan.diversNeeded === '' ? 0 : Number(plan.diversNeeded);
  if (!volunteersNeeded && !diversNeeded && !equipment.length) {
    return { error: 'Set the volunteers, divers or equipment this cleanup needs.' };
  }
  return { payload: { volunteersNeeded, diversNeeded, equipment } };
}

/** The volunteers, divers and equipment a cleanup needs. The parent owns the plan. */
export default function ResourceFields({ plan, onChange }) {
  const setLine = (index, key, value) =>
    onChange({ ...plan, equipment: plan.equipment.map((line, i) => (i === index ? { ...line, [key]: value } : line)) });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 'var(--space-3)' }}>
        <Input
          label="Volunteers needed"
          type="number"
          min="0"
          max="1000"
          iconLeft="users"
          value={plan.volunteersNeeded}
          onChange={(e) => onChange({ ...plan, volunteersNeeded: e.target.value })}
        />
        <Input
          label="Divers needed"
          type="number"
          min="0"
          max="1000"
          iconLeft="anchor"
          value={plan.diversNeeded}
          onChange={(e) => onChange({ ...plan, diversNeeded: e.target.value })}
        />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Equipment</span>
        {plan.equipment.map((line, index) => (
          <div key={index} style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'flex-end' }}>
            <Input
              placeholder="e.g. Heavy-duty gloves"
              aria-label={`Equipment ${index + 1}`}
              value={line.name}
              onChange={(e) => setLine(index, 'name', e.target.value)}
              style={{ flex: 1 }}
            />
            <Input
              type="number"
              min="1"
              max="1000"
              placeholder="Qty"
              aria-label={`Quantity ${index + 1}`}
              value={line.quantity}
              onChange={(e) => setLine(index, 'quantity', e.target.value)}
              style={{ width: 100 }}
            />
            <IconButton
              icon="trash"
              label={`Remove equipment ${index + 1}`}
              onClick={() => onChange({
                ...plan,
                equipment: plan.equipment.length > 1 ? plan.equipment.filter((_, i) => i !== index) : [emptyLine()],
              })}
            />
          </div>
        ))}
        <Button
          variant="ghost"
          size="sm"
          iconLeft="plus"
          disabled={plan.equipment.length >= 30}
          onClick={() => onChange({ ...plan, equipment: [...plan.equipment, emptyLine()] })}
          style={{ alignSelf: 'flex-start' }}
        >
          Add equipment
        </Button>
      </div>
    </div>
  );
}
