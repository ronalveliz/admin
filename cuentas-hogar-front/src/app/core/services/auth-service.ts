import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { AuthResponse } from '../../dto/AuthResponse.dto';
import { LoginRequest } from '../../dto/LoginRequest.dto';
import { RegistroRequest } from '../../dto/RegistroRequest.dto';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
  removeToken() {
    throw new Error('Method not implemented.');
  }
    
    private readonly API_URL = 'http://localhost:8080/api/auth';
    private tokenKey = 'auth_token';
    private userKey = 'auth_user';

    private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();
  isLoggedin: any;

    constructor(private http: HttpClient) {
        this.loadStoredUser();
    }

    // ========== REGISTRO ==========

    registrar(request: RegistroRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API_URL}/registro`, request);
    }

    // ========== LOGIN ==========

    login(request: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API_URL}/login`, request)
            .pipe(
                tap(response => this.handleAuthResponse(response))
            );
    }

    // ========== LOGIN CON PERFIL ==========

    loginConPerfil(request: LoginRequest, perfilId: number): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API_URL}/login/perfil/${perfilId}`, request)
            .pipe(
                tap(response => this.handleAuthResponse(response))
            );
    }

    // ========== LOGOUT ==========

    logout(): void {
        if (typeof localStorage !== 'undefined') {
            localStorage.removeItem(this.tokenKey);
            localStorage.removeItem(this.userKey);
        }
        this.currentUserSubject.next(null);
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    private handleAuthResponse(response: AuthResponse): void {
        if (typeof localStorage !== 'undefined') {
            localStorage.setItem(this.tokenKey, response.token);
            localStorage.setItem(this.userKey, JSON.stringify(response));
        }
        this.currentUserSubject.next(response);
    }

    private loadStoredUser(): void {
        if (typeof localStorage !== 'undefined') {
            const stored = localStorage.getItem(this.userKey);
            if (stored) {
                try {
                    const user = JSON.parse(stored);
                    this.currentUserSubject.next(user);
                } catch (e) {
                    this.logout();
                }
            }
        }
    }

    getToken(): string | null {
        if (typeof localStorage !== 'undefined') {
            return localStorage.getItem(this.tokenKey);
        }
        return null;
    }

    isAuthenticated(): boolean {
        return !!this.getToken();
    }

    getCurrentUser(): AuthResponse | null {
        return this.currentUserSubject.value;
    }

    getRol(): string | null {
        const user = this.getCurrentUser();
        return user ? user.usuario.rol : null;
    }

    isAdmin(): boolean {
        const rol = this.getRol();
        return rol === 'ADMINISTRADOR' || rol === 'FAMILIA' || rol === 'EMPRESA';
    }

    isPerfilIndependiente(): boolean {
        const rol = this.getRol();
        return rol === 'MIEMBRO' || rol === 'EMPLEADO';
    }
}