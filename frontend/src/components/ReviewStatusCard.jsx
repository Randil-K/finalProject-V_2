import React from 'react';
import { Badge } from '../design-system';
import { REVIEW_DECISION, formatDate } from '../lib/format.js';

function DecisionBadge({ decision, fallback }) {
  const d = decision ? REVIEW_DECISION[decision] : null;
  if (!d) return <Badge tone="neutral">{fallback}</Badge>;
  return <Badge tone={d.tone} icon={d.icon}>{d.label}</Badge>;
}

function Step({ title, badge, note, meta, children }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, paddingTop: 'var(--space-4)', borderTop: '1px solid var(--border-subtle)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>{title}</span>
        {badge}
      </div>
      {note ? (
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-body-color)', paddingLeft: 10, borderLeft: '2px solid var(--border-default)' }}>
          {note}
        </p>
      ) : null}
      {meta ? <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>{meta}</span> : null}
      {children}
    </div>
  );
}

/**
 * The whole review path: the community first, then the administrator and the government officer.
 * Approved reports redirect to their project, so this card only ever shows reports still under review.
 */
export default function ReviewStatusCard({ report }) {
  const confirmations = report.confirmVotes ?? 0;
  const needed = report.minimumConfirmations ?? 0;
  const trust = report.trustPercentage ?? 0;
  const communityVerified = confirmations >= needed && trust >= (report.thresholdPercent ?? 0);
  const communityBadge = communityVerified
    ? <Badge tone="success" icon="badge-check">Verified</Badge>
    : confirmations + (report.disputeVotes ?? 0) > 0
      ? <Badge tone="warning" icon="users">Verifying</Badge>
      : <Badge tone="neutral" icon="clock">No votes yet</Badge>;

  const authorityFallback = report.adminDecision === 'REJECTED' ? 'Not sent' : 'Waiting for administrator';
  const authorityMeta = report.decidedAt
    ? `Updated ${formatDate(report.decidedAt)}${report.authorityOfficer ? ` by ${report.authorityOfficer.fullName}` : ''}`
    : report.escalatedAt
      ? `Sent to the government officer ${formatDate(report.escalatedAt)}`
      : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
      <div>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Official review</span>
      </div>

      <Step
        title="Community"
        badge={communityBadge}
        meta={`${confirmations} of ${needed} confirmations · ${trust}% trust`}
      />

      <Step
        title="Administrator"
        badge={<DecisionBadge decision={report.adminDecision} fallback="Pending" />}
        note={report.moderationComment}
        meta={report.adminReviewedAt ? `Updated ${formatDate(report.adminReviewedAt)}` : null}
      />

      <Step
        title="Government officer"
        badge={<DecisionBadge decision={report.authorityDecision} fallback={authorityFallback} />}
        note={report.authorityComment}
        meta={authorityMeta}
      />

      <Step title="Cleanup project" badge={<Badge tone="neutral">After approval</Badge>} />
    </div>
  );
}
