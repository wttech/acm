const osMap = {
  Windows: {
    modifier: 'Ctrl',
    name: 'Windows',
  },
  Mac: { modifier: '⌘', name: 'Mac' },
  Linux: { modifier: 'Ctrl', name: 'Linux' },
} as const;

export const getOsInfo = () => {
  if (navigator.userAgent.toLowerCase().includes('mac')) {
    return osMap.Mac;
  } else if (navigator.userAgent.toLowerCase().includes('linux')) {
    return osMap.Linux;
  }
  return osMap.Windows;
};
