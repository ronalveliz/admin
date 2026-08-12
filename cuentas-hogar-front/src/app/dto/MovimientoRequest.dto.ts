export interface MovimientoRequest {
    monto: number;
    tipo: string;  // INGRESO, GASTO, TRANSFERENCIA
    categoria: string;
    descripcion?: string;
    fecha?: string;
    perfilId?: number;
}