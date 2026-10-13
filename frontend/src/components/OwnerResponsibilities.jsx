import React from 'react';
import { Alert, Button, Card, Icon } from '../design-system';

/** What a project owner takes on. Shown before anyone files a report, and agreed to or declined. */
const DUTIES = [
  {
    icon: 'camera',
    title: 'Show the site before you start',
    body: 'Post a “before” photo once the government officer approves the cleanup, so everyone can see what they are coming to.',
  },
  {
    icon: 'users',
    title: 'Be there on the day and keep the team together',
    body: 'Volunteers and divers are called in from up to 25 km away. Meet them at the site, agree who does what, and keep the work safe.',
  },
  {
    icon: 'image',
    title: 'Record the progress as it goes',
    body: 'Post photos and short notes while the cleanup is under way. These become the project’s story, which the community can read and comment on.',
  },
  {
    icon: 'badge-check',
    title: 'Close the project honestly',
    body: 'Post the “after” photo when the site is genuinely clean. That marks the project complete and tells everyone who helped.',
  },
  {
    icon: 'thumbs-up',
    title: 'Rate the people who turned up',
    body: 'Once it is finished, mark each participant’s contribution. Those marks build their standing in Tideline.',
  },
];

export default function OwnerResponsibilities({ onAccept, onDecline }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
      <div>
        <h1 style={{ font: 'var(--text-h2)', color: 'var(--text-strong)' }}>Before you report</h1>
        <p style={{ font: 'var(--text-body)', color: 'var(--text-body-color)', marginTop: 6 }}>
          If the administrator and the government officer approve your report, it becomes a cleanup project
          and <strong>you become its owner</strong>. Please read what that means and tell us whether you accept it.
        </p>
      </div>

      <Card padding="lg">
        <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>A project owner’s responsibilities</span>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', marginTop: 'var(--space-4)' }}>
          {DUTIES.map((duty, index) => (
            <div key={duty.title} style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
              <span
                style={{
                  width: 32, height: 32, flexShrink: 0, borderRadius: '50%', display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                  background: 'var(--sea-100)', color: 'var(--sea-600)',
                }}
              >
                <Icon name={duty.icon} size="sm" />
              </span>
              <div style={{ minWidth: 0 }}>
                <span style={{ font: 'var(--text-label)', color: 'var(--text-heading)' }}>
                  {index + 1}. {duty.title}
                </span>
                <p style={{ font: 'var(--text-body-sm)', color: 'var(--text-body-color)', marginTop: 2 }}>{duty.body}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Alert tone="info" title="If you would rather not take this on">
        Your report is still filed and still goes through verification and review exactly the same way. The only
        difference is that the progress updates are left open: anyone who joins the cleanup can post the photos
        and notes, and close the project when the site is clean.
      </Alert>

      <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
        <Button size="lg" iconLeft="badge-check" onClick={onAccept}>
          I accept — I'll own the project
        </Button>
        <Button size="lg" variant="secondary" iconLeft="users" onClick={onDecline}>
          I can't — let participants post progress
        </Button>
      </div>

      <p style={{ font: 'var(--text-caption)', color: 'var(--text-muted)' }}>
        Either way the site gets reported. You can see this page again from the report form before you submit.
      </p>
    </div>
  );
}
