import { Routes } from '@angular/router';
import { Login } from './login/login';
import { UsuariosForm } from './usuarios-form/usuarios-form';
import { Home } from './home/home';
import { NotFound } from './not-found/not-found';


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
  },
  {
    path: '**', // error a 404 page
    component: NotFound
  }

];
