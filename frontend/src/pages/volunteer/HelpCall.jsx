import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Badge, Button, Card, Icon, Input } from '../../design-system';
import MapLink from '../../components/MapLink.jsx';
import EvidenceGallery from '../../components/EvidenceGallery.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ALERTS_CHANGED } from '../../hooks/useUnreadAlerts.js';
import { SEVERITY_LABEL, SEVERITY_VAR, distanceKm, formatDate } from '../../lib/format.js';

function Fact({ icon, label, children }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: 'var(--space-3) var(--space-4)', borderRadius: 'var(--radius-md)', background: 'var(--surface-sunken)' }}>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6, font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
        <Icon name={icon} size="xs" />
        {label}
      </span>
      <span style={{ font: 'var(--text-body)', color: 'var(--text-strong)' }}>{children}</span>
    </div>
  );
}

/** What a call for help opens: everything about the cleanup, then Join or Ignore. */
export default function HelpCall() {
  const { alertId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const state = useApi(async () => {
    const alerts = await api.alerts.list();
    const call = alerts.find((a) => String(a.id) === String(alertId));
    if (!call) throw new Error('That call for help is no longer in your alerts.');
    return { call, project: await api.projects.get(call.projectId) };
  }, [alertId]);

  const [pledges, setPledges] = React.useState({});
  const [busy, setBusy] = React.useState(null);
  const [error, setError] = React.useState(null);

  async function answer(reply, equipment) {
    setError(null);
    setBusy(reply);
    try {
      await api.alerts.reply(alertId, reply, equipment);
      window.dispatchEvent(new Event(ALERTS_CHANGED));
      if (reply === 'JOINED') {
        navigate(`/app/cleanups/${state.data.project.id}`, {
          replace: true,
          state: { notice: 'You have joined this cleanup. Thank you.' },
        });
      } else {
        navigate('/app/alerts', { replace: true });
      }
    } catch (err) {
      setError(err.message);
      setBusy(null);
    }
  }

  return (
    <Async state={state}>
      {({ call, project }) => {
        const resources = project.resources;
        const volunteersNeeded = resources?.volunteersNeeded ?? 0;
        const diversNeeded = resources?.diversNeeded ?? 0;
        const shortfall = (resources?.equipment || []).filter((item) => item.securedQuantity < item.quantity);
        const away = distanceKm(user?.latitude, user?.longitude, project.latitude, project.longitude);
        const answered = Boolean(call.reply);
        const joinedSoFar = project.volunteerCount + project.diverCount;

        const equipmentFromPledges = () =>
          Object.entries(pledges)
            .map(([name, quantity]) => ({ name, quantity: Number(quantity) }))
            .filter((line) => line.quantity > 0);

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <Badge tone="neutral" size="sm">{project.reference}</Badge>
                {project.severity ? (
                  <Badge size="sm" style={{ background: `var(${SEVERITY_VAR[project.severity]}-bg)`, color: `var(${SEVERITY_VAR[project.severity]})` }}>
                    {SEVERITY_LABEL[project.severity]} severity
                  </Badge>
                ) : null}
                {answered ? (
                  <Badge tone={call.reply === 'JOINED' ? 'success' : 'neutral'} size="sm">
                    {call.reply === 'JOINED' ? 'You joined' : 'You passed on this'}
                  </Badge>
                ) : null}
              </div>
              <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>{project.title}</h1>
              <span style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
                {[project.locationName, project.province].filter(Boolean).join(' · ')}
              </span>
              <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)', marginTop: 4 }}>{project.description}</p>
              <MapLink latitude={project.latitude} longitude={project.longitude} label="See the site on the map" />
            </div>

            <EvidenceGallery report={project} />

            {error ? <Alert tone="danger" title="That didn't work" onDismiss={() => setError(null)}>{error}</Alert> : null}

            <Card padding="lg">
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>What you should know</span>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 'var(--space-3)', marginTop: 'var(--space-3)' }}>
                <Fact icon="map-pin" label="How far from you">
                  {away == null ? 'Add your location to see this' : `${away.toFixed(1)} km away`}
                </Fact>
                <Fact icon="users" label="Volunteers">
                  {project.volunteerCount} of {volunteersNeeded} joined
                </Fact>
                <Fact icon="anchor" label="Divers">
                  {project.diverCount} of {diversNeeded} joined
                </Fact>
                <Fact icon="calendar" label="Approved">{formatDate(project.createdAt, false)}</Fact>
              </div>
            </Card>

            <Card padding="lg">
              <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Equipment needed</span>
              {resources?.equipment?.length ? (
                <div style={{ display: 'flex', flexDirection: 'column', marginTop: 'var(--space-2)' }}>
                  {resources.equipment.map((item) => (
                    <div
                      key={item.name}
                      style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, padding: 'var(--space-2) 0', borderTop: '1px solid var(--border-subtle)', font: 'var(--text-body-sm)' }}
                    >
                      <span style={{ color: 'var(--text-heading)' }}>{item.name}</span>
                      <span style={{ font: '600 13px/1.5 var(--font-mono)', color: 'var(--text-strong)' }}>× {item.quantity}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 4 }}>
                  Nothing to bring — just yourself.
                </p>
              )}
            </Card>

            {answered ? (
              <Button variant="secondary" onClick={() => navigate(`/app/cleanups/${project.id}`)} style={{ alignSelf: 'flex-start' }}>
                Open the cleanup
              </Button>
            ) : (
              <Card padding="lg">
                <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Can you help?</span>
                <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
                  {joinedSoFar === 0
                    ? 'Nobody has joined yet — you would be the first.'
                    : `${joinedSoFar} ${joinedSoFar === 1 ? 'person has' : 'people have'} already joined.`}
                </p>

                {shortfall.length ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', marginTop: 'var(--space-3)' }}>
                    <span style={{ font: 'var(--text-body-sm)', color: 'var(--text-heading)' }}>Anything you can bring?</span>
                    {shortfall.map((item) => (
                      <div key={item.name} style={{ display: 'flex', alignItems: 'flex-end', gap: 'var(--space-3)' }}>
                        <span style={{ flex: 1, font: 'var(--text-body-sm)', color: 'var(--text-heading)' }}>
                          {item.name} — {item.quantity - item.securedQuantity} still needed
                        </span>
                        <Input
                          type="number"
                          min="0"
                          max={item.quantity - item.securedQuantity}
                          placeholder="0"
                          aria-label={`How many ${item.name} you can bring`}
                          value={pledges[item.name] ?? ''}
                          onChange={(e) => setPledges((p) => ({ ...p, [item.name]: e.target.value }))}
                          style={{ width: 110 }}
                        />
                      </div>
                    ))}
                  </div>
                ) : null}

                <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', marginTop: 'var(--space-4)' }}>
                  <Button
                    iconLeft="hand-heart"
                    disabled={Boolean(busy)}
                    onClick={() => answer('JOINED', equipmentFromPledges())}
                  >
                    {busy === 'JOINED' ? 'Joining…' : 'Join this cleanup'}
                  </Button>
                  <Button variant="secondary" disabled={Boolean(busy)} onClick={() => answer('IGNORED')}>
                    {busy === 'IGNORED' ? 'Saving…' : 'Ignore'}
                  </Button>
                </div>
              </Card>
            )}
          </div>
        );
      }}
    </Async>
  );
}
