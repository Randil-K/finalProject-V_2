/**
 * Towns and cities of Sri Lanka with their district, province and rough centre.
 *
 * The coordinates are what let "use my current location" name the place without calling an outside
 * service: the nearest town on this list is the answer. Coastal towns are covered most closely,
 * since that is where the reports come from.
 */
const PLACES = [
  // Western
  { name: 'Colombo', province: 'Western Province', district: 'Colombo', lat: 6.9271, lon: 79.8612 },
  { name: 'Dehiwala', province: 'Western Province', district: 'Colombo', lat: 6.8512, lon: 79.8653 },
  { name: 'Mount Lavinia', province: 'Western Province', district: 'Colombo', lat: 6.8389, lon: 79.8653 },
  { name: 'Moratuwa', province: 'Western Province', district: 'Colombo', lat: 6.7730, lon: 79.8816 },
  { name: 'Panadura', province: 'Western Province', district: 'Kalutara', lat: 6.7133, lon: 79.9026 },
  { name: 'Wadduwa', province: 'Western Province', district: 'Kalutara', lat: 6.6600, lon: 79.9300 },
  { name: 'Kalutara', province: 'Western Province', district: 'Kalutara', lat: 6.5854, lon: 79.9607 },
  { name: 'Beruwala', province: 'Western Province', district: 'Kalutara', lat: 6.4788, lon: 79.9828 },
  { name: 'Bentota', province: 'Western Province', district: 'Kalutara', lat: 6.4258, lon: 79.9959 },
  { name: 'Negombo', province: 'Western Province', district: 'Gampaha', lat: 7.2083, lon: 79.8358 },
  { name: 'Gampaha', province: 'Western Province', district: 'Gampaha', lat: 7.0917, lon: 80.0000 },
  { name: 'Wattala', province: 'Western Province', district: 'Gampaha', lat: 6.9897, lon: 79.8914 },
  { name: 'Ja-Ela', province: 'Western Province', district: 'Gampaha', lat: 7.0744, lon: 79.8919 },
  { name: 'Kelaniya', province: 'Western Province', district: 'Gampaha', lat: 6.9553, lon: 79.9220 },
  { name: 'Sri Jayawardenepura Kotte', province: 'Western Province', district: 'Colombo', lat: 6.8885, lon: 79.9179 },

  // Southern
  { name: 'Galle', province: 'Southern Province', district: 'Galle', lat: 6.0535, lon: 80.2210 },
  { name: 'Unawatuna', province: 'Southern Province', district: 'Galle', lat: 6.0100, lon: 80.2490 },
  { name: 'Hikkaduwa', province: 'Southern Province', district: 'Galle', lat: 6.1395, lon: 80.1063 },
  { name: 'Ambalangoda', province: 'Southern Province', district: 'Galle', lat: 6.2354, lon: 80.0539 },
  { name: 'Weligama', province: 'Southern Province', district: 'Matara', lat: 5.9749, lon: 80.4294 },
  { name: 'Mirissa', province: 'Southern Province', district: 'Matara', lat: 5.9483, lon: 80.4589 },
  { name: 'Matara', province: 'Southern Province', district: 'Matara', lat: 5.9549, lon: 80.5550 },
  { name: 'Dikwella', province: 'Southern Province', district: 'Matara', lat: 5.9667, lon: 80.6914 },
  { name: 'Tangalle', province: 'Southern Province', district: 'Hambantota', lat: 6.0240, lon: 80.7940 },
  { name: 'Hambantota', province: 'Southern Province', district: 'Hambantota', lat: 6.1241, lon: 81.1185 },
  { name: 'Tissamaharama', province: 'Southern Province', district: 'Hambantota', lat: 6.2789, lon: 81.2872 },

  // Eastern
  { name: 'Trincomalee', province: 'Eastern Province', district: 'Trincomalee', lat: 8.5874, lon: 81.2152 },
  { name: 'Nilaveli', province: 'Eastern Province', district: 'Trincomalee', lat: 8.7000, lon: 81.1900 },
  { name: 'Kinniya', province: 'Eastern Province', district: 'Trincomalee', lat: 8.4894, lon: 81.1811 },
  { name: 'Batticaloa', province: 'Eastern Province', district: 'Batticaloa', lat: 7.7102, lon: 81.6924 },
  { name: 'Kalmunai', province: 'Eastern Province', district: 'Ampara', lat: 7.4091, lon: 81.8344 },
  { name: 'Ampara', province: 'Eastern Province', district: 'Ampara', lat: 7.2970, lon: 81.6747 },
  { name: 'Arugam Bay', province: 'Eastern Province', district: 'Ampara', lat: 6.8400, lon: 81.8360 },
  { name: 'Pottuvil', province: 'Eastern Province', district: 'Ampara', lat: 6.8757, lon: 81.8300 },

  // Northern
  { name: 'Jaffna', province: 'Northern Province', district: 'Jaffna', lat: 9.6615, lon: 80.0255 },
  { name: 'Point Pedro', province: 'Northern Province', district: 'Jaffna', lat: 9.8167, lon: 80.2333 },
  { name: 'Chavakachcheri', province: 'Northern Province', district: 'Jaffna', lat: 9.6594, lon: 80.1636 },
  { name: 'Kilinochchi', province: 'Northern Province', district: 'Kilinochchi', lat: 9.3961, lon: 80.4036 },
  { name: 'Mannar', province: 'Northern Province', district: 'Mannar', lat: 8.9810, lon: 79.9044 },
  { name: 'Vavuniya', province: 'Northern Province', district: 'Vavuniya', lat: 8.7514, lon: 80.4971 },
  { name: 'Mullaitivu', province: 'Northern Province', district: 'Mullaitivu', lat: 9.2671, lon: 80.8142 },

  // North Western
  { name: 'Puttalam', province: 'North Western Province', district: 'Puttalam', lat: 8.0362, lon: 79.8283 },
  { name: 'Kalpitiya', province: 'North Western Province', district: 'Puttalam', lat: 8.2300, lon: 79.7600 },
  { name: 'Chilaw', province: 'North Western Province', district: 'Puttalam', lat: 7.5758, lon: 79.7953 },
  { name: 'Marawila', province: 'North Western Province', district: 'Puttalam', lat: 7.4200, lon: 79.8200 },
  { name: 'Kurunegala', province: 'North Western Province', district: 'Kurunegala', lat: 7.4863, lon: 80.3647 },
  { name: 'Kuliyapitiya', province: 'North Western Province', district: 'Kurunegala', lat: 7.4689, lon: 80.0406 },

  // Central
  { name: 'Kandy', province: 'Central Province', district: 'Kandy', lat: 7.2906, lon: 80.6337 },
  { name: 'Matale', province: 'Central Province', district: 'Matale', lat: 7.4675, lon: 80.6234 },
  { name: 'Nuwara Eliya', province: 'Central Province', district: 'Nuwara Eliya', lat: 6.9497, lon: 80.7891 },
  { name: 'Gampola', province: 'Central Province', district: 'Kandy', lat: 7.1642, lon: 80.5767 },
  { name: 'Dambulla', province: 'Central Province', district: 'Matale', lat: 7.8731, lon: 80.6511 },

  // North Central
  { name: 'Anuradhapura', province: 'North Central Province', district: 'Anuradhapura', lat: 8.3114, lon: 80.4037 },
  { name: 'Polonnaruwa', province: 'North Central Province', district: 'Polonnaruwa', lat: 7.9403, lon: 81.0188 },
  { name: 'Kekirawa', province: 'North Central Province', district: 'Anuradhapura', lat: 8.0375, lon: 80.5922 },

  // Uva
  { name: 'Badulla', province: 'Uva Province', district: 'Badulla', lat: 6.9934, lon: 81.0550 },
  { name: 'Bandarawela', province: 'Uva Province', district: 'Badulla', lat: 6.8322, lon: 80.9869 },
  { name: 'Monaragala', province: 'Uva Province', district: 'Monaragala', lat: 6.8714, lon: 81.3487 },
  { name: 'Ella', province: 'Uva Province', district: 'Badulla', lat: 6.8667, lon: 81.0466 },

  // Sabaragamuwa
  { name: 'Ratnapura', province: 'Sabaragamuwa Province', district: 'Ratnapura', lat: 6.6828, lon: 80.3992 },
  { name: 'Kegalle', province: 'Sabaragamuwa Province', district: 'Kegalle', lat: 7.2513, lon: 80.3464 },
  { name: 'Balangoda', province: 'Sabaragamuwa Province', district: 'Ratnapura', lat: 6.6500, lon: 80.7000 },
  { name: 'Embilipitiya', province: 'Sabaragamuwa Province', district: 'Ratnapura', lat: 6.3433, lon: 80.8492 },
];

