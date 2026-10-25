export interface RegistroRequest {
    email: string;
    password: string; 
    telefono: string;// FAMILIA, EMPRESA, ADMINISTRADOR
    nombreGrupo?: string;
    
}