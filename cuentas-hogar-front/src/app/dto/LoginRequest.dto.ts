export interface LoginRequest {
    email: string;
    password: string;
    perfilId?: number;  // Opcional: para login con perfil específico
}