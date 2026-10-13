import React from 'react';
import { formatDate } from '../lib/format.js';

/** The headline numbers for a cleanup. Progress itself is told by the story, not a percentage. */
export default function ProjectProgress({ project }) {
  const stats = [
    { value: project.volunteerCount, label: 'Volunteers' },
    { value: project.diverCount, label: 'Divers' },
    { value: project.debrisRemovedKg != null ? `${project.debrisRemovedKg} kg` : '—', label: 'Debris removed' },
    { value: project.startedAt ? formatDate(project.startedAt, false) : '—', label: 'Started' },
  ];

  return (
    <div style={{ padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
      <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
        {stats.map((stat) => (
          <div key={stat.label}>
            <div style={{ font: '600 20px/1 var(--font-mono)', color: 'var(--text-strong)' }}>{stat.value}</div>
            <div style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
