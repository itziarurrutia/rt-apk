import { AssetSearchField } from './models';

/** Mismo criterio que el backend: letras, números, '.', '_' o '-'. */
export const ASSET_QUERY_PATTERN = /^[\p{L}\p{N}._-]{1,64}$/u;

export const MATCHED_BY_LABEL: Record<AssetSearchField, string> = {
  INVENTARIO: 'Nº de inventario',
  SERIE: 'Nº de serie',
  USUARIO_WINDOWS: 'Usuario Windows',
};

const STATUS_LABEL: Record<string, string> = {
  new: 'Nuevo',
  open: 'Abierto',
  stalled: 'Parado',
  resolved: 'Resuelto',
  rejected: 'Rechazado',
  deleted: 'Borrado',
};

export function statusLabel(status: string): string {
  return STATUS_LABEL[status] ?? status;
}

const HISTORY_LABEL: Record<string, string> = {
  Create: 'Creación',
  Correspond: 'Respuesta',
  Comment: 'Comentario interno',
};

export function historyLabel(type: string): string {
  return HISTORY_LABEL[type] ?? type;
}

const TIME_UNITS: Record<string, [string, string]> = {
  minute: ['el último minuto', 'minutos'],
  hour: ['la última hora', 'horas'],
  day: ['el último día', 'días'],
  week: ['la última semana', 'semanas'],
};

/** Expresión de fecha de RT a texto: "5 hours ago" -> "en las últimas 5 horas". */
export function updatedSinceLabel(rtExpression: string): string {
  const m = /^(\d+)\s+(minute|hour|day|week)s?\s+ago$/i.exec(rtExpression.trim());
  if (!m) {
    return `desde ${rtExpression}`;
  }
  const [singular, plural] = TIME_UNITS[m[2].toLowerCase()];
  const n = Number(m[1]);
  if (n === 1) {
    return `en ${singular}`;
  }
  const feminine = plural === 'horas' || plural === 'semanas';
  return `en ${feminine ? 'las últimas' : 'los últimos'} ${n} ${plural}`;
}

/** "30C599728B91" -> "30:C5:99:72:8B:91". Otros formatos se dejan como están. */
export function formatMac(mac: string | null): string | null {
  if (!mac) {
    return null;
  }
  return /^[0-9A-Fa-f]{12}$/.test(mac) ? mac.toUpperCase().match(/.{2}/g)!.join(':') : mac;
}

/** true si la fecha ISO (yyyy-MM-dd) es hoy o posterior. */
export function isOnOrAfterToday(isoDate: string | null): boolean | null {
  if (!isoDate) {
    return null;
  }
  const today = new Date();
  const todayIso = [today.getFullYear(), today.getMonth() + 1, today.getDate()]
    .map((n, i) => String(n).padStart(i === 0 ? 4 : 2, '0'))
    .join('-');
  return isoDate >= todayIso;
}
