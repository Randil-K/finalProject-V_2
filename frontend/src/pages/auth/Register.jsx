import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Icon, Card, Button, Input, Select, Field, Alert, Badge } from '../../design-system';
import PasswordInput from '../../components/PasswordInput.jsx';
import ChipButton from '../../components/ChipButton.jsx';
import DocumentPicker from '../../components/DocumentPicker.jsx';
import { useAuth } from '../../auth/AuthContext.jsx';
import { CERTIFICATION_OPTIONS, PROVINCES } from '../../lib/format.js';

const ROLES = [
  { value: 'CITIZEN', label: 'Community member', icon: 'user' },
  { value: 'DIVER', label: 'Volunteer diver', icon: 'anchor' },
  { value: 'ORGANIZATION', label: 'Organisation', icon: 'building-2' },
  { value: 'AUTHORITY', label: 'Government officer', icon: 'shield-check' },
  { value: 'ADMIN', label: 'Administrator', icon: 'settings' },
];

const OFFICIAL_ROLES = ['AUTHORITY', 'ADMIN'];
const NEEDS_DOCUMENTS = ['DIVER', ...OFFICIAL_ROLES];

const ORGANIZATION_TYPES = [
  { value: 'NGO', label: 'NGO' },
  { value: 'TOURISM', label: 'Tourism operator' },
  { value: 'MARINE_INSTITUTION', label: 'Marine institution' },
];

const withScheme = (value) => (/^https?:\/\//i.test(value) ? value : `https://${value}`);

function Shell({ children, width = 460 }) {
  return (
    <div style={{ minHeight: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--surface-page-warm)', padding: 'var(--space-6)' }}>
      <div style={{ width: '100%', maxWidth: width }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, justifyContent: 'center', marginBottom: 'var(--space-8)' }}>
          <Icon name="waves-horizontal" size="lg" color="var(--tide-600)" />
          <span style={{ font: '700 20px/1 var(--font-display)', letterSpacing: '-0.03em', color: 'var(--text-strong)' }}>Tideline</span>
        </div>
        {children}
      </div>
    </div>
  );
}

function PendingConfirmation({ account }) {
  const navigate = useNavigate();
  const isDiver = account.role === 'DIVER';
  const isOfficial = OFFICIAL_ROLES.includes(account.role);
  return (
    <Shell width={440}>
      <Card padding="lg">
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--space-4)', textAlign: 'center' }}>
          <span style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--status-verified-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Icon name="badge-check" size="xl" color="var(--status-verified)" />
          </span>
          <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Your account has been created</h1>
          <Badge tone="warning" icon="clock">Pending verification</Badge>
          <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)' }}>
            {isDiver
              ? 'An administrator will check your certificates before you can sign in.'
              : isOfficial
                ? 'An existing administrator will confirm your appointment before you can sign in.'
                : "An administrator will check your organisation's website before you can sign in and post opportunities."}
          </p>
          <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
            Try signing in with {account.email} once you've been verified. If your application isn't approved, you'll see the reason there.
          </p>
          <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', justifyContent: 'center' }}>
            <Button onClick={() => navigate('/')}>Back to home</Button>
            <Button variant="secondary" onClick={() => navigate('/app')}>Browse reports</Button>
          </div>
        </div>
      </Card>
    </Shell>
  );
}

