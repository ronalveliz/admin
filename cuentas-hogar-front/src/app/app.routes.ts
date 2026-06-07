import { Routes } from '@angular/router';
import { Login } from './login/login';
import { UsuariosForm } from './usuarios-form/usuarios-form';


export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: Login
  },
{
    path: 'register',
    component: UsuariosForm
}


];
