import React from 'react';
import { Alert, Avatar, Button, Card, Icon, IconButton, Input } from '../design-system';
import { api } from '../api/index.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { mediaUrl } from '../api/client.js';
import { formatDate, plural } from '../lib/format.js';

const CHAPTERS = [
  { stage: 'BEFORE', title: 'How this project started', blurb: 'The site as the owner found it.' },
  { stage: 'DURING', title: 'During the project', blurb: 'The cleanup under way.' },
  { stage: 'AFTER', title: 'After the cleanup', blurb: 'What the site looks like now.' },
];

const CHAPTER_OF = Object.fromEntries(CHAPTERS.map((chapter) => [chapter.stage, chapter]));
const ORDER = CHAPTERS.map((chapter) => chapter.stage);
const SLIDE_MS = 3000;

/** One update: the photo, what the owner said about it, and what everyone else made of it. */
function Slide({ projectId, update, onChanged, onHold }) {
  const { user } = useAuth();
  const [comment, setComment] = React.useState('');
  const [busy, setBusy] = React.useState(null);
  const [error, setError] = React.useState(null);
  const [showComments, setShowComments] = React.useState(false);

  // Nobody should have the slide move out from under them while they are writing.
  function openComments(open) {
    setShowComments(open);
    onHold(open);
  }

  async function like() {
    setBusy('like');
    setError(null);
    try {
      onChanged(await api.projects.likeUpdate(projectId, update.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(null);
    }
  }

  async function send(event) {
    event.preventDefault();
    if (!comment.trim()) return;
    setBusy('comment');
    setError(null);
    try {
      onChanged(await api.projects.commentOnUpdate(projectId, update.id, comment.trim()));
      setComment('');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(null);
    }
  }

  const comments = update.comments || [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {update.imageUrl ? (
        <img
          src={mediaUrl(update.imageUrl)}
          alt={update.note || 'Progress photo'}
          style={{ width: '100%', maxHeight: 360, objectFit: 'cover', borderRadius: 'var(--radius-md)', display: 'block', background: 'var(--surface-sunken)' }}
        />
      ) : null}

      {update.note ? (
        <blockquote style={{ margin: 0, paddingLeft: 12, borderLeft: '3px solid var(--accent)', font: 'var(--text-body)', color: 'var(--text-heading)' }}>
          “{update.note}”
        </blockquote>
      ) : null}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Avatar name={update.author?.fullName || ''} src={mediaUrl(update.author?.avatarUrl)} size="xs" />
        <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
          {update.author?.fullName} · {formatDate(update.createdAt)}
        </span>
      </div>

      {error ? <Alert tone="danger" title="That didn't work" onDismiss={() => setError(null)}>{error}</Alert> : null}

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', flexWrap: 'wrap' }}>
        <Button
          size="sm"
          variant={update.likedByMe ? 'primary' : 'secondary'}
          iconLeft="thumbs-up"
          disabled={!user || busy === 'like'}
          aria-pressed={Boolean(update.likedByMe)}
          onClick={like}
        >
          {update.likedByMe ? 'Liked' : 'Like'}{update.likeCount ? ` (${update.likeCount})` : ''}
        </Button>
        <Button
          size="sm"
          variant="ghost"
          iconLeft="message-square"
          onClick={() => openComments(!showComments)}
        >
          {comments.length ? plural(comments.length, 'comment') : 'Comment'}
        </Button>
      </div>

      {showComments ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
          {comments.map((c) => (
            <div key={c.id} style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
              <Avatar name={c.author?.fullName || ''} src={mediaUrl(c.author?.avatarUrl)} size="xs" />
              <div style={{ flex: 1, minWidth: 0, background: 'var(--surface-sunken)', borderRadius: 'var(--radius-md)', padding: 'var(--space-2) var(--space-3)' }}>
                <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
                  {c.author?.fullName} · {formatDate(c.createdAt)}
                </span>
                <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-heading)', margin: '2px 0 0' }}>{c.body}</p>
              </div>
            </div>
          ))}

          {user ? (
            <form onSubmit={send} style={{ display: 'flex', gap: 8 }}>
              <Input
                placeholder="Say something about this"
                aria-label="Your comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                style={{ flex: 1 }}
              />
              <Button type="submit" size="sm" disabled={!comment.trim() || busy === 'comment'}>
                {busy === 'comment' ? 'Posting…' : 'Post'}
              </Button>
            </form>
          ) : (
            <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>Sign in to join the conversation.</p>
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * The project told through its own photos and notes, before to after, as one slideshow that moves
 * on by itself every few seconds and stops the moment somebody takes hold of it.
 */
export default function ProjectStory({ project, onChanged }) {
  const updates = React.useMemo(() => [...(project.updates || [])].sort((a, b) => {
    const stage = ORDER.indexOf(a.stage) - ORDER.indexOf(b.stage);
    if (stage !== 0) return stage;
    // Posted-at is only accurate to the second, so the id settles anything posted together.
    return (new Date(a.createdAt) - new Date(b.createdAt)) || (a.id - b.id);
  }), [project.updates]);

  const [index, setIndex] = React.useState(0);
  const [held, setHeld] = React.useState(false);
  const count = updates.length;
  const current = count ? Math.min(index, count - 1) : 0;

  React.useEffect(() => {
    if (held || count < 2) return undefined;
    const timer = setInterval(() => setIndex((i) => (i + 1) % count), SLIDE_MS);
    return () => clearInterval(timer);
  }, [held, count]);

  if (count === 0) {
    return (
      <Card padding="lg">
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>The story so far</span>
        <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-muted)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
          <Icon name="camera" size="sm" />
          Nothing posted yet. The project owner adds photos as the cleanup goes.
        </p>
      </Card>
    );
  }

  const update = updates[current];
  const chapter = CHAPTER_OF[update.stage] || CHAPTERS[1];
  const go = (next) => {
    setIndex((next + count) % count);
    setHeld(true);
  };

  return (
    <Card
      padding="lg"
      onMouseEnter={() => setHeld(true)}
      onMouseLeave={() => setHeld(false)}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8, flexWrap: 'wrap' }}>
        <div>
          <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>{chapter.title}</span>
          <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: 2 }}>{chapter.blurb}</p>
        </div>
        {count > 1 ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <IconButton icon="chevron-left" label="Previous photo" onClick={() => go(current - 1)} />
            <span style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
              {current + 1} of {count}
            </span>
            <IconButton icon="chevron-right" label="Next photo" onClick={() => go(current + 1)} />
          </div>
        ) : null}
      </div>

      <div style={{ marginTop: 'var(--space-4)' }}>
        <Slide
          key={update.id}
          projectId={project.id}
          update={update}
          onChanged={onChanged}
          onHold={setHeld}
        />
      </div>

      {count > 1 ? (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 'var(--space-4)' }}>
          {updates.map((item, i) => (
            <button
              key={item.id}
              type="button"
              aria-label={`Photo ${i + 1} of ${count}`}
              aria-current={i === current}
              onClick={() => go(i)}
              style={{
                width: i === current ? 22 : 8,
                height: 8,
                padding: 0,
                border: 'none',
                borderRadius: 999,
                cursor: 'pointer',
                transition: 'width 150ms ease, background 150ms ease',
                background: i === current ? 'var(--accent)' : 'var(--border-default)',
              }}
            />
          ))}
        </div>
      ) : null}
    </Card>
  );
}
