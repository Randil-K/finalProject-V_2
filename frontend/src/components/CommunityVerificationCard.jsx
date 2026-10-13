import React from 'react';
import { Badge, ProgressBar } from '../design-system';

/** How many confirmed and how many disputed, beside the bar, said in words. */
function Tally({ label, count, tone }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'baseline', gap: 5, color: tone }}>
      <span style={{ font: '600 13px/1 var(--font-mono)' }}>{count}</span>
      <span style={{ font: 'var(--text-caption)' }}>{label}</span>
    </span>
  );
}

/** Trust percentage against the threshold; `children` holds the voting controls where they apply. */
export default function CommunityVerificationCard({ report, children }) {
  const total = report.confirmVotes + report.disputeVotes;
  const needed = report.minimumConfirmations ?? 8;
  const passed = report.trustPercentage >= report.thresholdPercent && report.confirmVotes >= needed;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
      <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Community verification</span>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
        <span style={{ flex: 1, minWidth: 0 }}>
          <ProgressBar value={report.trustPercentage} tone={passed ? 'verified' : 'warning'} />
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-3)', flex: '0 0 auto' }}>
          <Tally label="confirmed" count={report.confirmVotes} tone="var(--status-verified)" />
          <Tally label="disputed" count={report.disputeVotes} tone="var(--text-muted)" />
        </span>
      </div>

      <Badge tone={passed ? 'success' : 'warning'} style={{ whiteSpace: 'normal', height: 'auto', padding: '6px 10px', alignSelf: 'flex-start' }}>
        {total === 0
          ? `No votes yet — needs ${report.thresholdPercent}% trust`
          : `${report.trustPercentage}% trust (${report.thresholdPercent}% needed)`}
      </Badge>
      {children}
    </div>
  );
}
