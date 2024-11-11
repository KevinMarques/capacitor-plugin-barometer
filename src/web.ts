import { WebPlugin } from '@capacitor/core';

import type { BarometerPluginPlugin } from './definitions';

export class BarometerPluginWeb extends WebPlugin implements BarometerPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
