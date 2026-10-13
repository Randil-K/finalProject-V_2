import React from 'react';
import { Button, Select, Textarea, Alert, Card, Icon, IconButton } from '../design-system';
import { api } from '../api/index.js';

const EMPTY = { stage: 'DURING', note: '' };

const STAGES = [
  { value: 'BEFORE', label: 'Before — how the site looked' },
  { value: 'DURING', label: 'During — the cleanup under way' },
  { value: 'AFTER', label: 'After — the site once finished' },
];

export default function ProgressUpdateForm({ projectId, onUpdated }) {
  const [update, setUpdate] = React.useState(EMPTY);
  const [photo, setPhoto] = React.useState(null);
  const [preview, setPreview] = React.useState(null);
  const [error, setError] = React.useState(null);
  const [busy, setBusy] = React.useState(false);
  const fileRef = React.useRef(null);

  function choose(file) {
    if (preview) URL.revokeObjectURL(preview);
    setPhoto(file || null);
    setPreview(file ? URL.createObjectURL(file) : null);
  }

  async function submit(event) {
    event.preventDefault();
    if (!photo && !update.note.trim()) {
      setError('Add a photo or a note so there is something to show.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const updated = await api.projects.addUpdate(projectId, { stage: update.stage, note: update.note.trim() || null }, photo);
      setUpdate(EMPTY);
      choose(null);
      if (fileRef.current) fileRef.current.value = '';
      onUpdated(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card padding="lg">
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <div>
          <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Record progress</span>
          <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)', marginTop: 2 }}>
            Posting an “after” photo closes the project and marks the site cleaned.
          </p>
        </div>
        {error ? <Alert tone="danger" title="That didn't post" onDismiss={() => setError(null)}>{error}</Alert> : null}

        <Select
          label="Stage"
          options={STAGES}
          value={update.stage}
          onChange={(e) => setUpdate((u) => ({ ...u, stage: e.target.value }))}
        />

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>Photo</span>
          {preview ? (
            <div style={{ position: 'relative', borderRadius: 'var(--radius-md)', overflow: 'hidden', maxWidth: 260 }}>
              <img src={preview} alt="" style={{ width: '100%', display: 'block' }} />
              <span style={{ position: 'absolute', top: 6, right: 6 }}>
                <IconButton icon="trash" label="Remove the photo" onClick={() => { choose(null); if (fileRef.current) fileRef.current.value = ''; }} />
              </span>
            </div>
          ) : (
            <label
              style={{ display: 'flex', alignItems: 'center', gap: 8, padding: 'var(--space-3) var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px dashed var(--border-default)', cursor: 'pointer', color: 'var(--text-muted)', font: 'var(--text-body-sm)' }}
            >
              <Icon name="camera" size="sm" />
              Add a photo of the site
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                hidden
                onChange={(e) => choose(e.target.files?.[0])}
              />
            </label>
          )}
        </div>

        <Textarea
          label="Note"
          hint="Optional — say what happened if you want to."
          rows={2}
          placeholder="What happened on site?"
          value={update.note}
          onChange={(e) => setUpdate((u) => ({ ...u, note: e.target.value }))}
        />

        <Button type="submit" disabled={busy} style={{ alignSelf: 'flex-start' }}>
          {busy ? 'Posting…' : 'Post update'}
        </Button>
      </form>
    </Card>
  );
}
