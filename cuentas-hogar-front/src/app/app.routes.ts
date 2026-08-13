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
        loadComponent: () => import('./pages/login/login').then(m => m.Login)
    },
    {
        path: 'registro',
        loadComponent: () => import('./pages/registro/registro').then(m => m.Registro)
    },
    {
        path: 'home',
        loadComponent: () => import('./pages/home/home').then(m => m.Home),
        canActivate: [authGuard]
    },
    {
        path: 'perfiles',
        canActivate: [authGuard, adminGuard],
        children: [
            {
                path: '',
                loadComponent: () => import('./pages/perfiles/perfil-list/perfil-list')
                    .then(m => m.PerfilList)
            },
            {
                path: 'nuevo',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form')
                    .then(m => m.PerfilForm)
            },
            {
                path: ':id',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form')
                    .then(m => m.PerfilForm)
            },
            {
                path: ':id/editar',
                loadComponent: () => import('./pages/perfiles/perfil-form/perfil-form')
                    .then(m => m.PerfilForm)
            }
        ]
    },
    {
        path: 'movimientos',
        canActivate: [authGuard],
        children: [
            {
                path: '',
                loadComponent: () => import('./pages/movimientos/movimiento-list/movimiento-list')
                    .then(m => m.MovimientoList)
            },
            {
                path: 'nuevo',
                loadComponent: () => import('./pages/movimientos/movimiento-form/movimiento-form')
                    .then(m => m.MovimientoForm)
            },
            {
                path: ':id',
                loadComponent: () => import('./pages/movimientos/movimiento-form/movimiento-form')
                    .then(m => m.MovimientoForm)
            }
        ]
    },
    {
        path: 'usuarios',
        loadComponent: () => import('./pages/usuarios-form/usuarios-form')
            .then(m => m.UsuariosForm),
        canActivate: [authGuard, adminGuard]
    },
    {
        path: 'not-found',
        loadComponent: () => import('./pages/not-found/not-found')
            .then(m => m.NotFound)
    },
    {
        path: '**',
        redirectTo: '/not-found'
    }
];