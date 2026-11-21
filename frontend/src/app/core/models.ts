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
  /** Cola de RT del asset, p. ej. assets.sobremesas-dpto. */
  cola: string | null;
  /** "Portátil" o "Sobremesa", según la cola. */
  tipo: string | null;
  /** "admon", "dpto", "aulas" o "lab", según la cola. */
  ambito: string | null;
}

/** Respuesta de /api/assets/{inventario}/pass-iz. Es una contraseña: no guardarla. */
export interface PassIz {
  passIz: string;
  /** Caracteres 10 a 18 de passIz. */
  fragmento: string;
}

/** Último despliegue del equipo: último mensaje del script de despliegue en el asset. */
export interface UltimoDespliegue {
  transaccionId: number;
  fecha: string | null;
  contenido: string;
}

/** Solicitudes desde la ficha de un equipo (ticket en la cola de averías). */
export type AssetRequestType = 'AVERIA' | 'FORMATEO';

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
  /** Cola de RT (Queue). */
  cola: string | null;
  departamento: string | null;
  /** Nº de inventario del equipo (campo Inventario de los tickets de apoyo.dpto). */
  inventario: string | null;
  ultimaActualizacion: string | null;
  /** Último mensaje (adjunto text/plain más reciente). */
  contenido: string | null;
}

/** Respuesta de "Mis tickets" (/api/tickets/mios). */
export interface MisTickets {
  tickets: TicketReciente[];
  /** Máximo de tickets de la respuesta: si tickets llega a él, puede haber más. */
  limite: number;
}

export interface TicketsRecientes {
  /** Cola de la consulta por defecto; null si se ha usado la consulta de un perfil. */
  cola: string | null;
  /** Expresión de RT de la consulta por defecto, p. ej. "5 hours ago"; null con perfil. */
  actualizadosDesde: string | null;
  tickets: TicketReciente[];
  /** Roles de Keycloak cuya consulta se ha usado (vacío = consulta por defecto). */
  perfiles: string[];
  /** Texto de cada perfil tras "Perfil <rol>" (configurable en el backend). */
  descripciones: Record<string, string>;
}
