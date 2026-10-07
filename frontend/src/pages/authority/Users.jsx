import React from 'react';
import { Badge, Button, Input, Select, Textarea, Alert, Avatar } from '../../design-system';
import Modal from '../../components/Modal.jsx';
import { Async } from '../../components/AsyncState.jsx';
import { api } from '../../api/index.js';
import { useApi } from '../../hooks/useApi.js';
import { ACCOUNT_STATUS, PROVINCES, ROLE_LABEL, formatDate } from '../../lib/format.js';
import { mediaUrl } from '../../api/client.js';

const COLUMNS = 'minmax(240px, 2fr) 150px 170px 100px 110px 120px';

export default function Users() {
  const [search, setSearch] = React.useState('');
  const [query, setQuery] = React.useState('');
  const [role, setRole] = React.useState('');
  const [province, setProvince] = React.useState('');
  const state = useApi(() => api.admin.users(query), [query]);

  const roleOptions = Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label }));
  const provinceOptions = PROVINCES.map((p) => ({ value: p, label: p }));
  const filtered = (list) => list.filter((user) =>
    (!role || user.role === role) && (!province || user.province === province));
  const filtersActive = Boolean(role || province);

  const [target, setTarget] = React.useState(null);
  const [reason, setReason] = React.useState('');
  const [busyId, setBusyId] = React.useState(null);
  const [error, setError] = React.useState(null);
  const [notice, setNotice] = React.useState(null);

  async function update(user, suspended) {
    setBusyId(user.id);
    setError(null);
    setNotice(null);
    try {
      await api.admin.setSuspension(user.id, suspended, suspended ? reason.trim() : null);
      setNotice(suspended ? `${user.fullName} is suspended and can no longer sign in.` : `${user.fullName} can sign in again.`);
      setTarget(null);
      setReason('');
      state.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
      <div>
        <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Users</h1>
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 2 }}>
          Suspend accounts that post false reports or abuse the community. Suspended members can't sign in.
        </p>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          setQuery(search.trim());
        }}
        style={{ display: 'flex', gap: 8, maxWidth: 480, alignItems: 'flex-end' }}
      >
        <Input iconLeft="search" placeholder="Search by name or email" value={search} onChange={(e) => setSearch(e.target.value)} style={{ flex: 1 }} />
        <Button type="submit" variant="secondary">Search</Button>
      </form>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <Select
          placeholder="All positions"
          options={roleOptions}
          value={role}
          onChange={(e) => setRole(e.target.value)}
          style={{ minWidth: 180 }}
        />
        <Select
          placeholder="All locations"
          options={provinceOptions}
          value={province}
          onChange={(e) => setProvince(e.target.value)}
          style={{ minWidth: 200 }}
        />
        {filtersActive ? (
          <Button variant="ghost" size="sm" onClick={() => { setRole(''); setProvince(''); }}>
            Clear filters
          </Button>
        ) : null}
      </div>

      {notice ? <Alert tone="success" title="Done" onDismiss={() => setNotice(null)}>{notice}</Alert> : null}
      {error && !target ? <Alert tone="danger" title="That didn't work">{error}</Alert> : null}

      <Async
        state={state}
        isEmpty={(list) => !filtered(list).length}
        empty={filtersActive ? 'No accounts match that search and filter.' : 'No accounts match that search.'}
        emptyIcon="users"
      >
        {(list) => {
          const users = filtered(list);
          return (
          <div style={{ overflowX: 'auto', background: 'var(--surface-card)', border: '1px solid var(--border-subtle)', borderRadius: 'var(--radius-lg)' }}>
            <div style={{ minWidth: 890 }}>
              <div style={{ display: 'grid', gridTemplateColumns: COLUMNS, padding: '10px var(--space-4)', background: 'var(--surface-sunken)', font: 'var(--text-micro)', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 'var(--tracking-micro)' }}>
                <span>Member</span>
                <span>Role</span>
                <span>Location</span>
                <span>Joined</span>
                <span>Status</span>
                <span />
              </div>
              {users.map((user) => (
                <div key={user.id} style={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', padding: 'var(--space-3) var(--space-4)', borderTop: '1px solid var(--border-subtle)', font: 'var(--text-body-sm)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
                    <Avatar name={user.fullName} src={mediaUrl(user.avatarUrl)} size="sm" />
                    <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                      <span style={{ color: 'var(--text-heading)', font: 'var(--text-label)' }}>{user.fullName}</span>
                      <span style={{ color: 'var(--text-muted)', font: 'var(--text-caption)', overflow: 'hidden', textOverflow: 'ellipsis' }}>{user.email}</span>
                    </div>
                  </div>
                  <span><Badge tone="neutral" size="sm">{ROLE_LABEL[user.role] || user.role}</Badge></span>
                  <span style={{ color: 'var(--text-muted)' }}>{[user.city, user.province].filter(Boolean).join(', ') || '—'}</span>
                  <span style={{ color: 'var(--text-muted)', font: 'var(--text-caption)' }}>{formatDate(user.createdAt, false)}</span>
                  <span title={user.suspensionReason || undefined}>
                    {user.suspended ? (
                      <Badge tone="danger" size="sm">Suspended</Badge>
                    ) : user.accountStatus !== 'APPROVED' ? (
                      <Badge tone={ACCOUNT_STATUS[user.accountStatus].tone} size="sm">{ACCOUNT_STATUS[user.accountStatus].label}</Badge>
                    ) : (
                      <Badge tone="success" size="sm">Active</Badge>
                    )}
                  </span>
                  <span style={{ justifySelf: 'end' }}>
                    {user.role === 'ADMIN' ? null : user.suspended ? (
                      <Button size="sm" variant="secondary" disabled={busyId === user.id} onClick={() => update(user, false)}>Reinstate</Button>
                    ) : (
                      <Button size="sm" variant="ghost" disabled={busyId === user.id} onClick={() => { setError(null); setReason(''); setTarget(user); }}>Suspend</Button>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
          );
        }}
      </Async>

      <Modal
        open={Boolean(target)}
        title={`Suspend ${target?.fullName}?`}
        description="They won't be able to sign in until an administrator reinstates them. Their reports stay visible."
        onClose={() => setTarget(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setTarget(null)}>Cancel</Button>
            <Button variant="danger" disabled={!reason.trim() || busyId === target?.id} onClick={() => update(target, true)}>
              Suspend account
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          {error ? <Alert tone="danger" title="Could not suspend">{error}</Alert> : null}
          <Textarea
            label="Reason"
            required
            rows={3}
            placeholder="e.g. Repeated false reports at Negombo."
            hint="Recorded so other administrators can see why."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  );
}
