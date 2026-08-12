import { Injectable } from '@angular/core';
import { MovimientoRequest } from '../../dto/MovimientoRequest.dto';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface MovimientoResponse {
    id: number;
    monto: number;
    tipo: string;
    categoria: string;
    descripcion: string;
    fecha: string;
    perfilNombre: string;
    usuarioNombre: string;
}

@Injectable({
    providedIn: 'root'
})
export class MovimientoService {
    private readonly API_URL = 'http://localhost:8080/api/movimientos';

    constructor(private http: HttpClient) {}

    // Crear movimiento
    crear(request: MovimientoRequest): Observable<MovimientoResponse> {
        return this.http.post<MovimientoResponse>(this.API_URL, request);
    }

    // Obtener mis movimientos
    getMisMovimientos(page: number = 0, size: number = 20): Observable<any> {
        return this.http.get<any>(`${this.API_URL}?page=${page}&size=${size}`);
    }

    // Obtener movimientos de un perfil
    getMovimientosByPerfil(perfilId: number, page: number = 0, size: number = 20): Observable<any> {
        return this.http.get<any>(`${this.API_URL}/perfil/${perfilId}?page=${page}&size=${size}`);
    }

    // Obtener movimiento por ID
    getMovimiento(id: number): Observable<MovimientoResponse> {
        return this.http.get<MovimientoResponse>(`${this.API_URL}/${id}`);
    }

    // Actualizar movimiento
    actualizar(id: number, request: MovimientoRequest): Observable<MovimientoResponse> {
        return this.http.put<MovimientoResponse>(`${this.API_URL}/${id}`, request);
    }

    // Eliminar movimiento
    eliminar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.API_URL}/${id}`);
    }
}