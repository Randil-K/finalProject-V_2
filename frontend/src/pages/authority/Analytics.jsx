import React from 'react';
import { Icon, Card, Badge } from '../../design-system';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { Async } from '../../components/AsyncState.jsx';

const STAT_CARDS = [
  { key: 'reportedSites', label: 'Open reports', icon: 'map-pin' },
  { key: 'verifiedIncidents', label: 'Verified incidents', icon: 'badge-check' },
  { key: 'escalatedReports', label: 'With the government officer', icon: 'flag' },
  { key: 'activeProjects', label: 'Active projects', icon: 'waves-horizontal' },
  { key: 'completedProjects', label: 'Completed projects', icon: 'check-check' },
  { key: 'registeredVolunteers', label: 'Registered volunteers', icon: 'users' },
];

const STATUS_LABEL = {
  PENDING: 'Pending',
  VERIFYING: 'Verifying',
  VERIFIED: 'Verified',
  ESCALATED: 'With government officer',
  REJECTED: 'Rejected',
};

export default function Analytics() {
  const state = useApi(() => api.analytics.summary(), []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
      <div>
        <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Analytics</h1>
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
          System-wide performance and regional pollution trends.
        </p>
      </div>

      <Async state={state}>
        {(summary) => {
          const locations = summary.topLocations || [];
          const maxLocation = Math.max(1, ...locations.map((l) => l.count));
          const statuses = Object.entries(summary.reportsByStatus || {});
          const maxStatus = Math.max(1, ...statuses.map(([, count]) => count));

          return (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 'var(--space-4)' }}>
                {STAT_CARDS.map((stat) => (
                  <Card key={stat.key} padding="md">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                      <span style={{ width: 32, height: 32, borderRadius: 'var(--radius-sm)', background: 'var(--accent-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Icon name={stat.icon} size="sm" color="var(--accent-on-soft)" />
                      </span>
                      <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>{stat.label}</span>
                    </div>
                    <div style={{ font: '600 30px/1 var(--font-mono)', color: 'var(--text-strong)' }}>{summary[stat.key]}</div>
                  </Card>
                ))}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: 'var(--space-4)' }}>
                <Card padding="lg">
                  <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Reports by location</span>
                  {locations.length ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 'var(--space-4)' }}>
                      {locations.slice(0, 8).map((row) => (
                        <div key={row.name}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', font: 'var(--text-caption)', color: 'var(--text-body-color)', marginBottom: 4 }}>
                            <span>{row.name}</span>
                            <span style={{ font: '600 12px/1.5 var(--font-mono)', color: 'var(--text-strong)' }}>{row.count}</span>
                          </div>
                          <div style={{ height: 8, borderRadius: 'var(--radius-pill)', background: 'var(--gray-200)', overflow: 'hidden' }}>
                            <div style={{ width: `${(row.count / maxLocation) * 100}%`, height: '100%', borderRadius: 'var(--radius-pill)', background: 'var(--sea-600)' }} />
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 12 }}>No reports recorded yet.</p>
                  )}
                </Card>

                <Card padding="lg">
                  <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Open reports by status</span>
                  <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16, height: 160, marginTop: 'var(--space-6)', paddingBottom: 'var(--space-2)' }}>
                    {statuses.map(([status, count]) => (
                      <div key={status} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, flex: 1 }}>
                        <span style={{ font: '600 13px/1 var(--font-mono)', color: 'var(--text-strong)' }}>{count}</span>
                        <div
                          style={{
                            width: '100%',
                            maxWidth: 36,
                            height: Math.max(6, (count / maxStatus) * 100),
                            borderRadius: 'var(--radius-sm) var(--radius-sm) 0 0',
                            background: 'var(--accent)',
                          }}
                        />
                        <Badge tone="neutral" size="sm">{STATUS_LABEL[status] || status}</Badge>
                      </div>
                    ))}
                  </div>
                </Card>
              </div>
            </>
          );
        }}
      </Async>
    </div>
  );
}
