import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/services/auth-service';

export const adminGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Verificar si está autenticado y es administrador
    if (authService.isAuthenticated() && authService.isAdmin()) {
        return true;
    }

    // Redirigir al home si no es administrador
    return router.createUrlTree(['/home']);
};
