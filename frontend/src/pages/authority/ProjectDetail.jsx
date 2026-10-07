import React from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Icon, IconButton, Badge, Alert } from '../../design-system';
import PhotoPlaceholder from '../../components/PhotoPlaceholder.jsx';
import ProjectProgress from '../../components/ProjectProgress.jsx';
import ProjectTimeline from '../../components/ProjectTimeline.jsx';
import ProgressUpdateForm from '../../components/ProgressUpdateForm.jsx';
import ProjectOrigin from '../../components/ProjectOrigin.jsx';
import ProjectStatusBar from '../../components/ProjectStatusBar.jsx';
import ProjectResources from '../../components/ProjectResources.jsx';
import ResourcePlanner from '../../components/ResourcePlanner.jsx';
import UserLink from '../../components/UserLink.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { PROJECT_STATUS_LABEL, PROJECT_STATUS_TONE, locationLine } from '../../lib/format.js';

export default function ProjectDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const location = useLocation();
  // Set when an officer's approval just created this project.
  const [notice, setNotice] = React.useState(location.state?.notice || null);
  const state = useApi(() => api.projects.get(id), [id]);

  return (
    <Async state={state}>
      {(project) => {
        // Only the project owner records progress; officials follow it here.
        const canPost = project.status !== 'COMPLETED' && user?.id === project.owner?.id;

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)', maxWidth: 820 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <IconButton icon="chevron-left" label="Back to projects" onClick={() => navigate('/console/projects')} />
              <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-muted)' }}>{project.reference}</span>
              <Badge tone={PROJECT_STATUS_TONE[project.status]} style={{ marginLeft: 'auto' }}>{PROJECT_STATUS_LABEL[project.status]}</Badge>
            </div>

            {notice ? <Alert tone="success" title="Project created" onDismiss={() => setNotice(null)}>{notice}</Alert> : null}

            <div>
              <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>{project.title}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 4 }}>
                <Icon name="map-pin" size="sm" />
                {locationLine(project)}
                {project.owner ? (user?.id === project.owner.id ? ' · you are the project owner' : <>{' · project owner '}<UserLink user={project.owner} style={{ color: 'var(--text-link)', fontWeight: 600 }} /></>) : ''}
              </div>
              {project.description ? (
                <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)', marginTop: 8 }}>{project.description}</p>
              ) : null}
            </div>

            <ProjectStatusBar project={project} />

            {user?.role === 'AUTHORITY' && project.status !== 'COMPLETED' ? (
              <ResourcePlanner key={project.resources?.finalizedAt || 'draft'} project={project} onSaved={state.setData} />
            ) : (
              <>
                {project.approval ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: 'var(--space-4) var(--space-5)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)' }}>
                    <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                      Official comment · {project.approval.officer?.fullName}
                    </span>
                    <span style={{ font: 'var(--text-body)', color: 'var(--text-heading)', whiteSpace: 'pre-wrap' }}>{project.approval.comment}</span>
                  </div>
                ) : null}
                <ProjectResources project={project} />
              </>
            )}

            <ProjectProgress project={project} />

            <div>
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Before / after</span>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 'var(--space-3)', marginTop: 8 }}>
                <div>
                  <PhotoPlaceholder ratio="4/3" style={{ borderRadius: 'var(--radius-md)' }} />
                  <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>Before</span>
                </div>
                <div>
                  <PhotoPlaceholder ratio="4/3" style={{ borderRadius: 'var(--radius-md)' }} />
                  <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                    {project.status === 'COMPLETED' ? 'After' : 'Latest update'}
                  </span>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Progress timeline</span>
              <ProjectTimeline updates={project.updates} />
            </div>

            {canPost ? <ProgressUpdateForm projectId={project.id} onUpdated={state.setData} /> : null}

            <ProjectOrigin project={project} />
          </div>
        );
      }}
    </Async>
  );
}
