import React from 'react';
import { useParams, useNavigate, useSearchParams, Navigate } from 'react-router-dom';
import { Icon, IconButton, Badge, Button, Avatar, Textarea, Alert } from '../../design-system';
import Modal from '../../components/Modal.jsx';
import MapLink from '../../components/MapLink.jsx';
import EvidenceGallery from '../../components/EvidenceGallery.jsx';
import ReportStatusBadge from '../../components/ReportStatusBadge.jsx';
import CommunityVerificationCard from '../../components/CommunityVerificationCard.jsx';
import ReviewStatusCard from '../../components/ReviewStatusCard.jsx';
import InfoRequestsPanel from '../../components/InfoRequestsPanel.jsx';
import ResourceFields, { buildResources, resourcePlanFrom } from '../../components/ResourceFields.jsx';
import UserLink from '../../components/UserLink.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { formatDate, locationLine } from '../../lib/format.js';
import { mediaUrl } from '../../api/client.js';

const projectHref = (projectId) => `/console/projects/${projectId}`;

export default function ReportReview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const state = useApi(() => api.reports.get(id), [id]);
  const infoState = useApi(() => api.reports.infoRequests(id), [id]);
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get('tab') === 'info' ? 'info' : 'review';
  const showTab = (next) => setSearchParams(next === 'info' ? { tab: 'info' } : {}, { replace: true });
  const [comment, setComment] = React.useState('');
  const [plan, setPlan] = React.useState(() => resourcePlanFrom(null));
  const [rejectOpen, setRejectOpen] = React.useState(false);
  const [error, setError] = React.useState(null);
  const [notice, setNotice] = React.useState(null);
  const [busy, setBusy] = React.useState(false);

  const isAdmin = user?.role === 'ADMIN';
  const isAuthority = user?.role === 'AUTHORITY';

  async function run(action, successMessage) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await action();
      const message = typeof successMessage === 'function' ? successMessage(updated) : successMessage;
      if (updated.projectId) {
        // The report is a project now — carry on from the project page.
        navigate(projectHref(updated.projectId), { replace: true, state: { notice: message } });
        return;
      }
      state.setData(updated);
      infoState.reload();
      setNotice(message);
      setComment('');
      setRejectOpen(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const moderate = (decision, message) => run(() => api.reports.moderate(id, decision, comment.trim() || null), message);
  const decide = (decision, message) => run(() => api.reports.authorityDecision(id, decision, comment.trim()), message);

  /** Approving commits the resources, so they travel with the decision. */
  function approveAsOfficer(message) {
    const { payload, error: invalid } = buildResources(plan);
    if (invalid) {
      setNotice(null);
      setError(invalid);
      return;
    }
    run(() => api.reports.authorityDecision(id, 'APPROVED', comment.trim(), payload), message);
  }

  return (
    <Async state={state}>
      {(report) => {
        const canModerate = isAdmin && ['PENDING', 'VERIFYING', 'VERIFIED'].includes(report.status);
        // Approval and questions wait for the community confirmations; rejection doesn't.
        const verified = report.status === 'VERIFIED';
        const waitingForReporter = report.infoRequestStatus === 'OPEN';
        const requests = infoState.data || [];
        const answered = requests.filter((r) => r.status === 'ANSWERED').length;
        const canDecide = isAuthority && report.status === 'ESCALATED';
        const hasComment = Boolean(comment.trim());
        if (report.projectId) return <Navigate to={projectHref(report.projectId)} replace />;

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)', maxWidth: 760 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <IconButton icon="chevron-left" label="Back to queue" onClick={() => navigate('/console')} />
              <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{report.reference}</span>
              <ReportStatusBadge status={report.status} style={{ marginLeft: 'auto' }} />
            </div>

            {notice ? <Alert tone="success" title="Done" onDismiss={() => setNotice(null)}>{notice}</Alert> : null}
            {error && !rejectOpen ? <Alert tone="danger" title="That didn't work">{error}</Alert> : null}

            <EvidenceGallery report={report} />

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>{report.title}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-body-sm)', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
                <Icon name="map-pin" size="sm" />
                {locationLine(report)} · {report.latitude?.toFixed(4)}° N, {report.longitude?.toFixed(4)}° E
              </div>
              <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)' }}>{report.description}</p>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <Avatar name={report.reporter?.fullName || ''} src={mediaUrl(report.reporter?.avatarUrl)} size="sm" />
                <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginRight: 'auto' }}>
                  {user?.id === report.reporter?.id ? 'Reported by you' : <>Reported by <UserLink user={report.reporter} style={{ color: 'var(--text-link)', fontWeight: 600 }} /></>} · {formatDate(report.createdAt)}
                </span>
                <MapLink latitude={report.latitude} longitude={report.longitude} />
              </div>
            </div>

            <div role="tablist" aria-label="Report sections" style={{ display: 'flex', gap: 'var(--space-5)', borderBottom: '1px solid var(--border-subtle)' }}>
              {[
                { key: 'review', label: 'Review' },
                { key: 'info', label: 'Additional information', count: requests.length, highlight: answered > 0 },
              ].map((t) => (
                <button
                  key={t.key}
                  type="button"
                  role="tab"
                  aria-selected={tab === t.key}
                  onClick={() => showTab(t.key)}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '10px 2px',
                    marginBottom: -1,
                    borderBottom: `2px solid ${tab === t.key ? 'var(--accent)' : 'transparent'}`,
                    font: 'var(--text-label)',
                    color: tab === t.key ? 'var(--text-strong)' : 'var(--text-muted)',
                    cursor: 'pointer',
                  }}
                >
                  {t.label}
                  {t.count ? <Badge tone={t.highlight ? 'accent' : 'neutral'} size="sm">{t.count}</Badge> : null}
                </button>
              ))}
            </div>

            {tab === 'info' ? (
              <>
                <Async state={infoState}>
                  {(list) => <InfoRequestsPanel requests={list} reporterName={report.reporter?.fullName} />}
                </Async>
              </>
            ) : (
            <>
            <CommunityVerificationCard report={report} />

            <ReviewStatusCard report={report} />

            {isAdmin && report.status === 'ESCALATED' ? (
              <Alert tone="info" title="With the government officer">
                Nothing more for you to do until a government officer approves, rejects or asks for more detail.
              </Alert>
            ) : null}
            {isAuthority && report.status !== 'ESCALATED' && !report.projectId && report.authorityDecision == null ? (
              <Alert tone="info" title="Not sent to the government officer yet">
                An administrator has to approve this report before you can decide on it.
              </Alert>
            ) : null}

            {canModerate || canDecide ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', padding: 'var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
                <div>
                  <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>
                    {canDecide ? 'Your decision as the government officer' : 'Your review as administrator'}
                  </span>
                  <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: 2 }}>
                    {canDecide
                      ? `Approving creates the cleanup project, with ${report.reporter?.fullName} as project owner.`
                      : 'Approving sends the report to the government officer for a decision.'}
                  </p>
                </div>
                {canDecide ? (
                  <>
                    <Alert tone="warning" title="Set the resources this cleanup needs">
                      A project cannot be approved without them. The owner and everyone who joins work
                      from what you set here, and you can revise it later from the project.
                    </Alert>
                    <ResourceFields plan={plan} onChange={setPlan} />
                  </>
                ) : null}
                <Textarea
                  label="Official comment"
                  hint={canDecide
                    ? 'Required for every decision — the reporter and administrators see it.'
                    : 'Required to request more information or reject — the reporter sees it.'}
                  placeholder={canDecide
                    ? 'e.g. Approved. Clear the drums first and coordinate with the Kalpitiya fisheries office.'
                    : 'Notes for the government officer, what information you need from the reporter, or why you are rejecting.'}
                  rows={3}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
                <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
                  {canModerate ? (
                    <>
                      <Button
                        iconLeft="flag"
                        disabled={busy || !verified}
                        onClick={() => moderate('APPROVED', 'Approved. The report is with the government officer, who has been alerted.')}
                      >
                        Approve and send to government officer
                      </Button>
                      <Button
                        variant="secondary"
                        iconLeft="message-square"
                        disabled={busy || !hasComment || !verified || waitingForReporter}
                        onClick={() => moderate('MORE_INFO_REQUESTED', 'More information requested. The reporter has a critical alert, and their answer will appear under Additional information.')}
                      >
                        Request more info
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        iconLeft="shield-check"
                        disabled={busy || !hasComment}
                        onClick={() => approveAsOfficer((updated) => `Approved. Project ${updated.projectReference} has been created with its resources, and ${updated.reporter?.fullName} is its project owner.`)}
                      >
                        Approve and create project
                      </Button>
                      <Button
                        variant="secondary"
                        iconLeft="message-square"
                        disabled={busy || !hasComment || waitingForReporter}
                        onClick={() => decide('MORE_INFO_REQUESTED', 'More information requested. The reporter has a critical alert, and their answer will appear under Additional information.')}
                      >
                        Request more info
                      </Button>
                    </>
                  )}
                  <Button variant="danger" iconLeft="x" disabled={busy} onClick={() => { setError(null); setRejectOpen(true); }}>
                    Reject report
                  </Button>
                </div>
              </div>
            ) : null}

            </>
            )}

            <Modal
              open={rejectOpen}
              title="Reject this report?"
              description="The reporter is notified with your reason, and the report is closed."
              onClose={() => setRejectOpen(false)}
              footer={
                <>
                  <Button variant="secondary" onClick={() => setRejectOpen(false)}>Cancel</Button>
                  <Button
                    variant="danger"
                    disabled={busy || !hasComment}
                    onClick={() => (canDecide
                      ? decide('REJECTED', 'Report rejected. The reporter and administrators have been notified.')
                      : moderate('REJECTED', 'Report rejected. The reporter has been notified.'))}
                  >
                    Reject report
                  </Button>
                </>
              }
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                {error ? <Alert tone="danger" title="Could not reject">{error}</Alert> : null}
                <Textarea
                  label="Reason"
                  required
                  rows={3}
                  placeholder="Explain why this report is being rejected…"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
              </div>
            </Modal>
          </div>
        );
      }}
    </Async>
  );
}
