import React from 'react';
import { useParams, useNavigate, Link, Navigate } from 'react-router-dom';
import { Icon, IconButton, Button, Avatar, Alert } from '../../design-system';
import MapLink from '../../components/MapLink.jsx';
import EvidenceGallery from '../../components/EvidenceGallery.jsx';
import ReportStatusBadge from '../../components/ReportStatusBadge.jsx';
import CommunityVerificationCard from '../../components/CommunityVerificationCard.jsx';
import ReviewStatusCard from '../../components/ReviewStatusCard.jsx';
import Discussion from '../../components/Discussion.jsx';
import UserLink from '../../components/UserLink.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { communityInputClosed, formatDate, locationLine } from '../../lib/format.js';
import { mediaUrl } from '../../api/client.js';

const projectHref = (projectId) => `/app/cleanups/${projectId}`;

export default function ReportDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const reportState = useApi(() => api.reports.get(id), [id, user?.id]);
  const [actionError, setActionError] = React.useState(null);
  const [voting, setVoting] = React.useState(false);

  async function vote(confirmed) {
    setActionError(null);
    setVoting(true);
    try {
      reportState.setData(await api.reports.vote(id, confirmed));
    } catch (error) {
      setActionError(error.message);
    } finally {
      setVoting(false);
    }
  }

  return (
    <Async state={reportState}>
      {(report) => {
        const votingClosed = communityInputClosed(report);
        const isReporter = user?.id === report.reporter?.id;
        // Once the government officer approves, the report lives on only as its project.
        if (report.projectId) return <Navigate to={projectHref(report.projectId)} replace />;

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <IconButton icon="chevron-left" label="Back" onClick={() => navigate(-1)} />
              <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{report.reference}</span>
              <ReportStatusBadge status={report.status} size="sm" style={{ marginLeft: 'auto' }} />
            </div>

            <EvidenceGallery report={report} />

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>{report.title}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
                <Icon name="map-pin" size="sm" />
                {locationLine(report)}
              </div>
              <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)', marginTop: 6 }}>{report.description}</p>
              <div style={{ alignSelf: 'flex-start', marginTop: 4 }}>
                <MapLink latitude={report.latitude} longitude={report.longitude} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6 }}>
                <UserLink user={report.reporter}><Avatar name={report.reporter?.fullName || ''} src={mediaUrl(report.reporter?.avatarUrl)} size="sm" /></UserLink>
                <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  {isReporter ? 'Reported by you' : <>Reported by <UserLink user={report.reporter} style={{ color: 'var(--text-link)', fontWeight: 600 }} /></>} · {formatDate(report.createdAt)}
                </span>
              </div>
            </div>

            {actionError ? <Alert tone="danger" title="That didn't work">{actionError}</Alert> : null}

            <CommunityVerificationCard report={report}>
              {votingClosed ? (
                <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  Voting has closed — an administrator has decided on this report.
                </p>
              ) : user ? (
                <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: 4 }}>
                  <Button
                    variant={report.myVote === true ? 'primary' : 'secondary'}
                    iconLeft="thumbs-up"
                    fullWidth
                    aria-pressed={report.myVote === true}
                    disabled={voting}
                    onClick={() => vote(true)}
                  >
                    {report.myVote === true ? 'Confirmed' : 'Confirm'} ({report.confirmVotes})
                  </Button>
                  <Button
                    variant={report.myVote === false ? 'danger' : 'secondary'}
                    iconLeft="thumbs-down"
                    fullWidth
                    aria-pressed={report.myVote === false}
                    disabled={voting}
                    onClick={() => vote(false)}
                  >
                    {report.myVote === false ? 'Disputed' : 'Dispute'} ({report.disputeVotes})
                  </Button>
                </div>
              ) : (
                <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  <Link to="/login" state={{ from: `/app/report/${id}` }}>Sign in</Link> to confirm or dispute this report.
                </p>
              )}
            </CommunityVerificationCard>

            <ReviewStatusCard report={report} />

            {isReporter && report.infoRequestStatus === 'OPEN' ? (
              <Alert tone="danger" title="Action needed: reviewers need more information">
                <span style={{ display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
                  <Button size="sm" variant="danger" iconRight="arrow-right" onClick={() => navigate(`/app/report/${id}/more-info`)}>
                    Add information
                  </Button>
                </span>
              </Alert>
            ) : null}
            {isReporter && report.infoRequestStatus === 'ANSWERED' ? (
              <Alert tone="success" title="Your additional information was sent">
                The reviewers have it. You'll get an alert when they decide.
              </Alert>
            ) : null}

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Discussion</span>
              <Discussion reportId={id} user={user} loginFrom={`/app/report/${id}`} closed={votingClosed} />
            </div>
          </div>
        );
      }}
    </Async>
  );
}
