// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { adminGuard } from './guards/admin-guard-guard';


export const routes: Routes = [
    {
        path: '',
        redirectTo: '/home',
        pathMatch: 'full'
    },
    {
        path: 'login',
        loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
    },
    {
        path: 'registro',
        loadComponent: () => import('./pages/registro/registro.component').then(m => m.RegistroComponent)
    },
    {
        path: 'home',
        loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
        canActivate: [authGuard]
    },
    {
        path: 'perfiles',
        canActivate: [authGuard, adminGuard],
        children: [
            {
                path: '',
                loadComponent: () => import('./pages/perfiles/perfil-list/perfil-list.component')
                    .then(m => m.PerfilListComponent)
            },
            {
                path: 'nuevo',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form.component')
                    .then(m => m.PerfilFormComponent)
            },
            {
                path: ':id',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form.component')
                    .then(m => m.PerfilFormComponent)
            },
            {
                path: ':id/editar',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form.component')
                    .then(m => m.PerfilFormComponent)
            }
        ]
    },
    {
        path: 'movimientos',
        canActivate: [authGuard],
        children: [
            {
                path: '',
                loadComponent: () => import('./pages/movimientos/movimiento-list/movimiento-list.component')
                    .then(m => m.MovimientoListComponent)
            },
            {
                path: 'nuevo',
                loadComponent: () => import('./pages/movimientos/movimiento-form/movimiento-form.component')
                    .then(m => m.MovimientoFormComponent)
            },
            {
                path: ':id',
                loadComponent: () => import('./pages/movimientos/movimiento-form/movimiento-form.component')
                    .then(m => m.MovimientoFormComponent)
            }
        ]
    },
    {
        path: 'usuarios',
        loadComponent: () => import('./pages/usuarios-form/usuarios-form.component')
            .then(m => m.UsuariosFormComponent),
        canActivate: [authGuard, adminGuard]
    },
    {
        path: 'not-found',
        loadComponent: () => import('./pages/not-found/not-found.component')
            .then(m => m.NotFoundComponent)
    },
    {
        path: '**',
        redirectTo: '/not-found'
    }
];