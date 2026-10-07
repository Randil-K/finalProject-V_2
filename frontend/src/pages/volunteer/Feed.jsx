import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button } from '../../design-system';
import ChipButton from '../../components/ChipButton.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { Async } from '../../components/AsyncState.jsx';
import ReportCard from '../../components/ReportCard.jsx';
import { useAuth } from '../../auth/AuthContext.jsx';

const STATUS_FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'VERIFYING', label: 'Verifying' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'ESCALATED', label: 'With government officer' },
];

export default function Feed() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [status, setStatus] = React.useState('all');

  const state = useApi(() => api.reports.list({ status, size: 30 }), [status]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Nearby reports</h1>
          <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
            {user?.province ? `${user.province} · all reports` : 'All reported sites'}
          </p>
        </div>
        <Button iconLeft="camera" onClick={() => navigate(user ? '/app/submit' : '/login')}>
          Report pollution
        </Button>
      </div>

      {user && user.latitude == null ? (
        <Alert tone="info" title="Add your location to hear about pollution near you">
          <span style={{ display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            New reports and cleanups alert people within 5 km.
            <Button variant="secondary" size="sm" iconLeft="map-pin" onClick={() => navigate('/app/profile')}>
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

      <Async
        state={state}
        isEmpty={(data) => !data?.content?.length}
        empty="No reports in this status yet — widen your filter or report a site."
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
