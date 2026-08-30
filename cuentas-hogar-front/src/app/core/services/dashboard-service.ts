import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ResumenMensualResponse } from '../../dto/ResumenMensual.dto';

@Injectable({
    providedIn: 'root'
})
export class DashboardService {
    private readonly API_URL = 'http://localhost:8080/api/movimientos/resumen';

    constructor(private http: HttpClient) {}

    obtenerResumen(): Observable<ResumenMensualResponse> {
        return this.http.get<ResumenMensualResponse>(this.API_URL);
    }
}
