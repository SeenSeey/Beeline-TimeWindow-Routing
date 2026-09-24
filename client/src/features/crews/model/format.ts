/** "55.75123, 37.61234" — latitude first, 5 decimals (about a metre), as on the request page */
export const formatCoordinates = (latitude: number, longitude: number): string =>
  `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`;
