import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Icon, Badge, Avatar, Button, Select } from '../../design-system';
import ChipButton from '../../components/ChipButton.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { DISTRICTS, SEVERITY_LABEL, SEVERITY_VAR, formatDate, plural, reviewStage } from '../../lib/format.js';
import { mediaUrl } from '../../api/client.js';

/** The officer's queue: their own approvals, then the cleanups still waiting to be resourced. */
const OFFICER_FILTERS = [
  { value: 'all', label: 'All reports' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'ESCALATED', label: 'Waiting for approval' },
  { value: 'AWAITING_RESOURCES', label: 'Waiting for resource assigning' },
  { value: 'REJECTED', label: 'Rejected' },
];

/**
 * The administrator's queue. "Waiting for approval" is what is waiting for theirs — the reports the
 * community has verified. Resourcing a cleanup is the officer's step, so it does not appear here.
 */
const ADMIN_FILTERS = [
  { value: 'all', label: 'All reports' },
  { value: 'VERIFIED', label: 'Waiting for approval' },
  { value: 'ESCALATED', label: 'With government officer' },
  { value: 'REJECTED', label: 'Rejected' },
];

/** Approved cleanups nobody has resourced yet come from their own listing, not a status. */
const listFor = (filter) => (filter === 'AWAITING_RESOURCES'
  ? { reviewQueue: true, awaitingResources: true, size: 100 }
  : { status: filter, reviewQueue: true, size: 100 });

/** Worst first, so an officer can work down the list. */
const RISK_OPTIONS = [
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

const ORDER_OPTIONS = [
  { value: 'newest', label: 'Latest reported first' },
  { value: 'oldest', label: 'Oldest reported first' },
];

const COLUMNS = '48px 90px minmax(200px, 1fr) 100px 140px 190px 80px';

export default function Queue() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isOfficer = user?.role === 'AUTHORITY';
  const filters = isOfficer ? OFFICER_FILTERS : ADMIN_FILTERS;
  // Officers mostly work on what administrators have sent them.
  const [filter, setFilter] = React.useState(isOfficer ? 'ESCALATED' : 'all');
  const [risk, setRisk] = React.useState('');
  const [district, setDistrict] = React.useState('');
  const [order, setOrder] = React.useState('newest');
  const state = useApi(() => api.reports.list(listFor(filter)), [filter]);

  const narrowed = Boolean(risk || district);
  const clear = () => { setRisk(''); setDistrict(''); };

  function matches(report) {
    if (risk && report.severity !== risk) return false;
    if (district) {
      // Reports filed before districts were recorded only carry a site name, so fall back to that.
      const named = report.district
        || [report.locationName, report.province].filter(Boolean).join(' ');
      if (report.district ? report.district !== district : !named.toLowerCase().includes(district.toLowerCase())) {
        return false;
      }
    }
    return true;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
      <div>
        <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Review queue</h1>
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {filters.map((f) => (
          <ChipButton key={f.value} selected={filter === f.value} onClick={() => setFilter(f.value)}>
            {f.label}
          </ChipButton>
        ))}
      </div>

      <Async
        state={state}
        isEmpty={(data) => !data?.content?.length}
        empty={filter === 'AWAITING_RESOURCES'
          ? 'Every approved cleanup has its resources — nothing is waiting here.'
          : 'Nothing in this queue — try another status filter.'}
        emptyIcon="list-filter"
      >
        {(data) => {
          const all = data.content;
          const rows = all.filter(matches).sort((a, b) => (order === 'oldest'
            ? (new Date(a.createdAt) - new Date(b.createdAt)) || (a.id - b.id)
            : (new Date(b.createdAt) - new Date(a.createdAt)) || (b.id - a.id)));

          return (
            <>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <Select
                  placeholder="Any risk level"
                  options={RISK_OPTIONS}
                  value={risk}
                  onChange={(e) => setRisk(e.target.value)}
                  style={{ minWidth: 170 }}
                />
                <Select
                  placeholder="Any district"
                  options={DISTRICTS.map((name) => ({ value: name, label: name }))}
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  style={{ minWidth: 190 }}
                />
                <Select
                  options={ORDER_OPTIONS}
                  value={order}
                  onChange={(e) => setOrder(e.target.value)}
                  style={{ minWidth: 200 }}
                />
                {narrowed ? (
                  <Button variant="ghost" size="sm" onClick={clear}>Clear filters</Button>
                ) : null}
                <span style={{ marginLeft: 'auto', font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  {narrowed ? `${plural(rows.length, 'report')} of ${all.length}` : plural(all.length, 'report')}
                </span>
              </div>

              {rows.length === 0 ? (
                <div style={{ padding: 'var(--space-6)', textAlign: 'center', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-lg)' }}>
                  <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
                    No reports match these filters.
                  </p>
                </div>
              ) : (
                <div style={{ overflowX: 'auto', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-lg)' }}>
                  <div style={{ minWidth: 900 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: COLUMNS, gap: 8, padding: '10px var(--space-4)', background: 'var(--surface-sunken)', font: 'var(--text-micro)', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 'var(--tracking-micro)' }}>
                      <span>#</span>
                      <span>Ref</span>
                      <span>Report</span>
                      <span>Risk</span>
                      <span>Location</span>
                      <span>Review</span>
                      <span>Reported</span>
                    </div>

                    {rows.map((report, index) => {
                      const stage = reviewStage(report);
                      return (
                        <div
                          key={report.id}
                          role="link"
                          tabIndex={0}
                          onClick={() => navigate(`/console/reports/${report.id}`)}
                          onKeyDown={(e) => { if (e.key === 'Enter') navigate(`/console/reports/${report.id}`); }}
                          style={{
                            display: 'grid',
                            gridTemplateColumns: COLUMNS,
                            gap: 8,
                            alignItems: 'center',
                            padding: 'var(--space-3) var(--space-4)',
                            borderTop: '1px solid var(--border-subtle)',
                            cursor: 'pointer',
                            font: 'var(--text-body-sm)',
                          }}
                        >
                          <span style={{ font: '600 12px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{index + 1}</span>
                          <span style={{ font: '600 12px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{report.reference}</span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                            <Avatar name={report.reporter?.fullName || ''} src={mediaUrl(report.reporter?.avatarUrl)} size="xs" />
                            <span style={{ color: 'var(--text-heading)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {report.title}
                            </span>
                          </div>
                          <span>
                            {report.severity ? (
                              <Badge
                                size="sm"
                                style={{
                                  background: `var(${SEVERITY_VAR[report.severity]}-bg)`,
                                  color: `var(${SEVERITY_VAR[report.severity]})`,
                                }}
                              >
                                {SEVERITY_LABEL[report.severity]}
                              </Badge>
                            ) : '—'}
                          </span>
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--text-muted)', minWidth: 0 }}>
                            <Icon name="map-pin" size="xs" />
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{report.locationName}</span>
                          </span>
                          <span><Badge tone={stage.tone} size="sm">{stage.label}</Badge></span>
                          <span style={{ color: 'var(--text-muted)', font: 'var(--text-caption)' }}>
                            {formatDate(report.createdAt, false)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </>
          );
        }}
      </Async>
    </div>
  );
}
