import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { PerfilRequest } from '../../dto/PerfilRequest.dto';
import { Observable } from 'rxjs';
import { PerfilResponse } from '../../dto/PerfilResponse.dto';

@Injectable({
  providedIn: 'root',
})
export class PerfilService {
    private readonly API_URL = 'http://localhost:8080/api/perfiles';

    constructor(private http: HttpClient) {}

    // Crear perfil
    crear(request: PerfilRequest): Observable<PerfilResponse> {
        return this.http.post<PerfilResponse>(this.API_URL, request);
    }

    // Obtener todos mis perfiles
    getMisPerfiles(): Observable<PerfilResponse[]> {
        return this.http.get<PerfilResponse[]>(this.API_URL);
    }

    // Obtener perfil por ID
    getPerfil(id: number): Observable<PerfilResponse> {
        return this.http.get<PerfilResponse>(`${this.API_URL}/${id}`);
    }

    // Actualizar perfil
    actualizar(id: number, request: PerfilRequest): Observable<PerfilResponse> {
        return this.http.put<PerfilResponse>(`${this.API_URL}/${id}`, request);
    }

    // Deshabilitar perfil
    deshabilitar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.API_URL}/${id}`);
    }

    // Habilitar perfil
    habilitar(id: number): Observable<void> {
        return this.http.put<void>(`${this.API_URL}/${id}/habilitar`, {});
    }

    // Cambiar contraseña de perfil
    cambiarPassword(id: number, nuevaPassword: string): Observable<void> {
        return this.http.put<void>(`${this.API_URL}/${id}/password`, null, {
            params: { nuevaPassword }
        });
    }
}
