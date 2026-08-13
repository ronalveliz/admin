import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/services/auth-service';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Verificar si el usuario está autenticado
    if (authService.isAuthenticated()) {
        return true;
    }

    // Redirigir al login con la URL de retorno
    return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
    });
};
