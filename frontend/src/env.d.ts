interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface ImportMetaEnv {
  /**
   * Built-in environment variables.
   */
  readonly NG_APP_ENV: string;
  [key: string]: any;
}
