/** The API returns enum names in upper case; StatusBadge keys are lower case. */
export const statusKey = (status) => String(status || '').toLowerCase();

export const SEVERITY_LABEL = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
};

export const SEVERITY_VAR = {
  LOW: '--severity-low',
  MEDIUM: '--severity-medium',
  HIGH: '--severity-high',
  CRITICAL: '--severity-critical',
};

export const CERTIFICATION_LABEL = {
  OPEN_WATER: 'Open Water',
  ADVANCED_OPEN_WATER: 'Advanced Open Water',
  RESCUE_DIVER: 'Rescue Diver',
  DIVEMASTER: 'Divemaster',
  INSTRUCTOR: 'Instructor',
};

export const ORGANIZATION_LABEL = {
  NGO: 'NGO',
  TOURISM: 'Tourism',
  MARINE_INSTITUTION: 'Marine institution',
};

export const ROLE_LABEL = {
  CITIZEN: 'Community member',
  DIVER: 'Volunteer diver',
  ORGANIZATION: 'Organisation',
  AUTHORITY: 'Government officer',
  ADMIN: 'Administrator',
};

export const PROJECT_STATUS_LABEL = { PLANNED: 'Planned', ACTIVE: 'Active', COMPLETED: 'Completed' };
export const PROJECT_STATUS_TONE = { PLANNED: 'neutral', ACTIVE: 'accent', COMPLETED: 'success' };

/** Great-circle distance in kilometres, or null when either point is missing. */
export function distanceKm(lat1, lon1, lat2, lon2) {
  if ([lat1, lon1, lat2, lon2].some((value) => value == null)) return null;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`;

export function formatDate(iso, withYear = true) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    ...(withYear ? { year: 'numeric' } : null),
  });
}

export function timeAgo(iso) {
  if (!iso) return '';
  const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? '' : 's'} ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days} day${days === 1 ? '' : 's'} ago`;
  return formatDate(iso);
}

export function locationLine(item) {
  return [item?.locationName, item?.province].filter(Boolean).join(' · ');
}

/** All nine provinces of Sri Lanka. Inland provinces are included for rivers, lakes and reservoirs. */
export const PROVINCES = [
  'Western Province',
  'Central Province',
  'Southern Province',
  'Northern Province',
  'Eastern Province',
  'North Western Province',
  'North Central Province',
  'Uva Province',
  'Sabaragamuwa Province',
];

export const CERTIFICATION_OPTIONS = Object.entries(CERTIFICATION_LABEL).map(([value, label]) => ({ value, label }));

export const REVIEW_DECISION = {
  PENDING: { label: 'Pending', tone: 'warning', icon: 'clock' },
  APPROVED: { label: 'Approved', tone: 'success', icon: 'badge-check' },
  REJECTED: { label: 'Rejected', tone: 'danger', icon: 'x' },
  MORE_INFO_REQUESTED: { label: 'More info requested', tone: 'info', icon: 'message-square' },
};

export const ACCOUNT_STATUS = {
  APPROVED: { label: 'Verified', tone: 'success' },
  PENDING_REVIEW: { label: 'Pending verification', tone: 'warning' },
  REJECTED: { label: 'Not approved', tone: 'danger' },
};

/** Where a report is in official review, in a few words for lists. */
export function reviewStage(report) {
  if (report.status === 'REJECTED') {
    return { label: report.authorityDecision === 'REJECTED' ? 'Rejected by authority' : 'Rejected by admin', tone: 'danger' };
  }
  if (report.status === 'ESCALATED') {
    if (report.infoRequestStatus === 'OPEN') return { label: 'Waiting for reporter', tone: 'info' };
    if (report.infoRequestStatus === 'ANSWERED' && report.authorityDecision === 'MORE_INFO_REQUESTED') {
      return { label: 'Reporter sent info', tone: 'accent' };
    }
    return { label: 'Waiting for government officer', tone: 'warning' };
  }
  if (report.infoRequestStatus === 'OPEN') return { label: 'Waiting for reporter', tone: 'info' };
  if (report.infoRequestStatus === 'ANSWERED' && report.adminDecision === 'MORE_INFO_REQUESTED') {
    return { label: 'Reporter sent info', tone: 'accent' };
  }
  if (report.status !== 'VERIFIED') return { label: 'Community checking', tone: 'neutral' };
  return { label: 'Waiting for admin', tone: 'warning' };
}

/** Reports a government officer approved have become projects, so they close to further review. */
export const CLOSED_REPORT_STATUSES = ['APPROVED', 'REJECTED', 'CLEANED'];

export function formatBytes(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Worded for the diver who applied. */
export const APPLICATION_STATUS = {
  PENDING: { label: 'Application sent', tone: 'accent' },
  ACCEPTED: { label: 'Accepted', tone: 'success' },
  DECLINED: { label: 'Not selected', tone: 'neutral' },
};
