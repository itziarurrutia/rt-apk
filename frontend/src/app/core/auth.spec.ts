import { splitIssuer } from './auth';

describe('splitIssuer', () => {
  it('separa la URL de Keycloak y el realm', () => {
    expect(splitIssuer('https://kc.example.org/realms/rt-mobile')).toEqual({
      url: 'https://kc.example.org',
      realm: 'rt-mobile',
    });
  });

  it('admite Keycloak bajo una ruta y la barra final', () => {
    expect(splitIssuer('https://sso.example.org/auth/realms/rt-mobile/')).toEqual({
      url: 'https://sso.example.org/auth',
      realm: 'rt-mobile',
    });
  });

  it('rechaza una URL sin realm', () => {
    expect(() => splitIssuer('https://kc.example.org')).toThrow();
  });
});
