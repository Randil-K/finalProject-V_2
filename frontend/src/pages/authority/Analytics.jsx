import React from 'react';
import { Icon, Card, Button, Select } from '../../design-system';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { Async } from '../../components/AsyncState.jsx';
import { DISTRICTS, PROVINCES, timeAgo } from '../../lib/format.js';

/** How often the dashboard refreshes itself while someone is looking at it. */
const REFRESH_MS = 60_000;

/** Enough colours for the deepest stack, kept in a fixed order so a legend can match them. */
const SERIES_COLOURS = ['var(--sea-600)', 'var(--buoy-600)', 'var(--tide-600)', 'var(--coral-600)'];

const CHART_HEIGHT = 150;

/** "North Western Province" is too long to sit under a column; the province is understood. */
const shortProvince = (name) => name.replace(/\s*Province$/, '');

/** Every province in the usual order, with a zero for any that has nothing to show. */
function allProvinces(rows) {
  const counts = new Map((rows || []).map((row) => [row.name, row.count]));
  return PROVINCES.map((name) => ({
    label: shortProvince(name),
    title: name,
    parts: [{ label: name, count: counts.get(name) || 0 }],
  }));
}

/**
 * A vertical bar chart. Each column is a list of parts stacked on top of each other — a column with
 * one part is a plain bar, which is what most of these are.
 */
function ColumnChart({ columns, legend }) {
  const totals = columns.map((column) => column.parts.reduce((sum, part) => sum + part.count, 0));
  const max = Math.max(1, ...totals);

  return (
    <div style={{ marginTop: 'var(--space-4)' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 10, height: CHART_HEIGHT + 22 }}>
        {columns.map((column, index) => (
          <div
            key={column.label}
            title={column.title || column.label}
            style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, height: '100%', justifyContent: 'flex-end' }}
          >
            <span style={{ font: '600 12px/1 var(--font-mono)', color: 'var(--text-strong)' }}>{totals[index]}</span>
            <div
              style={{
                width: '100%',
                maxWidth: 46,
                height: Math.max(4, (totals[index] / max) * CHART_HEIGHT),
                display: 'flex',
                flexDirection: 'column-reverse',
                borderRadius: 'var(--radius-sm) var(--radius-sm) 0 0',
                overflow: 'hidden',
                background: 'var(--gray-200)',
              }}
            >
              {column.parts.map((part, partIndex) => (
                <div
                  key={part.label}
                  title={`${part.label}: ${part.count}`}
                  style={{
                    height: `${totals[index] ? (part.count / totals[index]) * 100 : 0}%`,
                    background: SERIES_COLOURS[partIndex % SERIES_COLOURS.length],
                  }}
                />
              ))}
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
        {columns.map((column) => (
          <span
            key={column.label}
            title={column.title || column.label}
            style={{ flex: 1, minWidth: 0, textAlign: 'center', font: 'var(--text-micro)', color: 'var(--text-muted)', lineHeight: 1.3 }}
          >
            {column.label}
          </span>
        ))}
      </div>

      {legend?.length ? (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-3)', marginTop: 'var(--space-4)' }}>
          {legend.map((label, index) => (
            <span key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
              <span style={{ width: 10, height: 10, borderRadius: 3, background: SERIES_COLOURS[index % SERIES_COLOURS.length] }} />
              {label}
            </span>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function ChartCard({ title, blurb, children, wide = false }) {
  return (
    <Card padding="lg" style={wide ? { gridColumn: '1 / -1' } : undefined}>
      <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>{title}</span>
      {blurb ? <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: 2 }}>{blurb}</p> : null}
      {children}
    </Card>
  );
}

export default function Analytics() {
  const [district, setDistrict] = React.useState('');
  const state = useApi(() => api.analytics.summary(district || undefined), [district]);
  const { reload } = state;

  // Keep the figures current: on a timer, and whenever the window is looked at again.
  React.useEffect(() => {
    const timer = setInterval(reload, REFRESH_MS);
    const onFocus = () => {
      if (document.visibilityState === 'visible') reload();
    };
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onFocus);
    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onFocus);
    };
  }, [reload]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Analytics</h1>
          <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
            {district
              ? `Reports, cleanups and people in ${district} district.`
              : 'System-wide performance and regional pollution trends.'}
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <Select
            placeholder="Every district"
            aria-label="District"
            options={DISTRICTS.map((name) => ({ value: name, label: `${name} district` }))}
            value={district}
            onChange={(e) => setDistrict(e.target.value)}
            style={{ minWidth: 190 }}
          />
          <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
            {state.loading ? 'Updating…' : state.data?.generatedAt ? `Updated ${timeAgo(state.data.generatedAt)}` : null}
          </span>
          <Button variant="secondary" size="sm" iconLeft="refresh-cw" disabled={state.loading} onClick={reload}>
            Refresh
          </Button>
        </div>
      </div>

      <Async state={state}>
        {(summary) => {
          // The API names each part `name`; the chart calls it `label`.
          const statusColumns = (summary.reportStatusBreakdown || []).map((column) => ({
            label: column.label,
            parts: (column.parts || []).map((part) => ({ label: part.name, count: part.count })),
          }));
          // Only the deepest column needs naming; the single-part ones say it on the axis.
          const legend = statusColumns
            .map((column) => column.parts)
            .reduce((deepest, parts) => (parts.length > deepest.length ? parts : deepest), [])
            .map((part) => part.label);

          const stages = Object.entries(summary.projectsByStage || {})
            .map(([label, count]) => ({ label, parts: [{ label, count }] }));

          return (
            <>
              {summary.district ? (
                <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  <Icon name="info" size="xs" /> Every figure below is for {summary.district} district only.
                </p>
              ) : null}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: 'var(--space-4)' }}>
                <ChartCard title="Reports by review stage" blurb="Where every report that is not finished has got to.">
                  <ColumnChart columns={statusColumns} legend={legend.length > 1 ? legend : null} />
                </ChartCard>

                <ChartCard title="Cleanup projects" blurb="Where every project has got to.">
                  <ColumnChart columns={stages} />
                </ChartCard>

                {/* A district sits in one province, so these two only say something island-wide. */}
                {summary.district ? null : (
                  <>
                    <ChartCard title="Reports by province" blurb="Where the reports are coming from." wide>
                      <ColumnChart columns={allProvinces(summary.provinceBreakdown)} />
                    </ChartCard>

                    <ChartCard
                      title="Taking part by province"
                      blurb="Members who have joined a cleanup, counted where the cleanup is."
                      wide
                    >
                      <ColumnChart columns={allProvinces(summary.participationByProvince)} />
                    </ChartCard>
                  </>
                )}
              </div>
            </>
          );
        }}
      </Async>
    </div>
  );
}
