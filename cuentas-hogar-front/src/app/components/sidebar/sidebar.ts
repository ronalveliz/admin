// src/app/components/sidebar/sidebar.component.ts

import { Component, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth-service';


interface MenuItem {
    icon: string;
    label: string;
    route: string;
    roles: string[];
    children?: MenuItem[];
}

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './sidebar.html', 
    styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
    // ========== INYECCIÓN ==========
    private authService = inject(AuthService);
    private router = inject(Router);

    // ========== SIGNALS ==========
    isCollapsed = signal<boolean>(false);
    isMobile = signal<boolean>(typeof window !== 'undefined' ? window.innerWidth < 768 : false);
    expandedMenus = signal<Set<string>>(new Set());

    // ========== USUARIO ==========
    user = signal<any>(null);
    userInitials = computed(() => {
        const nombre = this.user()?.usuario?.nombre || 'Usuario';
        return nombre
            .split(' ')
            .map((word: string) => word[0])
            .join('')
            .toUpperCase()
            .slice(0, 2);
    });

    userName = computed(() => {
        return this.user()?.usuario?.nombre || 'Usuario';
    });

    userRol = computed(() => {
        const rol = this.user()?.usuario?.rol || '';
        const labels: Record<string, string> = {
            'ADMINISTRADOR': '👑 Administrador',
            'FAMILIA': '🏠 Familia',
            'EMPRESA': '🏢 Empresa',
            'MIEMBRO': '👤 Miembro',
            'EMPLEADO': '👤 Empleado'
        };
        return labels[rol] || rol;
    });

    // ========== MENÚ ==========
    menuItems: MenuItem[] = [
        {
            icon: 'fa-house',
            label: 'Dashboard',
            route: '/home',
            roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA', 'MIEMBRO', 'EMPLEADO']
        },
        {
            icon: 'fa-arrows-rotate',
            label: 'Movimientos',
            route: '/movimientos',
            roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA', 'MIEMBRO', 'EMPLEADO']
        },
        {
            icon: 'fa-users',
            label: 'Perfiles',
            route: '/perfiles',
            roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA']
        },
        {
            icon: 'fa-user-gear',
            label: 'Usuarios',
            route: '/usuarios',
            roles: ['ADMINISTRADOR']
        },
        {
            icon: 'fa-chart-simple',
            label: 'Reportes',
            route: '/reportes',
            roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA'],
            children: [
                { icon: 'fa-circle', label: 'Mensual', route: '/reportes/mensual', roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA'] },
                { icon: 'fa-circle', label: 'Anual', route: '/reportes/anual', roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA'] },
                { icon: 'fa-circle', label: 'Por Categoría', route: '/reportes/categorias', roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA'] }
            ]
        },
        {
            icon: 'fa-gear',
            label: 'Configuración',
            route: '/configuracion',
            roles: ['ADMINISTRADOR', 'FAMILIA', 'EMPRESA']
        }
    ];

    // ========== COMPUTED ==========
    filteredMenu = computed(() => {
        const userRol = this.user()?.usuario?.rol || '';
        return this.menuItems
            .filter(item => item.roles.includes(userRol))
            .map(item => ({
                ...item,
                children: item.children?.filter(child => child.roles.includes(userRol))
            }));
    });

    isAdmin = computed(() => {
        const user = this.user();
        return user?.usuario?.rol === 'ADMINISTRADOR' ||
               user?.usuario?.rol === 'FAMILIA' ||
               user?.usuario?.rol === 'EMPRESA';
    });

    // ========== CONSTRUCTOR ==========
    constructor() {
        // Cargar usuario
        this.user.set(this.authService.getCurrentUser());

        // Escuchar cambios en autenticación
        this.authService.currentUser$.subscribe(user => {
            this.user.set(user);
        });

        // Responsive (solo en navegador)
        if (typeof window !== 'undefined') {
            window.addEventListener('resize', () => {
                this.isMobile.set(window.innerWidth < 768);
                if (this.isMobile()) {
                    this.isCollapsed.set(true);
                }
            });
        }

        // Efecto para colapsar en móvil
        effect(() => {
            if (this.isMobile()) {
                this.isCollapsed.set(true);
            }
        });
    }

    // ========== MÉTODOS ==========

    toggleSidebar(): void {
        this.isCollapsed.update(v => !v);
    }

    toggleMenu(menuLabel: string): void {
        const current = this.expandedMenus();
        if (current.has(menuLabel)) {
            current.delete(menuLabel);
        } else {
            current.add(menuLabel);
        }
        this.expandedMenus.set(new Set(current));
    }

    isMenuExpanded(menuLabel: string): boolean {
        return this.expandedMenus().has(menuLabel);
    }

    isActive(route: string): boolean {
        return this.router.url === route;
    }

    isActiveChild(children: MenuItem[] | undefined): boolean {
        if (!children) return false;
        return children.some(child => this.router.url === child.route);
    }

    logout(): void {
        this.authService.logout();
        this.router.navigate(['/login']);
    }

    // ========== GETTERS ==========
    get isCollapsedState() {
        return this.isCollapsed();
    }

    get isMobileState() {
        return this.isMobile();
    }
}