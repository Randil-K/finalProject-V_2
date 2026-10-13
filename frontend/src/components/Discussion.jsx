import React from 'react';
import { Link } from 'react-router-dom';
import { Alert, Avatar, Badge, Button, Icon, Textarea } from '../design-system';
import { Async } from './AsyncState.jsx';
import { api } from '../api/index.js';
import { useApi } from '../hooks/useApi.js';
import { formatDate, timeAgo } from '../lib/format.js';
import UserLink from './UserLink.jsx';
import { mediaUrl } from '../api/client.js';

const REACTIONS = [
  { type: 'LIKE', label: 'Like', icon: 'thumbs-up', color: 'var(--tide-600)', countKey: 'likeCount' },
  { type: 'HEART', label: 'Love', icon: 'heart', color: 'var(--danger)', countKey: 'heartCount' },
];

function actionStyle(active, color) {
  return {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
    padding: '2px 6px',
    marginLeft: -6,
    borderRadius: 'var(--radius-sm)',
    font: 'var(--text-caption)',
    fontWeight: 600,
    color: active ? color : 'var(--text-muted)',
    cursor: 'pointer',
  };
}

function Comment({ comment, user, onReact, onReply, reacting, small }) {
  const mine = user?.id === comment.author?.id;
  return (
    <div style={{ display: 'flex', gap: 10 }}>
      <UserLink user={comment.author} style={{ flex: '0 0 auto', height: 'fit-content' }}>
        <Avatar name={comment.author?.fullName || ''} src={mediaUrl(comment.author?.avatarUrl)} size={small ? 'xs' : 'sm'} role={comment.official ? 'authority' : undefined} />
      </UserLink>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0, flex: 1 }}>
        <div style={{ alignSelf: 'flex-start', maxWidth: '100%', padding: '8px 12px', borderRadius: 'var(--radius-lg)', background: 'var(--surface-sunken)' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 8, font: 'var(--text-label)', color: 'var(--text-heading)' }}>
            <UserLink user={comment.author}>{mine ? 'You' : comment.author?.fullName}</UserLink>
            {comment.official ? <Badge tone="info" size="sm">Official</Badge> : null}
          </span>
          <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-body-color)', whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{comment.body}</p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', paddingLeft: 12 }}>
          <time dateTime={comment.createdAt} title={formatDate(comment.createdAt)} style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
            {timeAgo(comment.createdAt)}
          </time>
          {REACTIONS.map((reaction) => {
            const active = comment.myReaction === reaction.type;
            const count = comment[reaction.countKey];
            return (
              <button
                key={reaction.type}
                type="button"
                aria-pressed={active}
                disabled={reacting}
                onClick={() => onReact(comment, reaction.type)}
                style={actionStyle(active, reaction.color)}
              >
                <Icon name={reaction.icon} size="xs" color={active ? reaction.color : 'currentColor'} />
                {reaction.label}
                {count ? <span style={{ fontWeight: 400 }}>{count}</span> : null}
              </button>
            );
          })}
          {onReply ? (
            <button type="button" onClick={() => onReply(comment)} style={actionStyle(false)}>
              <Icon name="message-square" size="xs" />
              Reply
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function ReplyBox({ target, user, onCancel, onPosted, reportId }) {
  const [body, setBody] = React.useState('');
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState(null);
  const name = user?.id === target.author?.id ? 'yourself' : target.author?.fullName;

  async function post() {
    setBusy(true);
    setError(null);
    try {
      await api.reports.comment(reportId, body, target.id);
      setBody('');
      onPosted();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, paddingLeft: 42 }}>
      {error ? <Alert tone="danger" title="Could not reply">{error}</Alert> : null}
      <Textarea
        autoFocus
        rows={2}
        maxLength={1000}
        placeholder={`Reply to ${name}…`}
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button size="sm" variant="ghost" onClick={onCancel}>Cancel</Button>
        <Button size="sm" disabled={!body.trim() || busy} onClick={post}>{busy ? 'Replying…' : 'Reply'}</Button>
      </div>
    </div>
  );
}

/** A report's discussion: comments with times, like and love reactions, and one level of replies. */
export default function Discussion({ reportId, user, loginFrom, closed = false }) {
  const state = useApi(() => (user ? api.reports.comments(reportId) : Promise.resolve([])), [reportId, user?.id]);
  const [body, setBody] = React.useState('');
  const [posting, setPosting] = React.useState(false);
  const [replyTo, setReplyTo] = React.useState(null);
  const [reactingId, setReactingId] = React.useState(null);
  const [error, setError] = React.useState(null);

  async function post() {
    setPosting(true);
    setError(null);
    try {
      await api.reports.comment(reportId, body);
      setBody('');
      state.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setPosting(false);
    }
  }

  async function react(comment, type) {
    setReactingId(comment.id);
    setError(null);
    try {
      const updated = await api.reports.react(reportId, comment.id, type);
      state.setData((state.data || []).map((c) => (c.id === updated.id ? updated : c)));
    } catch (err) {
      setError(err.message);
    } finally {
      setReactingId(null);
    }
  }

  if (!user) {
    return (
      <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)' }}>
        <Link to="/login" state={{ from: loginFrom }}>Sign in</Link> to read and join the discussion.
      </p>
    );
  }

  // Replies always belong to a top-level comment, so the box opens under that thread.
  const threadOf = (comment) => comment.parentId ?? comment.id;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {error ? <Alert tone="danger" title="That didn't work" onDismiss={() => setError(null)}>{error}</Alert> : null}

      <Async state={state} isEmpty={(list) => !list?.length} empty="No comments yet — be the first to add context." emptyIcon="message-square">
        {(comments) => {
          const topLevel = comments.filter((c) => !c.parentId);
          const repliesFor = (id) => comments.filter((c) => c.parentId === id);
          const shared = { user, onReact: react, onReply: closed ? null : setReplyTo };
          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
              {topLevel.map((comment) => (
                <div key={comment.id} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                  <Comment comment={comment} reacting={reactingId === comment.id} {...shared} />
                  {repliesFor(comment.id).length || (replyTo && threadOf(replyTo) === comment.id) ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', marginLeft: 42 }}>
                      {repliesFor(comment.id).map((reply) => (
                        <Comment key={reply.id} comment={reply} reacting={reactingId === reply.id} small {...shared} />
                      ))}
                    </div>
                  ) : null}
                  {replyTo && threadOf(replyTo) === comment.id ? (
                    <ReplyBox
                      key={replyTo.id}
                      target={replyTo}
                      user={user}
                      reportId={reportId}
                      onCancel={() => setReplyTo(null)}
                      onPosted={() => {
                        setReplyTo(null);
                        state.reload();
                      }}
                    />
                  ) : null}
                </div>
              ))}
            </div>
          );
        }}
      </Async>

      {closed ? (
        <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
          The discussion is closed — an administrator has decided on this report.
        </p>
      ) : (
        <>
          <Textarea placeholder="Add what you know about this site…" rows={2} maxLength={1000} value={body} onChange={(e) => setBody(e.target.value)} />
          <Button variant="secondary" style={{ alignSelf: 'flex-end' }} disabled={!body.trim() || posting} onClick={post}>
            {posting ? 'Posting…' : 'Post comment'}
          </Button>
        </>
      )}
    </div>
  );
}
