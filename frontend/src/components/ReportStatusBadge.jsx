import React from 'react';
import { StatusBadge } from '../design-system';
import { statusKey } from '../lib/format.js';

// The design system carries the SRS lifecycle words, which read oddly to the people using the app.
// "Escalated" sounds like trouble when it only means the report has moved on to the government
// officer; who verified it is spelled out in the Official review card. One the officer approved
// has become a project.
const RENAMED = {
  APPROVED: { status: 'verified', label: 'Project' },
  ESCALATED: { status: 'verified', label: 'With government officer' },
};

export default function ReportStatusBadge({ status, ...props }) {
  const renamed = RENAMED[status];
  if (renamed) {
    return <StatusBadge status={renamed.status} label={renamed.label} {...props} />;
  }
  return <StatusBadge status={statusKey(status)} {...props} />;
}
