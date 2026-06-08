import { Routes } from '@angular/router';
import { Login } from './login/login';
import { UsuariosForm } from './usuarios-form/usuarios-form';
import { Home } from './home/home';


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
  },
  {
    path: 'home',
    component: Home
  }



];
