/** Where an alert opens in the console (administrators and government officers). */
export function consoleAlertLink(alert) {
  if (alert.type === 'ACCOUNT_APPLICATION') return '/console/verifications';
  if (alert.type === 'ACCOUNT_REVIEW') return '/app/profile';
  if (alert.type === 'INFO_RESPONSE') return `/console/reports/${alert.reportId}?tab=info`;
  if (alert.projectId) return `/console/projects/${alert.projectId}`;
  return alert.reportId ? `/console/reports/${alert.reportId}` : null;
}

/** Where an alert opens in the volunteer app. */
export function volunteerAlertLink(alert) {
  if (alert.type === 'ACCOUNT_APPLICATION') return '/console/verifications';
  if (alert.type === 'ACCOUNT_REVIEW') return '/app/profile';
  if (alert.type === 'HELP_NEEDED') return `/app/help/${alert.id}`;
  if (alert.type === 'INFO_REQUESTED') return `/app/report/${alert.reportId}/more-info`;
  if (alert.projectId) return `/app/cleanups/${alert.projectId}`;
  return alert.reportId ? `/app/report/${alert.reportId}` : null;
}

export const isConsoleRole = (user) => user?.role === 'ADMIN' || user?.role === 'AUTHORITY';
