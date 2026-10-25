// src/app/pages/perfiles/perfil-list/perfil-list.component.ts

import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PerfilService } from '../../../core/services/perfil-service';
import { AuthService } from '../../../core/services/auth-service';
import { PerfilResponse } from '../../../dto/PerfilResponse.dto';

@Component({
    selector: 'app-perfil-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './perfil-list.html',
    styleUrls: ['./perfil-list.css']
})
export class PerfilListComponent implements OnInit {
    // ========== INYECCIÓN ==========
    private perfilService = inject(PerfilService);
    private authService = inject(AuthService);

    // ========== SIGNALS ==========
    isLoading = signal<boolean>(true);
    perfiles = signal<PerfilResponse[]>([]);
    perfilesFiltrados = signal<PerfilResponse[]>([]);
    searchTerm = signal<string>('');
    filterTipo = signal<string>('TODOS');
    showInactivos = signal<boolean>(false);

    // ========== COMPUTED ==========
    isAdmin = computed(() => {
        const user = this.authService.getCurrentUser();
        return user?.usuario?.rol === 'ADMINISTRADOR' ||
               user?.usuario?.rol === 'FAMILIA' ||
               user?.usuario?.rol === 'EMPRESA';
    });

    tipos = [
        { value: 'TODOS', label: 'Todos' },
        { value: 'PERSONAL', label: '👤 Personal' },
        { value: 'MIEMBRO', label: '👥 Miembro' },
        { value: 'EMPLEADO', label: '👔 Empleado' }
    ];

    totalPerfiles = computed(() => this.perfiles().length);
    totalActivos = computed(() => this.perfiles().filter(p => p.activo).length);
    totalInactivos = computed(() => this.perfiles().filter(p => !p.activo).length);

    constructor() {
        // Efecto para filtrar perfiles
        effect(() => {
            const term = this.searchTerm().toLowerCase().trim();
            const tipo = this.filterTipo();
            const showInactivos = this.showInactivos();

            let filtered = this.perfiles();

            // Filtrar por búsqueda
            if (term) {
                filtered = filtered.filter(p =>
                    p.nombreCompleto.toLowerCase().includes(term) ||
                    p.email?.toLowerCase().includes(term) ||
                    p.tipo?.toLowerCase().includes(term)
                );
            }

            // Filtrar por tipo
            if (tipo !== 'TODOS') {
                filtered = filtered.filter(p => p.tipo === tipo);
            }

            // Filtrar por activos/inactivos
            if (!showInactivos) {
                filtered = filtered.filter(p => p.activo);
            }

            this.perfilesFiltrados.set(filtered);
        });
    }

    ngOnInit(): void {
        this.cargarPerfiles();
    }

    // ========== CARGA DE DATOS ==========

    cargarPerfiles(): void {
        this.isLoading.set(true);
        this.perfilService.getMisPerfiles().subscribe({
            next: (perfiles) => {
                this.perfiles.set(perfiles);
                this.isLoading.set(false);
            },
            error: (err) => {
                console.error('❌ Error cargando perfiles:', err);
                this.isLoading.set(false);
            }
        });
    }

    // ========== ACCIONES ==========

    cambiarEstado(perfil: PerfilResponse): void {
        if (!this.isAdmin()) return;

        const confirmMsg = perfil.activo
            ? `¿Estás seguro de que quieres deshabilitar el perfil "${perfil.nombreCompleto}"?`
            : `¿Estás seguro de que quieres habilitar el perfil "${perfil.nombreCompleto}"?`;

        if (!confirm(confirmMsg)) return;

        const operation = perfil.activo
            ? this.perfilService.deshabilitar(perfil.id)
            : this.perfilService.habilitar(perfil.id);

        operation.subscribe({
            next: () => {
                // Actualizar el perfil en la lista
                const updatedPerfiles = this.perfiles().map(p => {
                    if (p.id === perfil.id) {
                        return { ...p, activo: !p.activo };
                    }
                    return p;
                });
                this.perfiles.set(updatedPerfiles);
            },
            error: (err) => {
                console.error('❌ Error cambiando estado:', err);
                alert('Error al cambiar el estado del perfil');
            }
        });
    }

    eliminarPerfil(perfil: PerfilResponse): void {
        if (!this.isAdmin()) return;

        if (perfil.tipo === 'PERSONAL') {
            alert('No se puede eliminar el perfil personal');
            return;
        }

        if (!confirm(`¿Estás seguro de que quieres eliminar el perfil "${perfil.nombreCompleto}"?`)) return;

        this.perfilService.deshabilitar(perfil.id).subscribe({
            next: () => {
                const updatedPerfiles = this.perfiles().filter(p => p.id !== perfil.id);
                this.perfiles.set(updatedPerfiles);
            },
            error: (err) => {
                console.error('❌ Error eliminando perfil:', err);
                alert('Error al eliminar el perfil');
            }
        });
    }

    // ========== UTILIDADES ==========

    getTipoClass(tipo: string): string {
        const classes: Record<string, string> = {
            'PERSONAL': 'badge-primary',
            'MIEMBRO': 'badge-success',
            'EMPLEADO': 'badge-info'
        };
        return classes[tipo] || 'badge-secondary';
    }

    getTipoIcon(tipo: string): string {
        const icons: Record<string, string> = {
            'PERSONAL': 'fa-user',
            'MIEMBRO': 'fa-users',
            'EMPLEADO': 'fa-user-tie'
        };
        return icons[tipo] || 'fa-user';
    }

    getEstadoText(activo: boolean): string {
        return activo ? 'Activo' : 'Inactivo';
    }

    getEstadoClass(activo: boolean): string {
        return activo ? 'badge-success' : 'badge-danger';
    }

    formatCurrency(value: number): string {
        return new Intl.NumberFormat('es-ES', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2
        }).format(value);
    }

    getInitials(name: string): string {
        return name.split(' ')
            .map(w => w[0])
            .join('')
            .toUpperCase()
            .slice(0, 2);
    }

    getColorForPerfil(name: string): string {
        const colors = [
            '#667eea', '#764ba2', '#28a745', '#dc3545',
            '#ffc107', '#17a2b8', '#fd7e14', '#6f42c1'
        ];
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }
        return colors[Math.abs(hash) % colors.length];
    }

    // ========== REFRESH ==========
    refreshData(): void {
        this.cargarPerfiles();
    }
}