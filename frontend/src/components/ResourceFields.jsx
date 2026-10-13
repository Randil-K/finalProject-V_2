import React from 'react';
import { Button, IconButton, Input, Select } from '../design-system';

/** What coastal cleanups actually ask for, so an officer picks rather than types. */
export const EQUIPMENT_NAMES = [
  'Heavy-duty gloves',
  'Refuse sacks',
  'Litter pickers',
  'Rakes',
  'Shovels',
  'Buckets',
  'Wheelbarrows',
  'Net cutters',
  'Mesh collection bags',
  'High-visibility vests',
  'Face masks',
  'First aid kits',
  'Drinking water',
  'Tarpaulins',
  'Weighing scales',
  'Sharps containers',
  'Oil absorbent pads',
  'Dive knives',
  'Lift bags',
  'Boat or dinghy',
];

const OTHER = '__other__';

const EQUIPMENT_OPTIONS = [
  ...EQUIPMENT_NAMES.map((name) => ({ value: name, label: name })),
  { value: OTHER, label: 'Other — type a name' },
];

const emptyLine = () => ({ name: '', quantity: '', custom: false });

/** A blank plan, or the one a project already has. */
export function resourcePlanFrom(resources) {
  return {
    // Every cleanup needs someone to turn up, so one volunteer is the starting point.
    volunteersNeeded: resources?.volunteersNeeded ?? 1,
    diversNeeded: resources?.diversNeeded ?? '',
    equipment: resources?.equipment?.length
      ? resources.equipment.map((item) => ({
        name: item.name,
        quantity: String(item.quantity),
        custom: !EQUIPMENT_NAMES.includes(item.name),
      }))
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
  if (!(volunteersNeeded >= 1)) {
    return { error: 'Every cleanup needs at least one volunteer.' };
  }
  return { payload: { volunteersNeeded, diversNeeded, equipment } };
}

/** The volunteers, divers and equipment a cleanup needs. The parent owns the plan. */
export default function ResourceFields({ plan, onChange }) {
  const setLine = (index, changes) =>
    onChange({ ...plan, equipment: plan.equipment.map((line, i) => (i === index ? { ...line, ...changes } : line)) });

  function chooseName(index, value) {
    // "Other" clears the line so the officer can type their own name into the box it opens.
    setLine(index, value === OTHER ? { custom: true, name: '' } : { custom: false, name: value });
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 'var(--space-3)' }}>
        <Input
          label="Volunteers needed"
          required
          type="number"
          min="1"
          max="1000"
          iconLeft="users"
          hint="At least one — a cleanup needs people on the sand."
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
          <div key={index} style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
              <Select
                placeholder="Choose equipment"
                aria-label={`Equipment ${index + 1}`}
                options={EQUIPMENT_OPTIONS}
                value={line.custom ? OTHER : line.name}
                onChange={(e) => chooseName(index, e.target.value)}
              />
              {line.custom ? (
                <Input
                  placeholder="e.g. Chainsaw for driftwood"
                  aria-label={`Equipment ${index + 1} name`}
                  value={line.name}
                  onChange={(e) => setLine(index, { name: e.target.value })}
                />
              ) : null}
            </div>
            <Input
              type="number"
              min="1"
              max="1000"
              placeholder="Qty"
              aria-label={`Quantity ${index + 1}`}
              value={line.quantity}
              onChange={(e) => setLine(index, { quantity: e.target.value })}
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
