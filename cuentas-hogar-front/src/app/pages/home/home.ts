import { Component, inject, signal, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth-service';
import { DashboardService } from '../../core/services/dashboard-service';
import { ResumenMensualResponse } from '../../dto/ResumenMensual.dto';
import { Sidebar } from '../../components/sidebar/sidebar';

@Component({
  selector: 'app-home',
  imports: [Sidebar],
  templateUrl: './home.html',
  styleUrl: './home.html',
})
export class Home implements OnInit {
  authService = inject(AuthService);
  private dashboardService = inject(DashboardService);

  // Señal que guardará los datos reales
  resumen = signal<ResumenMensualResponse | null>(null);

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.dashboardService.obtenerResumen().subscribe({
      next: (data: ResumenMensualResponse) => {
        this.resumen.set(data);
        console.log('✅ Datos del dashboard cargados:', data);
      },
      error: (err: any) => {
        console.error('❌ Error al obtener datos:', err);
      }
    });
  }

  // Helper para formatear moneda
  formatearMoneda(valor: number | null | undefined): string {
    if (valor === null || valor === undefined) return '$0.00';
    return '$' + valor.toLocaleString('es-ES', { minimumFractionDigits: 2 });
  }

  // Helper para el monto en la tabla (rojo o verde)
  formatearMontoTransaccion(monto: number): string {
    if (monto > 0) return `+${this.formatearMoneda(monto)}`;
    return `-${this.formatearMoneda(Math.abs(monto))}`;
  }

  // Helper para clasificar por tipo (para saber si es verde o rojo)
  esIngreso(mov: any): boolean {
    return mov.tipo === 'INGRESO' || mov.tipo === 'TRANSFERENCIA';
  }
}