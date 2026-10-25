// src/app/pages/movimientos/movimiento-list/movimiento-list.component.ts

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovimientoService } from '../../../core/services/movimiento-service';
import { AuthService } from '../../../core/services/auth-service';
import { PerfilService } from '../../../core/services/perfil-service';

@Component({
    selector: 'app-movimiento-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './movimiento-list.html',
    styleUrls: ['./movimiento-list.css']
})
export class MovimientoList implements OnInit {
    // ========== INYECCIÓN ==========
    private movimientoService = inject(MovimientoService);
    private authService = inject(AuthService);
    private perfilService = inject(PerfilService);

    // ========== MATH PROPERTY ==========
    Math = Math;

    // ========== SIGNALS ==========
    isLoading = signal<boolean>(true);
    movimientos = signal<any[]>([]);
    perfiles = signal<any[]>([]);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);

    // ========== FILTROS ==========
    filtroTipo = signal<string>('TODOS');
    filtroCategoria = signal<string>('TODAS');
    filtroPerfil = signal<string>('TODOS');
    filtroFechaInicio = signal<string>('');
    filtroFechaFin = signal<string>('');
    busqueda = signal<string>('');

    // ========== COMPUTED ==========
    isAdmin = computed(() => {
        const user = this.authService.getCurrentUser();
        return user?.usuario?.rol === 'ADMINISTRADOR' ||
               user?.usuario?.rol === 'FAMILIA' ||
               user?.usuario?.rol === 'EMPRESA';
    });

    tiposFiltro = [
        { value: 'TODOS', label: 'Todos' },
        { value: 'INGRESO', label: '💰 Ingresos' },
        { value: 'GASTO', label: '💸 Gastos' },
        { value: 'TRANSFERENCIA', label: '🔄 Transferencias' }
    ];

    categorias = [
        'ALIMENTACION', 'TRANSPORTE', 'VIVIENDA', 'SERVICIOS',
        'EDUCACION', 'SALUD', 'ENTRETENIMIENTO', 'AHORRO',
        'INVERSION', 'SALARIO', 'IMPUESTOS', 'OTROS'
    ];

    categoriasFiltro = computed(() => {
        const base = [
            { value: 'TODAS', label: 'Todas las categorías' }
        ];
        const cats = this.categorias.map(c => ({
            value: c,
            label: this.getCategoriaLabel(c)
        }));
        return [...base, ...cats];
    });

    resumenIngresos = computed(() => {
        return this.movimientos()
            .filter(m => m.tipo === 'INGRESO')
            .reduce((sum, m) => sum + (m.monto || 0), 0);
    });

    resumenGastos = computed(() => {
        return this.movimientos()
            .filter(m => m.tipo === 'GASTO')
            .reduce((sum, m) => sum + (m.monto || 0), 0);
    });

    resumenBalance = computed(() => {
        return this.resumenIngresos() - this.resumenGastos();
    });

    // ========== NGONINIT ==========
    ngOnInit(): void {
        this.cargarPerfiles();
        this.cargarMovimientos();
    }

    // ========== CARGA DE DATOS ==========

    cargarPerfiles(): void {
        this.perfilService.getMisPerfiles().subscribe({
            next: (perfiles) => {
                this.perfiles.set(perfiles);
            },
            error: (err) => {
                console.error('❌ Error cargando perfiles:', err);
            }
        });
    }

    cargarMovimientos(): void {
        this.isLoading.set(true);

        const tipo = this.filtroTipo();
        const categoria = this.filtroCategoria();
        const perfil = this.filtroPerfil();

        this.movimientoService.getMisMovimientos({
            page: this.currentPage(),
            size: this.pageSize(),
            tipo: tipo !== 'TODOS' ? tipo : undefined,
            categoria: categoria !== 'TODAS' ? categoria : undefined,
            perfilId: perfil !== 'TODOS' ? perfil : undefined,
            fechaInicio: this.filtroFechaInicio() || undefined,
            fechaFin: this.filtroFechaFin() || undefined,
            busqueda: this.busqueda() || undefined
        }).subscribe({
            next: (data) => {
                this.movimientos.set(data.content || []);
                this.totalElements.set(data.totalElements || 0);
                this.totalPages.set(data.totalPages || 0);
                this.isLoading.set(false);
            },
            error: (err) => {
                console.error('❌ Error cargando movimientos:', err);
                this.isLoading.set(false);
            }
        });
    }

    // ========== MÉTODOS DE FILTRO ==========

    aplicarFiltros(): void {
        this.currentPage.set(0);
        this.cargarMovimientos();
    }

    limpiarFiltros(): void {
        this.filtroTipo.set('TODOS');
        this.filtroCategoria.set('TODAS');
        this.filtroPerfil.set('TODOS');
        this.filtroFechaInicio.set('');
        this.filtroFechaFin.set('');
        this.busqueda.set('');
        this.currentPage.set(0);
        this.cargarMovimientos();
    }

    cambiarPagina(page: number): void {
        if (page >= 0 && page < this.totalPages()) {
            this.currentPage.set(page);
            this.cargarMovimientos();
        }
    }

    // ========== UTILIDADES ==========

    formatCurrency(value: number): string {
        return new Intl.NumberFormat('es-ES', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2
        }).format(value);
    }

    getCategoriaLabel(categoria: string): string {
        const labels: Record<string, string> = {
            'ALIMENTACION': '🍎 Alimentación',
            'TRANSPORTE': '🚗 Transporte',
            'VIVIENDA': '🏠 Vivienda',
            'SERVICIOS': '📡 Servicios',
            'EDUCACION': '📚 Educación',
            'SALUD': '🏥 Salud',
            'ENTRETENIMIENTO': '🎮 Entretenimiento',
            'AHORRO': '🏦 Ahorro',
            'INVERSION': '📈 Inversión',
            'SALARIO': '💰 Salario',
            'IMPUESTOS': '📋 Impuestos',
            'OTROS': '📦 Otros'
        };
        return labels[categoria] || categoria;
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

    getTipoBgColor(tipo: string): string {
        const colors: Record<string, string> = {
            'INGRESO': 'bg-success-subtle',
            'GASTO': 'bg-danger-subtle',
            'TRANSFERENCIA': 'bg-warning-subtle'
        };
        return colors[tipo] || 'bg-secondary-subtle';
    }

    getFormattedDate(date: string): string {
        return new Date(date).toLocaleDateString('es-ES', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
    }

    eliminarMovimiento(id: number): void {
        if (confirm('¿Estás seguro de que deseas eliminar este movimiento?')) {
            this.movimientoService.eliminar(id).subscribe({
                next: () => {
                    console.log('✅ Movimiento eliminado exitosamente');
                    this.cargarMovimientos();
                },
                error: (err: any) => {
                    console.error('❌ Error al eliminar:', err);
                }
            });
        }
    }

    // ========== PAGINACIÓN ==========
    getPages(): number[] {
        const total = this.totalPages();
        const current = this.currentPage();
        const pages: number[] = [];
        const maxVisible = 5;

        if (total <= maxVisible) {
            for (let i = 0; i < total; i++) {
                pages.push(i);
            }
        } else {
            const start = Math.max(0, current - 2);
            const end = Math.min(total - 1, current + 2);

            if (start > 0) {
                pages.push(0);
                if (start > 1) pages.push(-1); // ...
            }

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }

            if (end < total - 1) {
                if (end < total - 2) pages.push(-1); // ...
                pages.push(total - 1);
            }
        }

        return pages;
    }
}