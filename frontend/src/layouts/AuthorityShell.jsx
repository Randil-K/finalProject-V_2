import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Icon, Avatar, Badge, Button } from '../design-system';
import { useAuth } from '../auth/AuthContext.jsx';
import { useUnreadAlerts } from '../hooks/useUnreadAlerts.js';
import { ROLE_LABEL } from '../lib/format.js';
import { mediaUrl } from '../api/client.js';

const NAV = [
  { to: '/console', label: 'Review queue', icon: 'list-filter', end: true },
  { to: '/console/projects', label: 'Projects', icon: 'map-pin' },
  { to: '/console/alerts', label: 'Alerts', icon: 'bell', showUnread: true },
  { to: '/console/analytics', label: 'Analytics', icon: 'chart-column' },
  { to: '/console/verifications', label: 'Verifications', icon: 'badge-check', adminOnly: true },
  { to: '/console/users', label: 'Users', icon: 'users', adminOnly: true },
];

export default function AuthorityShell() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const unread = useUnreadAlerts();

  function signOut() {
    // Leave the protected page before clearing the session.
    navigate('/', { replace: true });
    setTimeout(logout, 0);
  }

  return (
    <div style={{ minHeight: '100%', display: 'flex' }}>
      <aside
        style={{
          width: 'var(--sidebar-width)',
          flex: '0 0 auto',
          background: 'var(--surface-brand)',
          color: 'var(--text-inverse)',
          display: 'flex',
          flexDirection: 'column',
          padding: 'var(--space-5) var(--space-4)',
          gap: 'var(--space-8)',
        }}
      >
        <button onClick={() => navigate('/')} style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', padding: '0 var(--space-2)' }}>
          <Icon name="waves-horizontal" size="md" color="var(--tide-300)" />
          <span style={{ font: '700 18px/1 var(--font-display)', letterSpacing: '-0.03em', color: 'var(--white)' }}>Tideline</span>
        </button>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {NAV.filter((n) => !n.adminOnly || user?.role === 'ADMIN').map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                height: 40,
                padding: '0 var(--space-3)',
                borderRadius: 'var(--radius-md)',
                background: isActive ? 'rgba(255,255,255,0.12)' : 'transparent',
                color: isActive ? 'var(--white)' : 'var(--text-inverse-muted)',
                font: 'var(--text-label)',
                textDecoration: 'none',
              })}
            >
              <Icon name={n.icon} size="sm" />
              {n.label}
              {n.showUnread && unread ? (
                <Badge tone="accent" size="sm" style={{ marginLeft: 'auto' }} aria-label={`${unread} unread`}>{unread}</Badge>
              ) : null}
            </NavLink>
          ))}
        </nav>

        <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '0 var(--space-2)' }}>
            <Avatar name={user?.fullName || ''} src={mediaUrl(user?.avatarUrl)} role="authority" size="sm" />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 }}>
              <span style={{ font: 'var(--text-label)', color: 'var(--white)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user?.fullName}
              </span>
              <Badge tone="inverse" size="sm">{ROLE_LABEL[user?.role] || user?.role}</Badge>
            </div>
          </div>
          <button
            onClick={signOut}
            style={{ display: 'flex', alignItems: 'center', gap: 10, height: 40, padding: '0 var(--space-2)', color: 'var(--text-inverse-muted)', font: 'var(--text-label)', cursor: 'pointer' }}
          >
            <Icon name="log-out" size="sm" />
            Sign out
          </button>
        </div>
      </aside>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <header
          style={{
            height: 'var(--appbar-height)',
            flex: '0 0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 var(--space-6)',
            background: 'var(--surface-card)',
            borderBottom: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)', minWidth: 0 }}>
            <Button variant="secondary" size="sm" iconLeft="chevron-left" onClick={() => navigate('/app')}>
              Back to feed
            </Button>
            <span style={{ font: 'var(--text-h3)', color: 'var(--text-strong)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {user?.role === 'AUTHORITY' ? 'Government officer console' : 'Administrator console'}
            </span>
          </div>
          <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>Marine Environment Protection Authority</span>
        </header>
        <main style={{ flex: 1, padding: 'var(--space-6)', maxWidth: 1200, width: '100%', margin: '0 auto' }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
