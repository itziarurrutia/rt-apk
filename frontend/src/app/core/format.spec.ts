import {
  ASSET_QUERY_PATTERN,
  formatMac,
  isOnOrAfterToday,
  statusLabel,
  updatedSinceLabel,
} from './format';

describe('format', () => {
  it('acepta inventario, serie y usuario de Windows y rechaza el resto', () => {
    for (const ok of ['78763', 'PF4XK21B', 'carlos.lopez', 'maría_ruiz-2']) {
      expect(ASSET_QUERY_PATTERN.test(ok)).toBe(true);
    }
    for (const ko of ['', 'a b', '../ticket/1', 'x?y']) {
      expect(ASSET_QUERY_PATTERN.test(ko)).toBe(false);
    }
  });

  it('formatea MAC sin separadores', () => {
    expect(formatMac('30C599728B91')).toBe('30:C5:99:72:8B:91');
    expect(formatMac('30:c5:99')).toBe('30:c5:99');
    expect(formatMac(null)).toBeNull();
  });

  it('compara fechas de garantía con hoy', () => {
    expect(isOnOrAfterToday('2999-01-01')).toBe(true);
    expect(isOnOrAfterToday('2000-01-01')).toBe(false);
    expect(isOnOrAfterToday(null)).toBeNull();
  });

  it('traduce la expresión de fecha de RT', () => {
    expect(updatedSinceLabel('5 hours ago')).toBe('en las últimas 5 horas');
    expect(updatedSinceLabel('1 hour ago')).toBe('en la última hora');
    expect(updatedSinceLabel('2 days ago')).toBe('en los últimos 2 días');
    expect(updatedSinceLabel('2026-11-01')).toBe('desde 2026-11-01');
  });

  it('traduce estados de RT', () => {
    expect(statusLabel('open')).toBe('Abierto');
    expect(statusLabel('custom')).toBe('custom');
  });
});
