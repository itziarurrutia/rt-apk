import {
  AssetRequestDialogData,
  availableOperatingSystems,
  suggestedOperatingSystem,
} from './asset-request-dialog';

function data(partial: Partial<AssetRequestDialogData>): AssetRequestDialogData {
  return {
    tipo: 'FORMATEO',
    numeroInventario: '78763',
    procesador: null,
    sistemaOperativo: null,
    compatibleWindows11: false,
    ...partial,
  };
}

describe('suggestedOperatingSystem', () => {
  it('propone Windows 11 si el procesador es compatible', () => {
    expect(suggestedOperatingSystem(data({ compatibleWindows11: true, sistemaOperativo: 'Windows 10' }))).toBe(
      'Windows 11',
    );
  });

  it('si no, propone el sistema que ya tiene el equipo', () => {
    expect(suggestedOperatingSystem(data({ sistemaOperativo: 'Windows 10' }))).toBe('Windows 10');
    expect(suggestedOperatingSystem(data({ sistemaOperativo: 'windows 11 Pro' }))).toBe('Windows 11');
  });

  it('Windows 11 solo es una opción si el equipo puede llevarlo', () => {
    expect(availableOperatingSystems(data({ compatibleWindows11: true }))).toEqual(['Windows 10', 'Windows 11']);
    expect(availableOperatingSystems(data({ sistemaOperativo: 'Windows 11' }))).toEqual(['Windows 10', 'Windows 11']);
    const incompatible = { procesador: 'Intel Core i5-7500', sistemaOperativo: 'Windows 10' };
    expect(availableOperatingSystems(data(incompatible))).toEqual(['Windows 10']);
  });

  it('con el procesador sin indicar salen las dos y se marca la del equipo', () => {
    const unknown = data({ procesador: null, sistemaOperativo: 'Windows 10' });
    expect(availableOperatingSystems(unknown)).toEqual(['Windows 10', 'Windows 11']);
    expect(suggestedOperatingSystem(unknown)).toBe('Windows 10');
    expect(suggestedOperatingSystem(data({ procesador: ' ', sistemaOperativo: 'Windows 11' }))).toBe('Windows 11');
  });

  it('sin procesador compatible ni sistema reconocido no propone ninguno', () => {
    expect(suggestedOperatingSystem(data({ sistemaOperativo: 'Windows 7' }))).toBe('');
    expect(suggestedOperatingSystem(data({}))).toBe('');
  });
});
