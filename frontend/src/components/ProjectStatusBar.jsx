import React from 'react';
import { Icon } from '../design-system';
import { formatDate } from '../lib/format.js';

/** Approved → Resources assigned → Resources gathered → In progress → Completed. */
export function projectStages(project) {
  const resourcesDone = Boolean(project.resources?.finalized);
  const gathered = Boolean(project.resourcesGatheredAt);
  const started = project.status === 'ACTIVE' || project.status === 'COMPLETED';
  const completed = project.status === 'COMPLETED';
  return [
    { key: 'approved', label: 'Approved', done: true, date: project.createdAt },
    { key: 'resources', label: 'Resources assigned', done: resourcesDone, date: project.resources?.finalizedAt },
    { key: 'gathered', label: 'Resources gathered', done: gathered, date: project.resourcesGatheredAt },
    { key: 'progress', label: 'In progress', done: started, date: project.startedAt },
    { key: 'completed', label: 'Completed', done: completed, date: project.completedAt },
  ];
}

export default function ProjectStatusBar({ project, compact = false }) {
  const stages = projectStages(project);
  const current = stages.findIndex((stage) => !stage.done);
  const dot = compact ? 18 : 28;

  return (
    <div
      role="list"
      aria-label="Project status"
      style={compact ? undefined : { padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}
    >
      {compact ? null : <span style={{ display: 'block', font: 'var(--text-label)', color: 'var(--text-heading)', marginBottom: 'var(--space-4)' }}>Project status</span>}
      <div style={{ display: 'grid', gridTemplateColumns: `repeat(${stages.length}, 1fr)` }}>
        {stages.map((stage, index) => {
          const isCurrent = index === current;
          const color = stage.done ? 'var(--status-verified)' : isCurrent ? 'var(--accent)' : 'var(--border-default)';
          return (
            <div key={stage.key} role="listitem" aria-current={isCurrent ? 'step' : undefined} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, position: 'relative', minWidth: 0 }}>
              {index > 0 ? (
                <span
                  aria-hidden
                  style={{
                    position: 'absolute',
                    top: dot / 2 - 1,
                    right: '50%',
                    width: '100%',
                    height: 2,
                    background: stage.done || isCurrent ? 'var(--status-verified)' : 'var(--border-default)',
                  }}
                />
              ) : null}
              <span
                style={{
                  position: 'relative',
                  width: dot,
                  height: dot,
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: stage.done ? 'var(--status-verified)' : 'var(--surface-card)',
                  border: `2px solid ${color}`,
                }}
              >
                {stage.done ? <Icon name="check" size={compact ? 10 : 14} color="var(--white)" /> : null}
              </span>
              <span
                style={{
                  font: compact ? 'var(--text-micro)' : 'var(--text-caption)',
                  fontWeight: isCurrent ? 600 : undefined,
                  color: stage.done || isCurrent ? 'var(--text-heading)' : 'var(--text-muted)',
                  textAlign: 'center',
                }}
              >
                {stage.label}
              </span>
              {!compact && stage.done && stage.date ? (
                <span style={{ font: 'var(--text-micro)', color: 'var(--text-muted)' }}>{formatDate(stage.date, false)}</span>
              ) : null}
            </div>
          );
        })}
      </div>
    </div>
  );
}
