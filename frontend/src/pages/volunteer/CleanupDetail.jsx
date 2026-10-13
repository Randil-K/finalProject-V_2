import React from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Icon, IconButton, Badge, Button, Alert } from '../../design-system';
import { Async } from '../../components/AsyncState.jsx';
import MapLink from '../../components/MapLink.jsx';
import ParticipantList from '../../components/ParticipantList.jsx';
import ProjectProgress from '../../components/ProjectProgress.jsx';
import ProjectStory from '../../components/ProjectStory.jsx';
import ProgressUpdateForm from '../../components/ProgressUpdateForm.jsx';
import ProjectOrigin from '../../components/ProjectOrigin.jsx';
import ProjectStatusBar from '../../components/ProjectStatusBar.jsx';
import ProjectResources from '../../components/ProjectResources.jsx';
import UserLink from '../../components/UserLink.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { PROJECT_STATUS_LABEL, PROJECT_STATUS_TONE, locationLine } from '../../lib/format.js';

export default function CleanupDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const state = useApi(() => api.projects.get(id), [id, user?.id]);
  const [joining, setJoining] = React.useState(false);
  const [error, setError] = React.useState(null);

  async function join() {
    setJoining(true);
    setError(null);
    try {
      state.setData(await api.projects.join(id));
    } catch (err) {
      setError(err.message);
    } finally {
      setJoining(false);
    }
  }

  function participation(project, isOwner) {
    if (project.status === 'COMPLETED') {
      return (
        <Alert tone="success" title="This cleanup is finished">
          {project.debrisRemovedKg != null ? `${project.debrisRemovedKg} kg of debris was removed. ` : ''}
          Thank you to everyone who took part.
        </Alert>
      );
    }
    if (!user) {
      return (
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
          <Link to="/login" state={{ from: `/app/cleanups/${id}` }}>Sign in</Link> to join this cleanup.
        </p>
      );
    }
    if (isOwner) {
      return (
        <Alert tone="info" title="You're the project owner">
          {project.progressOpenToParticipants
            ? 'You reported this site but left the updates open, so anyone who joins can post the progress — and so can you.'
            : 'You reported this site and the government officer approved it. Post progress below as the cleanup goes.'}
        </Alert>
      );
    }
    if (project.joined) {
      return (
        <Alert tone="success" title="You've joined this cleanup">
          {project.progressOpenToParticipants
            ? 'The owner left the progress updates open to whoever turns up — post the photos and notes below as the work goes.'
            : "You'll get an alert when it's finished."}
        </Alert>
      );
    }
    if (project.resourcesGatheredAt) {
      return (
        <Alert tone="success" title="This cleanup has everyone it needs">
          All the volunteers and divers the government officer asked for have signed up, so it is closed to
          new joiners. Watch the feed — there will be other sites near you.
        </Alert>
      );
    }
    if (!project.resources?.finalizedAt) {
      return (
        <Alert tone="info" title="Not open to join yet">
          The government officer is still working out how many volunteers and divers this cleanup needs.
          You'll be alerted as soon as it opens.
        </Alert>
      );
    }
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Button size="lg" fullWidth iconLeft="hand-heart" loading={joining} disabled={joining} onClick={join}>
          {joining ? 'Joining…' : 'Join this cleanup'}
        </Button>
        {project.progressOpenToParticipants ? (
          <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
            The owner left the progress updates open: join and you can post this cleanup's photos and notes yourself.
          </p>
        ) : null}
      </div>
    );
  }

  return (
    <Async state={state}>
      {(project) => {
        const isOwner = user?.id === project.owner?.id;
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <IconButton icon="chevron-left" label="Back" onClick={() => navigate(-1)} />
              <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{project.reference}</span>
              <Badge tone={PROJECT_STATUS_TONE[project.status]} style={{ marginLeft: 'auto' }}>
                {PROJECT_STATUS_LABEL[project.status]}
              </Badge>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>{project.title}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
                <Icon name="map-pin" size="sm" />
                {locationLine(project)}
                {project.owner ? (isOwner ? ' · you are the project owner' : <>{' · project owner '}<UserLink user={project.owner} style={{ color: 'var(--text-link)', fontWeight: 600 }} /></>) : ''}
              </div>
              {project.description ? (
                <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)', marginTop: 4 }}>{project.description}</p>
              ) : null}
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 4 }}>
                <MapLink latitude={project.latitude} longitude={project.longitude} label="Meeting point on the map" />
              </div>
            </div>

            <ProjectStatusBar project={project} />

            <ProjectResources project={project} />

            <ProjectProgress project={project} />

            {participation(project, isOwner)}
            {error ? <Alert tone="danger" title="That didn't work">{error}</Alert> : null}

            {project.participants ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Who's taking part</span>
                <ParticipantList
                  project={project}
                  canRate={isOwner && project.status === 'COMPLETED'}
                  onRated={state.setData}
                />
              </div>
            ) : null}

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Progress</span>
              <ProjectStory project={project} onChanged={state.setData} />
            </div>

            {project.canPostProgress ? (
              <ProgressUpdateForm projectId={project.id} onUpdated={state.setData} />
            ) : null}

            <ProjectOrigin project={project} />
          </div>
        );
      }}
    </Async>
  );
}
