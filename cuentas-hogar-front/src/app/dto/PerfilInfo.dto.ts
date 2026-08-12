export interface PerfilInfo {
    id: number;
    nombre: string;
    apellidos: string;
    nombreCompleto: string;
    email: string;
    telefono: string;
    tipo: string;
    fotoPerfil: string;
    totalIngresos: number;
    totalGastos: number;
    balance: number;
    tieneAccesoIndependiente: boolean;
}