export default function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [role, setRole] = React.useState('DIVER');
  const [form, setForm] = React.useState({
    fullName: '',
    email: '',
    phone: '',
    province: '',
    password: '',
    certificationLevel: '',
    organizationName: '',
    organizationType: '',
    websiteUrl: '',
  });
  const [certificates, setCertificates] = React.useState([]);
  const [licences, setLicences] = React.useState([]);
  const [error, setError] = React.useState(null);
  const [busy, setBusy] = React.useState(false);
  const [pending, setPending] = React.useState(null);

  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }));

  async function submit(event) {
    event.preventDefault();
    setError(null);

    if (role === 'DIVER' && !certificates.length) {
      setError('Attach at least one diving certificate so an administrator can verify you.');
      return;
    }
    if (OFFICIAL_ROLES.includes(role) && !form.organizationName.trim()) {
      setError('Add the department or agency you work for.');
      return;
    }
    if (OFFICIAL_ROLES.includes(role) && !certificates.length) {
      setError('Attach proof of your appointment, such as a staff ID or appointment letter.');
      return;
    }
    if (role === 'ORGANIZATION' && !form.websiteUrl.trim()) {
      setError("Add your organisation's website so an administrator can check it.");
      return;
    }

    setBusy(true);
    try {
      const result = await register(
        {
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          phone: form.phone || null,
          role,
          province: form.province || null,
          certificationLevel: role === 'DIVER' ? form.certificationLevel || null : null,
          organizationName: role === 'ORGANIZATION' || OFFICIAL_ROLES.includes(role) ? form.organizationName.trim() || null : null,
          organizationType: role === 'ORGANIZATION' ? form.organizationType || null : null,
          websiteUrl: role === 'ORGANIZATION' ? withScheme(form.websiteUrl.trim()) : null,
        },
        NEEDS_DOCUMENTS.includes(role) ? certificates : [],
        role === 'DIVER' ? licences : [],
      );
      if (result.token) {
        navigate('/app', { replace: true });
      } else {
        setPending(result.user);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (pending) return <PendingConfirmation account={pending} />;

  return (
    <Shell>
      <Card padding="lg">
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <div>
            <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Create your account</h1>
            <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 4 }}>
              Join citizens, divers and organisations protecting the coast.
            </p>
          </div>

          <Field label="I am registering as">
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {ROLES.map((r) => (
                <ChipButton key={r.value} icon={r.icon} selected={role === r.value} onClick={() => setRole(r.value)}>
                  {r.label}
                </ChipButton>
              ))}
            </div>
          </Field>

          {role !== 'CITIZEN' ? (
            <Alert tone="info" title="Accounts are verified by an administrator">
              {role === 'DIVER'
                ? 'Attach your diving certificates. You can sign in once an administrator has checked them.'
                : OFFICIAL_ROLES.includes(role)
                  ? 'Attach proof of your appointment. You can sign in once an existing administrator has approved you.'
                  : "Add your organisation's website. You can sign in once an administrator has checked it."}
            </Alert>
          ) : null}

          <Input label="Full name" placeholder="e.g. Hiruna Perera" required value={form.fullName} onChange={set('fullName')} />
          <Input label="Email" type="email" placeholder="you@example.lk" required value={form.email} onChange={set('email')} />
          <Input label="Phone" type="tel" placeholder="+94 7X XXX XXXX" value={form.phone} onChange={set('phone')} />
          <Select label="Province" placeholder="Select your province" options={PROVINCES} value={form.province} onChange={set('province')} />

          {role === 'DIVER' ? (
            <>
              <Select
                label="Certification level"
                placeholder="Select certification"
                options={CERTIFICATION_OPTIONS}
                value={form.certificationLevel}
                onChange={set('certificationLevel')}
              />
              <DocumentPicker
                label="Certificates"
                actionLabel="Add certificate files"
                required
                hint="Your diving certification card or logbook page."
                files={certificates}
                onChange={setCertificates}
              />
              <DocumentPicker
                label="Licences"
                actionLabel="Add licence files"
                hint="Any diving, boat or safety licence you hold."
                files={licences}
                onChange={setLicences}
              />
            </>
          ) : null}

          {OFFICIAL_ROLES.includes(role) ? (
            <>
              <Input
                label="Department or agency"
                required
                placeholder={role === 'AUTHORITY' ? 'e.g. Marine Environment Protection Authority' : 'e.g. Tideline operations team'}
                value={form.organizationName}
                onChange={set('organizationName')}
              />
              <DocumentPicker
                label="Proof of appointment"
                actionLabel="Add proof of appointment"
                required
                hint="Staff ID card or appointment letter."
                files={certificates}
                onChange={setCertificates}
              />
            </>
          ) : null}

          {role === 'ORGANIZATION' ? (
            <>
              <Input label="Organisation name" required placeholder="e.g. Blue Resurgence NGO" value={form.organizationName} onChange={set('organizationName')} />
              <Select label="Organisation type" placeholder="Select type" options={ORGANIZATION_TYPES} value={form.organizationType} onChange={set('organizationType')} />
              <Input
                label="Website"
                type="url"
                required
                iconLeft="arrow-up-right"
                placeholder="https://your-organisation.org"
                hint="Your official website or public page, so an administrator can confirm who you are."
                value={form.websiteUrl}
                onChange={set('websiteUrl')}
                onBlur={() => form.websiteUrl.trim() && setForm((f) => ({ ...f, websiteUrl: withScheme(f.websiteUrl.trim()) }))}
              />
            </>
          ) : null}

          <PasswordInput
            label="Password"
            placeholder="At least 8 characters"
            required
            minLength={8}
            value={form.password}
            onChange={set('password')}
          />

          {error ? <Alert tone="danger" title="Could not create your account">{error}</Alert> : null}

          <Button type="submit" fullWidth size="lg" loading={busy} disabled={busy}>
            {busy ? 'Creating your account…' : 'Create account'}
          </Button>

          <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', textAlign: 'center' }}>
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </form>
      </Card>
    </Shell>
  );
}
