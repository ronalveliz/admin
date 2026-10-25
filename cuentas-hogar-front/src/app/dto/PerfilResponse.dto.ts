// src/app/dto/PerfilResponse.dto.ts

export interface PerfilResponse {
    id: number;
    nombre: string;
    apellidos: string;
    nombreCompleto: string;
    email: string;
    telefono: string;
    direccion: string;
    fotoPerfil: string;
    fechaNacimiento?: string;
    tipo: string;
    totalIngresos: number;
    totalGastos: number;
    balance: number;
    tieneAccesoIndependiente: boolean;
    activo: boolean;
}