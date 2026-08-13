import { UsuarioInfo } from './UsuarioInfo.dt';
import { PerfilInfo } from './PerfilInfo.dto';
import { GrupoInfo } from './GrupoInfo.dto';

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