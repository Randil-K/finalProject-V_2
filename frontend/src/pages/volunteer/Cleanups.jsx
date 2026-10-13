import React from 'react';
import { Button, Select } from '../../design-system';
import ChipButton from '../../components/ChipButton.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { Async } from '../../components/AsyncState.jsx';
import ProjectCard from '../../components/ProjectCard.jsx';
import { PROVINCES, plural } from '../../lib/format.js';

const FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'PLANNED', label: 'Planned' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'COMPLETED', label: 'Completed' },
];

export default function Cleanups() {
  const { user } = useAuth();
  const [status, setStatus] = React.useState('all');
  const [province, setProvince] = React.useState('');
  const state = useApi(() => api.projects.list({ status }), [status, user?.id]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
      <div>
        <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Cleanups</h1>
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
          Reports the government officer approved, now running as cleanup projects. Join one near you.
        </p>
      </div>

      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 2 }}>
        {FILTERS.map((f) => (
          <ChipButton key={f.value} selected={status === f.value} onClick={() => setStatus(f.value)}>
            {f.label}
          </ChipButton>
        ))}
      </div>

      <Async
        state={state}
        isEmpty={(list) => !list?.length}
        empty="No cleanup projects here yet — a report becomes a project once the government officer approves it."
        emptyIcon="users"
      >
        {(projects) => {
          const shown = province ? projects.filter((p) => p.province === province) : projects;
          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <Select
                  placeholder="Every province"
                  aria-label="Province"
                  options={PROVINCES.map((name) => ({ value: name, label: name }))}
                  value={province}
                  onChange={(e) => setProvince(e.target.value)}
                  style={{ minWidth: 210 }}
                />
                {province ? (
                  <Button variant="ghost" size="sm" onClick={() => setProvince('')}>Every province</Button>
                ) : null}
                <span style={{ marginLeft: 'auto', font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  {province ? `${plural(shown.length, 'cleanup')} of ${projects.length}` : plural(projects.length, 'cleanup')}
                </span>
              </div>

              {shown.length === 0 ? (
                <div style={{ padding: 'var(--space-6)', textAlign: 'center', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-lg)' }}>
                  <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
                    No cleanups in {province} with this status yet.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 'var(--space-4)' }}>
                  {shown.map((project) => (
                    <ProjectCard key={project.id} project={project} to={`/app/cleanups/${project.id}`} />
                  ))}
                </div>
              )}
            </div>
          );
        }}
      </Async>
    </div>
  );
}
