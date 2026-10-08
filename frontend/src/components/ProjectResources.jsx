import React from 'react';
import { Badge, Icon } from '../design-system';

function Need({ icon, label, needed, joined }) {
  const met = joined >= needed;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: 'var(--space-3) var(--space-4)', borderRadius: 'var(--radius-md)', background: 'var(--surface-sunken)' }}>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
        <Icon name={icon} size="xs" />
        {label}
      </span>
      <span style={{ font: '700 22px/1.1 var(--font-display)', color: 'var(--text-strong)' }}>
        {joined} <span style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>of {needed} joined</span>
      </span>
      {needed > 0 ? <Badge tone={met ? 'success' : 'warning'} size="sm" style={{ alignSelf: 'flex-start' }}>{met ? 'Filled' : `${needed - joined} more needed`}</Badge> : null}
    </div>
  );
}

/** What the government officer asked for, and how much of it people have pledged. */
export default function ProjectResources({ project }) {
  const resources = project.resources;
  if (!resources) return null;
  const volunteers = resources.volunteersNeeded ?? 0;
  const divers = resources.diversNeeded ?? 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Required resources</span>
        {!resources.finalized ? <Badge tone="warning" size="sm">Draft</Badge>
          : project.resourcesGatheredAt ? <Badge tone="success" size="sm">All gathered</Badge>
            : project.recruitmentRadiusKm ? <Badge tone="accent" size="sm">Asking within {project.recruitmentRadiusKm} km</Badge>
              : null}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 'var(--space-3)' }}>
        {volunteers > 0 ? <Need icon="users" label="Volunteers" needed={volunteers} joined={project.volunteerCount} /> : null}
        {divers > 0 ? <Need icon="anchor" label="Divers" needed={divers} joined={project.diverCount} /> : null}
      </div>

      {resources.equipment?.length ? (
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginBottom: 4 }}>Equipment</span>
          {resources.equipment.map((item, index) => (
            <div
              key={`${item.name}-${index}`}
              style={{ display: 'flex', justifyContent: 'space-between', gap: 8, padding: 'var(--space-2) 0', borderTop: '1px solid var(--border-subtle)', font: 'var(--text-body-sm)' }}
            >
              <span style={{ color: 'var(--text-heading)' }}>{item.name}</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Badge tone={item.securedQuantity >= item.quantity ? 'success' : 'warning'} size="sm">
                  {item.securedQuantity >= item.quantity
                    ? 'Secured'
                    : `${item.securedQuantity} of ${item.quantity} pledged`}
                </Badge>
                <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-strong)' }}>× {item.quantity}</span>
              </span>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
