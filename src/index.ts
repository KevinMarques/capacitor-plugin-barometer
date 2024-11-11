import { registerPlugin } from '@capacitor/core';

import { BarometerPlugin } from './definitions';

const BarometerPlugin = registerPlugin<BarometerPlugin>('BarometerPlugin');

export * from './definitions';
export { BarometerPlugin };
