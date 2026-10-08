import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Icon, Card, Badge, Button } from '../design-system';
import { api } from '../api/index.js';
import { useApi } from '../hooks/useApi.js';
import { ALERTS_CHANGED } from '../hooks/useUnreadAlerts.js';
import { Async } from './AsyncState.jsx';
import { plural, timeAgo } from '../lib/format.js';

const KIND = {
  NEW_REPORT_NEARBY: { icon: 'map-pin', color: 'var(--tide-600)' },
  REPORT_VERIFIED: { icon: 'badge-check', color: 'var(--status-verified)' },
  ALERT_ESCALATED: { icon: 'flag', color: 'var(--status-escalated)' },
  PROJECT_PLANNED: { icon: 'hand-heart', color: 'var(--tide-600)' },
  PROJECT_UPDATE: { icon: 'check-check', color: 'var(--status-verified)' },
  AUTHORITY_DECISION: { icon: 'shield-check', color: 'var(--sea-600)' },
  OPPORTUNITY: { icon: 'anchor', color: 'var(--buoy-600)' },
  ACCOUNT_REVIEW: { icon: 'user', color: 'var(--buoy-600)' },
  ACCOUNT_APPLICATION: { icon: 'badge-check', color: 'var(--buoy-600)' },
  HELP_NEEDED: { icon: 'hand-heart', color: 'var(--tide-600)' },
  RESOURCES_GATHERED: { icon: 'users', color: 'var(--status-verified)' },
  COMMENT_REPLY: { icon: 'message-square', color: 'var(--tide-600)' },
  INFO_REQUESTED: { icon: 'triangle-alert', color: 'var(--danger)' },
  INFO_RESPONSE: { icon: 'message-square', color: 'var(--sea-600)' },
  RESOURCES_NEEDED: { icon: 'triangle-alert', color: 'var(--danger)' },
  RESOURCES_ASSIGNED: { icon: 'users', color: 'var(--status-verified)' },
};

/** Shared by the volunteer app and the console; `linkFor` maps an alert to the screen it opens. */
export default function AlertList({ linkFor, empty }) {
  const navigate = useNavigate();
  const state = useApi(() => api.alerts.list(), []);
  const [marking, setMarking] = React.useState(false);

  function changed() {
    state.reload();
    window.dispatchEvent(new Event(ALERTS_CHANGED));
  }

  async function open(alert) {
    if (!alert.read) {
      try {
        await api.alerts.markRead(alert.id);
        changed();
      } catch {
        /* marking read is best-effort */
      }
    }
    const target = linkFor(alert);
    if (target) navigate(target);
  }

  async function markAll() {
    setMarking(true);
    try {
      await api.alerts.markAllRead();
      changed();
    } finally {
      setMarking(false);
    }
  }

  const unread = (state.data || []).filter((alert) => !alert.read).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {unread ? (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
          <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>{plural(unread, 'unread alert')}</span>
          <Button variant="ghost" size="sm" iconLeft="check-check" disabled={marking} onClick={markAll}>
            Mark all as read
          </Button>
        </div>
      ) : null}

      <Async state={state} isEmpty={(list) => !list?.length} empty={empty} emptyIcon="bell">
        {(alerts) => (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {alerts.map((alert) => {
              const kind = KIND[alert.type] || KIND.NEW_REPORT_NEARBY;
              const target = linkFor(alert);
              // Critical alerts need the recipient to act, so they stay red until answered.
              const urgent = alert.critical && !alert.read;
              return (
                <Card
                  key={alert.id}
                  padding="md"
                  tone={alert.read ? 'default' : 'accent'}
                  interactive={Boolean(target)}
                  onClick={() => open(alert)}
                  style={{
                    cursor: target || !alert.read ? 'pointer' : 'default',
                    ...(urgent ? { background: 'var(--danger-bg)', border: '1px solid var(--danger)' } : null),
                  }}
                >
                  <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                    <span style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--surface-card)', display: 'flex', alignItems: 'center', justifyContent: 'center', flex: '0 0 auto' }}>
                      <Icon name={kind.icon} size="sm" color={kind.color} />
                    </span>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>{alert.title}</span>
                        {urgent ? (
                          <Badge tone="danger" dot size="sm">Action needed</Badge>
                        ) : !alert.read ? (
                          <Badge tone="accent" dot size="sm">New</Badge>
                        ) : null}
                      </div>
                      <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-body-color)' }}>{alert.body}</p>
                      <span style={{ font: 'var(--text-micro)', color: 'var(--text-muted)' }}>
                        {timeAgo(alert.createdAt)}
                        {alert.radiusKm ? ` · within ${alert.radiusKm} km` : ''}
                        {alert.reply === 'JOINED' ? ' · you joined' : alert.reply === 'IGNORED' ? ' · you passed on this' : ''}
                        {alert.type === 'HELP_NEEDED' && !alert.reply ? ' · open to join or ignore' : ''}
                      </span>
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </Async>
    </div>
  );
}
