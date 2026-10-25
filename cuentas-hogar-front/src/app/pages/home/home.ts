// src/app/pages/home/home.component.ts

import { Component, OnInit, inject, signal, computed, effect, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { AuthService } from '../../core/services/auth-service';
import { MovimientoService } from '../../core/services/movimiento-service';
import { PerfilService } from '../../core/services/perfil-service';
import { ChartService } from '../../core/services/chart.service';

// Registrar todos los componentes de Chart.js
Chart.register(...registerables);

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './home.html',
    styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {
    // ========== INYECCIÓN ==========
    private authService = inject(AuthService);
    private movimientoService = inject(MovimientoService);
    private perfilService = inject(PerfilService);
    private chartService = inject(ChartService);

    // ========== REFERENCIAS A CANVAS ==========
    @ViewChild('lineChart') lineChartCanvas!: ElementRef<HTMLCanvasElement>;
    @ViewChild('donutChart') donutChartCanvas!: ElementRef<HTMLCanvasElement>;
    @ViewChild('barChart') barChartCanvas!: ElementRef<HTMLCanvasElement>;

    // ========== SIGNALS ==========
    isLoading = signal<boolean>(true);
    user = signal<any>(null);
    perfiles = signal<any[]>([]);
    movimientos = signal<any[]>([]);
    resumen = signal({
        totalIngresos: 0,
        totalGastos: 0,
        balance: 0,
        porcentajeCambio: 0
    });
    gastosPorCategoria = signal<{ categoria: string; monto: number; porcentaje: number; color: string }[]>([]);
    movimientosRecientes = signal<any[]>([]);
    presupuesto = signal({
        total: 2500000,
        ejecutado: 1700000,
        porcentaje: 68
    });

    // ========== INSTANCIAS DE GRÁFICOS ==========
    private lineChart: Chart<'line', number[], string> | null = null;
    private donutChart: Chart<'doughnut', number[], string> | null = null;
    private barChart: Chart<'bar', number[], string> | null = null;

    // ========== COMPUTED ==========
    isAdmin = computed(() => {
        const user = this.user();
        return user?.usuario?.rol === 'ADMINISTRADOR' ||
               user?.usuario?.rol === 'FAMILIA' ||
               user?.usuario?.rol === 'EMPRESA';
    });

    greeting = computed(() => {
        const hour = new Date().getHours();
        if (hour < 12) return '🌅 Buenos días';
        if (hour < 18) return '☀️ Buenas tardes';
        return '🌙 Buenas noches';
    });

    userName = computed(() => {
        return this.user()?.usuario?.nombre || 'Usuario';
    });

    private colors = ['#4CAF50', '#FF5722', '#2196F3', '#FF9800', '#9C27B0', '#E91E63', '#00BCD4', '#8BC34A'];

    constructor() {
        effect(() => {
            const movs = this.movimientos();
            if (movs.length > 0) {
                this.calcularResumen(movs);
                this.calcularGastosPorCategoria(movs);
                this.actualizarMovimientosRecientes(movs);
                // Actualizar gráficos después de un pequeño delay
                setTimeout(() => this.actualizarGraficos(), 300);
            }
        });
    }

    ngOnInit(): void {
        this.user.set(this.authService.getCurrentUser());
        this.cargarDatos();
    }

    ngAfterViewInit(): void {
        // Inicializar gráficos después de que la vista esté lista
        setTimeout(() => {
            this.inicializarGraficos();
        }, 800);
    }

    ngOnDestroy(): void {
        this.destroyGraficos();
    }

    // ========== CARGA DE DATOS ==========

    cargarDatos(): void {
        this.isLoading.set(true);

        this.perfilService.getMisPerfiles().subscribe({
            next: (perfiles) => this.perfiles.set(perfiles),
            error: (err) => console.error('Error cargando perfiles:', err)
        });

        this.movimientoService.getMisMovimientos().subscribe({
            next: (data) => {
                this.movimientos.set(data.content || []);
                this.isLoading.set(false);
            },
            error: (err) => {
                console.error('Error cargando movimientos:', err);
                this.isLoading.set(false);
            }
        });
    }

    // ========== GRÁFICOS ==========

    inicializarGraficos(): void {
        this.destroyGraficos();

        try {
            // Gráfico de líneas
            if (this.lineChartCanvas) {
                const ctx = this.lineChartCanvas.nativeElement.getContext('2d');
                if (ctx) {
                    const data = this.chartService.getLineChartData(this.movimientos());
                    this.lineChart = new Chart(ctx, {
                        type: 'line',
                        data: data,
                        options: this.chartService.getLineOptions()
                    });
                    console.log('✅ Gráfico de líneas creado');
                }
            }

            // Gráfico de donut
            if (this.donutChartCanvas) {
                const ctx = this.donutChartCanvas.nativeElement.getContext('2d');
                if (ctx) {
                    const data = this.chartService.getDonutChartData(this.movimientos());
                    this.donutChart = new Chart(ctx, {
                        type: 'doughnut',
                        data: data,
                        options: this.chartService.getDonutOptions()
                    });
                    console.log('✅ Gráfico de donut creado');
                }
            }

            // Gráfico de barras
            if (this.barChartCanvas) {
                const ctx = this.barChartCanvas.nativeElement.getContext('2d');
                if (ctx) {
                    const data = this.chartService.getBarChartData(this.movimientos());
                    this.barChart = new Chart(ctx, {
                        type: 'bar',
                        data: data,
                        options: this.chartService.getBarOptions()
                    });
                    console.log('✅ Gráfico de barras creado');
                }
            }
        } catch (error) {
            console.error('❌ Error al inicializar gráficos:', error);
        }
    }

    actualizarGraficos(): void {
        try {
            if (this.lineChart) {
                this.lineChart.data = this.chartService.getLineChartData(this.movimientos());
                this.lineChart.update();
            }
            if (this.donutChart) {
                this.donutChart.data = this.chartService.getDonutChartData(this.movimientos());
                this.donutChart.update();
            }
            if (this.barChart) {
                this.barChart.data = this.chartService.getBarChartData(this.movimientos());
                this.barChart.update();
            }
        } catch (error) {
            console.error('❌ Error al actualizar gráficos:', error);
        }
    }

    destroyGraficos(): void {
        if (this.lineChart) {
            this.lineChart.destroy();
            this.lineChart = null;
        }
        if (this.donutChart) {
            this.donutChart.destroy();
            this.donutChart = null;
        }
        if (this.barChart) {
            this.barChart.destroy();
            this.barChart = null;
        }
    }

    // ========== CÁLCULOS ==========

    calcularResumen(movimientos: any[]): void {
        const ingresos = movimientos.filter(m => m.tipo === 'INGRESO').reduce((s, m) => s + m.monto, 0);
        const gastos = movimientos.filter(m => m.tipo === 'GASTO').reduce((s, m) => s + m.monto, 0);
        const balance = ingresos - gastos;
        const porcentajeCambio = ingresos > 0 ? ((balance / ingresos) * 100) : 0;

        this.resumen.set({ totalIngresos: ingresos, totalGastos: gastos, balance, porcentajeCambio });
    }

    calcularGastosPorCategoria(movimientos: any[]): void {
        const gastos = movimientos.filter(m => m.tipo === 'GASTO');
        const totalGastos = gastos.reduce((s, m) => s + m.monto, 0);

        if (totalGastos === 0) {
            this.gastosPorCategoria.set([]);
            return;
        }

        const agrupado = gastos.reduce((acc, m) => {
            const key = m.categoria || 'OTROS';
            acc[key] = (acc[key] || 0) + m.monto;
            return acc;
        }, {} as Record<string, number>);

        const result: { categoria: string; monto: number; porcentaje: number; color: string }[] = (Object.entries(agrupado) as [string, number][]) 
            .map(([categoria, monto], index) => ({
                categoria,
                monto,
                porcentaje: (monto / totalGastos) * 100,
                color: this.colors[index % this.colors.length]
            }))
            .sort((a, b) => b.monto - a.monto)
            .slice(0, 6);

        this.gastosPorCategoria.set(result);
    }

    actualizarMovimientosRecientes(movimientos: any[]): void {
        const sorted = [...movimientos]
            .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
            .slice(0, 5);
        this.movimientosRecientes.set(sorted);
    }

    // ========== UTILIDADES ==========

    formatCurrency(value: number): string {
        return new Intl.NumberFormat('es-ES', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2
        }).format(value);
    }

    getInitials(name: string): string {
        return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
    }

    getTipoIcon(tipo: string): string {
        const icons: Record<string, string> = {
            'INGRESO': 'fa-arrow-up',
            'GASTO': 'fa-arrow-down',
            'TRANSFERENCIA': 'fa-exchange-alt'
        };
        return icons[tipo] || 'fa-circle';
    }

    getTipoColor(tipo: string): string {
        const colors: Record<string, string> = {
            'INGRESO': 'text-success',
            'GASTO': 'text-danger',
            'TRANSFERENCIA': 'text-warning'
        };
        return colors[tipo] || 'text-secondary';
    }

    getFormattedDate(date: string): string {
        return new Date(date).toLocaleDateString('es-ES', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
    }

    refreshData(): void {
        this.cargarDatos();
    }
}