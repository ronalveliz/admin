import { Injectable } from '@angular/core';
import { ChartOptions } from 'chart.js';

@Injectable({
    providedIn: 'root'
})
export class ChartService {
    private readonly colors = ['#4CAF50', '#FF5722', '#2196F3', '#FF9800', '#9C27B0', '#E91E63', '#00BCD4', '#8BC34A'];

    getLineChartData(movimientos: any[]): any {
        const porFecha = new Map<string, number>();
        movimientos.forEach(movimiento => {
            const fecha = movimiento.fecha?.substring(0, 10) || '';
            const valor = movimiento.tipo === 'INGRESO' ? movimiento.monto : -movimiento.monto;
            porFecha.set(fecha, (porFecha.get(fecha) || 0) + valor);
        });

        const labels = Array.from(porFecha.keys()).sort();
        return {
            labels,
            datasets: [{
                label: 'Balance diario',
                data: labels.map(fecha => porFecha.get(fecha) || 0),
                borderColor: '#2196F3',
                backgroundColor: 'rgba(33, 150, 243, 0.15)',
                fill: true,
                tension: 0.3
            }]
        };
    }

    getDonutChartData(movimientos: any[]): any {
        const porCategoria = this.sumarPor(movimientos.filter(movimiento => movimiento.tipo === 'GASTO'), 'categoria');
        return {
            labels: Object.keys(porCategoria),
            datasets: [{
                data: Object.values(porCategoria),
                backgroundColor: this.colors
            }]
        };
    }

    getBarChartData(movimientos: any[]): any {
        const ingresos = this.sumarPor(movimientos.filter(movimiento => movimiento.tipo === 'INGRESO'), 'categoria');
        const gastos = this.sumarPor(movimientos.filter(movimiento => movimiento.tipo === 'GASTO'), 'categoria');
        const labels = Array.from(new Set([...Object.keys(ingresos), ...Object.keys(gastos)]));
        return {
            labels,
            datasets: [
                { label: 'Ingresos', data: labels.map(label => ingresos[label] || 0), backgroundColor: '#4CAF50' },
                { label: 'Gastos', data: labels.map(label => gastos[label] || 0), backgroundColor: '#FF5722' }
            ]
        };
    }

    getLineOptions(): ChartOptions<'line'> {
        return this.getCommonOptions<'line'>();
    }

    getDonutOptions(): ChartOptions<'doughnut'> {
        return this.getCommonOptions<'doughnut'>();
    }

    getBarOptions(): ChartOptions<'bar'> {
        return this.getCommonOptions<'bar'>();
    }

    private sumarPor(movimientos: any[], campo: string): Record<string, number> {
        return movimientos.reduce((totales, movimiento) => {
            const clave = movimiento[campo] || 'OTROS';
            totales[clave] = (totales[clave] || 0) + (movimiento.monto || 0);
            return totales;
        }, {} as Record<string, number>);
    }

    private getCommonOptions<T extends 'line' | 'bar' | 'doughnut'>(): ChartOptions<T> {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: true, position: 'bottom' }
            }
        } as ChartOptions<T>;
    }
}
