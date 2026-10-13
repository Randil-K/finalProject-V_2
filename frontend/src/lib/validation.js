/**
 * What the API will accept, checked here first so someone sees the problem beside the field
 * instead of after a round trip. The server checks all of this again — this is only for comfort.
 */

const NAME = /^\p{L}[\p{L} .'-]*$/u;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function nameError(value, what = 'name') {
  const name = (value || '').trim();
  if (!name) return `Enter your ${what}.`;
  if (name.length < 2) return `That is too short to be a ${what}.`;
  if (name.length > 50) return 'Keep it under 50 characters.';
  if (!NAME.test(name)) return 'Use letters, spaces, apostrophes, hyphens and full stops only.';
  return null;
}

/** Older accounts were stored as one name; show it in the two boxes as best we can. */
export function splitName(user) {
  if (user?.firstName || user?.lastName) {
    return { firstName: user.firstName || '', lastName: user.lastName || '' };
  }
  const whole = (user?.fullName || '').trim();
  const space = whole.indexOf(' ');
  return space < 0
    ? { firstName: whole, lastName: '' }
    : { firstName: whole.slice(0, space), lastName: whole.slice(space + 1) };
}

export function emailError(value) {
  const email = (value || '').trim();
  if (!email) return 'Enter your email address.';
  if (email.length > 150) return 'That email address is too long.';
  if (!EMAIL.test(email)) return 'That does not look like an email address.';
  return null;
}

/** A phone number is optional, but a Sri Lankan one if given. */
export function phoneError(value) {
  const raw = (value || '').trim();
  if (!raw) return null;
  const digits = raw.replace(/[\s()./-]/g, '');
  const national = digits.startsWith('+94') ? digits.slice(3)
    : digits.startsWith('0094') ? digits.slice(4)
      : digits.startsWith('94') && digits.length === 11 ? digits.slice(2)
        : digits.startsWith('0') ? digits.slice(1)
          : null;
  if (national === null || !/^[1-9]\d{8}$/.test(national)) {
    return 'Use a Sri Lankan number such as +94 71 234 5678 or 071 234 5678.';
  }
  return null;
}

/**
 * A Sri Lankan identity card: the twelve-digit number issued since 2016, or the older nine
 * digits ending in V or X. Spaces are forgiven; the letter is not.
 */
export function nicError(value) {
  const nic = (value || '').replace(/\s+/g, '').toUpperCase();
  if (!nic) return 'Enter your NIC number.';
  if (!/^(\d{12}|\d{9}[VX])$/.test(nic)) {
    return 'Use 12 digits (e.g. 200012345678) or 9 digits and a V or X (e.g. 921234567V).';
  }
  return null;
}

/** What the API stores: no spaces, and the old format's letter in capitals. */
export const tidyNic = (value) => (value || '').replace(/\s+/g, '').toUpperCase();

export function passwordError(value) {
  const password = value || '';
  if (!password) return 'Choose a password.';
  if (password.length < 8) return 'Use at least 8 characters.';
  if (password.length > 100) return 'That password is too long.';
  if (!/\p{L}/u.test(password) || !/\d/.test(password)) {
    return 'Include at least one letter and one number.';
  }
  return null;
}
