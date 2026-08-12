export interface AuthResponse {
    token: string;
    tipo: string;
    expiracion: number;
    usuario: UsuarioInfo;
    perfilActivo: PerfilInfo;
    todosLosPerfiles: PerfilInfo[];
    grupo: GrupoInfo;
    permisos: string[];
}