/** Every town in a province, or all of them when no province has been chosen yet. */
export function citiesIn(province) {
  return PLACES.filter((place) => !province || place.province === province)
    .map((place) => place.name)
    .sort((a, b) => a.localeCompare(b));
}

/** The province a town belongs to. */
export function provinceOf(city) {
  return PLACES.find((place) => place.name === city)?.province || '';
}

/** The district a town belongs to, for the fields that ask for one. */
export function districtOf(city) {
  return PLACES.find((place) => place.name === city)?.district || '';
}

/** The nearest town to a pair of coordinates, so a captured location can name itself. */
export function nearestPlace(latitude, longitude) {
  if (latitude == null || longitude == null) return null;
  const toRad = (deg) => (deg * Math.PI) / 180;
  let best = null;
  let bestDistance = Infinity;
  for (const place of PLACES) {
    const dLat = toRad(place.lat - latitude);
    const dLon = toRad(place.lon - longitude);
    const a = Math.sin(dLat / 2) ** 2
      + Math.cos(toRad(latitude)) * Math.cos(toRad(place.lat)) * Math.sin(dLon / 2) ** 2;
    const km = 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    if (km < bestDistance) {
      bestDistance = km;
      best = place;
    }
  }
  return best ? { ...best, distanceKm: bestDistance } : null;
}
