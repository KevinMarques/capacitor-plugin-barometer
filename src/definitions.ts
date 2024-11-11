export interface BarometerPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
