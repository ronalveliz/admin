export interface RegistroRequest {
    email: string;
    password: string;
    nombre: string;
    telefono: string;
    roleName: string;  // FAMILIA, EMPRESA, ADMINISTRADOR
    imgUser?: string;
    nombreGrupo?: string;
    descripcionGrupo?: string;
}