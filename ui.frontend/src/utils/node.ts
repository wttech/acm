export function isProduction() {
  return process.env.NODE_ENV === 'production';
}

export const devServerPort = 5501;
