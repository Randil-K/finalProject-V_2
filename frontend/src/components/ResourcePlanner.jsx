import React from 'react';
import { Alert, Badge, Button } from '../design-system';
import ResourceFields, { buildResources, resourcePlanFrom } from './ResourceFields.jsx';
import { api } from '../api/index.js';
import { formatDate } from '../lib/format.js';

/** A government officer revises what they committed to a project when they approved it. */
export default function ResourcePlanner({ project, onSaved }) {
  const [plan, setPlan] = React.useState(() => resourcePlanFrom(project.resources));
  const [busy, setBusy] = React.useState(null);
  const [error, setError] = React.useState(null);
  const [notice, setNotice] = React.useState(null);
  const finalized = Boolean(project.resources?.finalized);

  async function save(publish) {
    setError(null);
    setNotice(null);
    const { payload, error: invalid } = buildResources(plan);
    if (invalid) {
      setError(invalid);
      return;
    }
    setBusy(publish ? 'publish' : 'draft');
    try {
      const updated = await api.projects.updateResources(project.id, { ...payload, publish });
      onSaved(updated);
      setNotice(publish ? 'Resources updated and shown on the project.' : 'Draft saved.');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(null);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Assign resources</span>
        <Badge tone={finalized ? 'success' : 'warning'} size="sm">{finalized ? 'Finalized' : 'Not finalized'}</Badge>
      </div>

      {project.approval ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: 'var(--space-3) var(--space-4)', borderRadius: 'var(--radius-md)', background: 'var(--danger-bg)', borderLeft: '3px solid var(--danger)' }}>
          <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
            Official comment · {project.approval.officer?.fullName} · {formatDate(project.approval.decidedAt)}
          </span>
          <span style={{ font: 'var(--text-body)', color: 'var(--text-heading)', whiteSpace: 'pre-wrap' }}>{project.approval.comment}</span>
        </div>
      ) : null}

      {notice ? <Alert tone="success" title="Saved" onDismiss={() => setNotice(null)}>{notice}</Alert> : null}
      {error ? <Alert tone="danger" title="Not saved" onDismiss={() => setError(null)}>{error}</Alert> : null}

      <ResourceFields plan={plan} onChange={setPlan} />

      <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
        {!finalized ? (
          <Button variant="secondary" disabled={Boolean(busy)} onClick={() => save(false)}>
            {busy === 'draft' ? 'Saving…' : 'Save draft'}
          </Button>
        ) : null}
        <Button iconLeft="check" disabled={Boolean(busy)} onClick={() => save(true)}>
          {busy === 'publish' ? 'Saving…' : finalized ? 'Update resources' : 'Finalize resources'}
        </Button>
      </div>
    </div>
  );
}
