export type ErrorKind = 'invalid' | 'expired' | 'network' | 'auth-failed';

export interface AuthorizationDetails {
  clientName: string;
  scopes: string;
}
