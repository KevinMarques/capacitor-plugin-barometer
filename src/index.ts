import { registerPlugin } from '@capacitor/core';

import type { BarometerPluginPlugin } from './definitions';

const BarometerPlugin = registerPlugin<BarometerPluginPlugin>('BarometerPlugin', {
  web: () => import('./web').then((m) => new m.BarometerPluginWeb()),
});

export * from './definitions';
export { BarometerPlugin };
