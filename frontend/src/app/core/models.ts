/** Tipos de la API del backend (/api). Las fechas llegan como texto ISO 8601. */

export type AssetSearchField = 'INVENTARIO' | 'SERIE' | 'USUARIO_WINDOWS';

export interface Asset {
  id: number;
  numeroInventario: string | null;
  numeroSerie: string | null;
  marca: string | null;
  modelo: string | null;
  sufijoDns: string | null;
  ipEth0: string | null;
  macEth0: string | null;
  fechaCompra: string | null;
  garantia: string | null;
  /** Texto tal cual viene de RT (puede llevar un '*' final). */
  fechaP: string | null;
  usuarioWindows: string | null;
}

/** Respuesta de /api/assets/{inventario}/pass-iz. Es una contraseña: no guardarla. */
export interface PassIz {
  passIz: string;
  /** Caracteres 10 a 18 de passIz. */
  fragmento: string;
}

export interface AssetMatch {
  matchedBy: AssetSearchField;
  asset: Asset;
}

export interface Ticket {
  id: number;
  subject: string;
  queue: string;
  status: string;
  owner: string;
  requestors: string[];
  priority: number | null;
  created: string | null;
  lastUpdated: string | null;
  customFields: Record<string, string>;
}

export interface TicketHistoryEntry {
  id: number;
  type: string;
  creator: string;
  created: string | null;
  description: string;
  content: string | null;
}

export interface CurrentUser {
  username: string;
  /** Opcional: los usuarios de Keycloak entran con nombre de usuario. */
  email: string | null;
  name: string;
  agent: boolean;
}

/** Ticket de la vista de tickets recientes. Las fechas son ISO 8601. */
export interface TicketReciente {
  id: number;
  asunto: string;
  estado: string | null;
  propietario: string | null;
  solicitante: string | null;
  departamento: string | null;
  ultimaActualizacion: string | null;
  /** Último mensaje (adjunto text/plain más reciente). */
  contenido: string | null;
}

export interface TicketsRecientes {
  /** Cola de la consulta por defecto; null si se ha usado la consulta de un perfil. */
  cola: string | null;
  /** Expresión de RT de la consulta por defecto, p. ej. "5 hours ago"; null con perfil. */
  actualizadosDesde: string | null;
  tickets: TicketReciente[];
  /** Roles de Keycloak cuya consulta se ha usado (vacío = consulta por defecto). */
  perfiles: string[];
}
