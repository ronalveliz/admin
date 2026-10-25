// src/app/app.component.ts

import { Component, Renderer2, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { SidebarComponent } from './components/sidebar/sidebar';
import { AuthService } from './core/services/auth-service';


@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, SidebarComponent, CommonModule],
    templateUrl: './app.html',
    styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
    private authService = inject(AuthService);
    private router = inject(Router);
    private renderer = inject(Renderer2);
    private platformId = inject(PLATFORM_ID);

    title = 'cuentas-hogar-front';
    isSidebarCollapsed = false;
    isAuthenticated = signal(this.authService.isAuthenticated());

    ngOnInit(): void {
        // Escuchar cambios en autenticación
        this.authService.currentUser$.subscribe(user => {
            this.isAuthenticated.set(!!user);
            if (!user && isPlatformBrowser(this.platformId)) {
                // Si no hay usuario, remover clases del sidebar
                this.renderer.removeClass(document.body, 'sidebar-collapsed');
            }
        });
    }

    onSidebarCollapsed(collapsed: boolean): void {
        this.isSidebarCollapsed = collapsed;
        if (!isPlatformBrowser(this.platformId)) {
            return;
        }
        if (collapsed) {
            this.renderer.addClass(document.body, 'sidebar-collapsed');
        } else {
            this.renderer.removeClass(document.body, 'sidebar-collapsed');
        }
    }
}