import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Badge, Button, Input, Select, Switch, Tag, Alert } from '../../design-system';
import ProfileOverview, { DetailRow, DetailSection } from '../../components/ProfileOverview.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { ACCOUNT_STATUS, CERTIFICATION_OPTIONS, PROVINCES } from '../../lib/format.js';
import AvatarEditor from '../../components/AvatarEditor.jsx';

function formFrom(user) {
  return {
    fullName: user?.fullName || '',
    phone: user?.phone || '',
    province: user?.province || '',
    city: user?.city || '',
    latitude: user?.latitude ?? '',
    longitude: user?.longitude ?? '',
  };
}

function diverFrom(user) {
  return {
    certificationLevel: user?.diverProfile?.certificationLevel || '',
    experienceYears: user?.diverProfile?.experienceYears ?? '',
    equipment: user?.diverProfile?.equipment || '',
  };
}

export default function Profile() {
  const navigate = useNavigate();
  const { user, setUser, logout } = useAuth();
  const profileState = useApi(() => (user ? api.users.profile(user.id) : Promise.resolve(null)), [user]);

  const [editing, setEditing] = React.useState(false);
  const [form, setForm] = React.useState(() => formFrom(user));
  const [diver, setDiver] = React.useState(() => diverFrom(user));
  const [regions, setRegions] = React.useState(user?.diverProfile?.preferredRegions || []);
  const [available, setAvailable] = React.useState(user?.availableForAlerts ?? true);
  const [status, setStatus] = React.useState(null);
  const [busy, setBusy] = React.useState(false);
  const [locating, setLocating] = React.useState(false);

  const isDiver = user?.role === 'DIVER';
  const hasLocation = user?.latitude != null && user?.longitude != null;
  const remainingRegions = PROVINCES.filter((p) => !regions.includes(p));

  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }));
  const setDiverField = (key) => (event) => setDiver((d) => ({ ...d, [key]: event.target.value }));

  function startEditing() {
    setForm(formFrom(user));
    setDiver(diverFrom(user));
    setRegions(user?.diverProfile?.preferredRegions || []);
    setAvailable(user?.availableForAlerts ?? true);
    setStatus(null);
    setEditing(true);
  }

  function captureLocation() {
    setStatus(null);
    if (!navigator.geolocation) {
      setStatus({ tone: 'warning', title: 'Location not saved', message: 'This browser cannot share your location.' });
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const updated = await api.users.updateProfile({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
          setUser(updated);
          setForm((f) => ({ ...f, latitude: updated.latitude, longitude: updated.longitude }));
          setStatus({ tone: 'success', title: 'Location saved', message: "You'll get alerts for reports and cleanups within 5 km." });
        } catch (error) {
          setStatus({ tone: 'danger', title: 'Location not saved', message: error.message });
        } finally {
          setLocating(false);
        }
      },
      () => {
        setLocating(false);
        setStatus({ tone: 'warning', title: 'Location not saved', message: 'Location access was blocked. Allow it in your browser settings and try again.' });
      },
      { timeout: 8000 },
    );
  }

  async function save() {
    setBusy(true);
    setStatus(null);
    try {
      let updated = await api.users.updateProfile({
        ...form,
        latitude: form.latitude === '' ? null : Number(form.latitude),
        longitude: form.longitude === '' ? null : Number(form.longitude),
        availableForAlerts: available,
      });
      if (isDiver) {
        updated = await api.users.updateDiverProfile({
          certificationLevel: diver.certificationLevel || null,
          experienceYears: diver.experienceYears === '' ? null : Number(diver.experienceYears),
          equipment: diver.equipment || null,
          preferredRegions: regions,
        });
      }
      setUser(updated);
      setEditing(false);
      setStatus({ tone: 'success', title: 'Saved', message: 'Your profile is up to date.' });
    } catch (error) {
      setStatus({ tone: 'danger', title: "That didn't save", message: error.message });
    } finally {
      setBusy(false);
    }
  }

  function signOut() {
    // Leave the protected page before clearing the session.
    navigate('/', { replace: true });
    setTimeout(logout, 0);
  }

  if (!user) return null;

  const statusAlert = status ? (
    <Alert tone={status.tone} title={status.title} onDismiss={() => setStatus(null)}>{status.message}</Alert>
  ) : null;

  if (!editing) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
        {statusAlert}
        <Async state={profileState}>
          {(profile) => (
            <ProfileOverview
              profile={profile}
              actions={<Button variant="secondary" iconLeft="settings" onClick={startEditing}>Edit profile</Button>}
              avatar={
                <AvatarEditor
                  user={user}
                  name={user.role === 'ORGANIZATION' && user.organizationName ? user.organizationName : user.fullName}
                  role={isDiver ? 'diver' : undefined}
                  onChange={(updated) => {
                    setUser(updated);
                    setStatus(null);
                  }}
                />
              }
            >
              <DetailSection title="Account">
                <DetailRow icon="user" label="Email">{user.email}</DetailRow>
                <DetailRow icon="message-square" label="Phone">{user.phone || '—'}</DetailRow>
                {isDiver || user.role === 'ORGANIZATION' ? (
                  <DetailRow icon="shield-check" label="Account status">
                    <Badge tone={ACCOUNT_STATUS[user.accountStatus]?.tone} size="sm">{ACCOUNT_STATUS[user.accountStatus]?.label}</Badge>
                  </DetailRow>
                ) : null}
                {isDiver ? <DetailRow icon="life-buoy" label="Equipment">{user.diverProfile?.equipment || '—'}</DetailRow> : null}
                <DetailRow icon="map-pin" label="Alert location">
                  {hasLocation ? `${user.latitude.toFixed(4)}° N, ${user.longitude.toFixed(4)}° E` : 'Not set'}
                </DetailRow>
                <DetailRow icon="bell" label="Cleanup alerts">{user.availableForAlerts ? 'On' : 'Off'}</DetailRow>
              </DetailSection>
            </ProfileOverview>
          )}
        </Async>

        <Button variant="secondary" iconLeft="log-out" onClick={signOut} style={{ alignSelf: 'flex-start' }}>
          Sign out
        </Button>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
      <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Edit profile</h1>

      {statusAlert}

      <section style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Contact details</span>
        <Input label="Full name" value={form.fullName} onChange={set('fullName')} />
        <Input label="Email" type="email" value={user.email} disabled />
        <Input label="Phone" type="tel" value={form.phone} onChange={set('phone')} />
      </section>

      <section style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Location</span>
        <Input label="City or town" iconLeft="map-pin" placeholder="e.g. Negombo" value={form.city} onChange={set('city')} />
        <Select label="Province" placeholder="Select your province" options={PROVINCES} value={form.province} onChange={set('province')} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: 'var(--space-4)', borderRadius: 'var(--radius-md)', background: 'var(--surface-sunken)' }}>
          <Button variant="secondary" size="sm" iconLeft="map-pin" onClick={captureLocation} disabled={locating} style={{ alignSelf: 'flex-start' }}>
            {locating ? 'Locating…' : hasLocation ? 'Update to my current location' : 'Use my current location'}
          </Button>
          <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
            <Input
              label="Latitude"
              type="number"
              step="0.0001"
              placeholder="6.9271"
              value={form.latitude}
              onChange={set('latitude')}
              style={{ flex: 1, minWidth: 140 }}
            />
            <Input
              label="Longitude"
              type="number"
              step="0.0001"
              placeholder="79.8612"
              value={form.longitude}
              onChange={set('longitude')}
              style={{ flex: 1, minWidth: 140 }}
            />
          </div>
        </div>
      </section>

      <section style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Alerts</span>
        <div style={{ padding: 'var(--space-4)', borderRadius: 'var(--radius-md)', background: 'var(--surface-sunken)' }}>
          <Switch label="Available for cleanup alerts" checked={available} onChange={(e) => setAvailable(e.target.checked)} />
        </div>
      </section>

      {isDiver ? (
        <section style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Diving profile</span>
          <Select label="Certification level" placeholder="Select certification" options={CERTIFICATION_OPTIONS} value={diver.certificationLevel} onChange={setDiverField('certificationLevel')} />
          <Input label="Experience" type="number" min="0" suffix="years" value={diver.experienceYears} onChange={setDiverField('experienceYears')} />
          <Input label="Equipment available" placeholder="e.g. Own BCD, regulator, wetsuit" value={diver.equipment} onChange={setDiverField('equipment')} />

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Preferred working regions</span>
            {regions.length ? (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                {regions.map((region) => (
                  <Tag key={region} icon="map-pin" onRemove={() => setRegions((rs) => rs.filter((r) => r !== region))}>
                    {region}
                  </Tag>
                ))}
              </div>
            ) : null}
            {remainingRegions.length ? (
              <Select
                placeholder="Add a region"
                options={remainingRegions}
                value=""
                onChange={(e) => {
                  const region = e.target.value;
                  if (region) setRegions((rs) => [...rs, region]);
                }}
              />
            ) : null}
          </div>
        </section>
      ) : null}

      <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
        <Button fullWidth onClick={save} loading={busy} disabled={busy}>
          {busy ? 'Saving…' : 'Save changes'}
        </Button>
        <Button variant="secondary" onClick={() => { setStatus(null); setEditing(false); }} disabled={busy}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
