import { Injectable } from '@angular/core';
import { MovimientoRequest } from '../../dto/MovimientoRequest.dto';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

export interface MovimientosQueryParams {
    page?: number;
    size?: number;
    tipo?: string;
    categoria?: string;
    perfilId?: string;
    fechaInicio?: string;
    fechaFin?: string;
    busqueda?: string;
    sort?: string;
}

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

    // Obtener mis movimientos (con filtros opcionales)
    getMisMovimientos(params: MovimientosQueryParams = {}): Observable<Page<MovimientoResponse>> {
        let httpParams = new HttpParams()
            .set('page', (params.page ?? 0).toString())
            .set('size', (params.size ?? 20).toString());

        if (params.tipo)        httpParams = httpParams.set('tipo', params.tipo);
        if (params.categoria)   httpParams = httpParams.set('categoria', params.categoria);
        if (params.perfilId)    httpParams = httpParams.set('perfilId', params.perfilId);
        if (params.fechaInicio) httpParams = httpParams.set('fechaInicio', params.fechaInicio);
        if (params.fechaFin)    httpParams = httpParams.set('fechaFin', params.fechaFin);
        if (params.busqueda)    httpParams = httpParams.set('busqueda', params.busqueda);
        if (params.sort)        httpParams = httpParams.set('sort', params.sort);

        return this.http.get<Page<MovimientoResponse>>(this.API_URL, { params: httpParams });
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