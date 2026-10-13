import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Select } from '../../design-system';
import ChipButton from '../../components/ChipButton.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { Async } from '../../components/AsyncState.jsx';
import ReportCard from '../../components/ReportCard.jsx';
import { useAuth } from '../../auth/AuthContext.jsx';
import { plural } from '../../lib/format.js';

const STATUS_FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'VERIFYING', label: 'Verifying' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'ESCALATED', label: 'With government officer' },
  { value: 'REJECTED', label: 'Rejected' },
];

const SEVERITY_OPTIONS = [
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

const ORDER_OPTIONS = [
  { value: 'newest', label: 'Latest reported first' },
  { value: 'oldest', label: 'Oldest reported first' },
];

const EMPTY_TEXT = {
  REJECTED: 'No rejected reports here — nothing in your province has been turned down.',
  ESCALATED: 'Nothing is with the government officer in your province right now.',
};

export default function Feed() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [status, setStatus] = React.useState('all');
  const [severity, setSeverity] = React.useState('');
  const [sort, setSort] = React.useState('newest');

  const state = useApi(
    () => api.reports.list({ status, severity: severity || undefined, sort, size: 30 }),
    [status, severity, sort],
  );

  const filtered = Boolean(severity) || sort === 'oldest';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Nearby reports</h1>
          <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
            {user?.province ? `Reports in ${user.province}` : 'All reported sites'}
          </p>
        </div>
        <Button iconLeft="camera" onClick={() => navigate(user ? '/app/submit' : '/login')}>
          Report pollution
        </Button>
      </div>

      {user && !user.province ? (
        <Alert tone="info" title="Tell us your province to see the reports around you">
          <span style={{ display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            Your feed shows the reports in your own province once we know where you are.
            <Button variant="secondary" size="sm" iconLeft="map-pin" onClick={() => navigate('/app/profile?edit=location')}>
              Set my province
            </Button>
          </span>
        </Alert>
      ) : null}

      {user && user.province && user.latitude == null ? (
        <Alert tone="info" title="Add your location to hear about pollution near you">
          <span style={{ display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            New reports and cleanups alert people within 5 km.
            <Button variant="secondary" size="sm" iconLeft="map-pin" onClick={() => navigate('/app/profile?edit=location')}>
              Add my location
            </Button>
          </span>
        </Alert>
      ) : null}

      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 2 }}>
        {STATUS_FILTERS.map((f) => (
          <ChipButton key={f.value} selected={status === f.value} onClick={() => setStatus(f.value)}>
            {f.label}
          </ChipButton>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        <Select
          placeholder="Any severity"
          aria-label="Severity"
          options={SEVERITY_OPTIONS}
          value={severity}
          onChange={(e) => setSeverity(e.target.value)}
          style={{ minWidth: 170 }}
        />
        <Select
          aria-label="Sort by date reported"
          options={ORDER_OPTIONS}
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          style={{ minWidth: 200 }}
        />
        {filtered ? (
          <Button variant="ghost" size="sm" onClick={() => { setSeverity(''); setSort('newest'); }}>
            Clear filters
          </Button>
        ) : null}
        {state.data?.content ? (
          <span style={{ marginLeft: 'auto', font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
            {plural(state.data.content.length, 'report')}
          </span>
        ) : null}
      </div>

      <Async
        state={state}
        isEmpty={(data) => !data?.content?.length}
        empty={EMPTY_TEXT[status] || 'No reports match these filters — widen them or report a site.'}
      >
        {(data) => (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 'var(--space-4)' }}>
            {data.content.map((report) => (
              <ReportCard key={report.id} report={report} />
            ))}
          </div>
        )}
      </Async>
    </div>
  );
}
