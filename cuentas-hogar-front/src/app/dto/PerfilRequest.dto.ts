export interface PerfilRequest {
    nombre: string;
    apellidos?: string;
    email?: string;
    telefono?: string;
    direccion?: string;
    fotoPerfil?: string;
    fechaNacimiento?: string;
    tipo: string;  // PERSONAL, MIEMBRO, EMPLEADO
}