import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication/authentication.service';
import { Token } from '../dto/token';
import { LoginRequest } from '../dto/LoginRequest';
import { UsuariosForm } from "../usuarios-form/usuarios-form";
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, UsuariosForm, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
isRegister = signal(true);

  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private authService = inject(AuthenticationService);
  private router = inject(Router);

  errorMessage = signal<string>('');

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });  

  save(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.errorMessage.set('Por favor, completa todos los campos correctamente');
      return;
    }

    const login: LoginRequest = {
      email: this.loginForm.value.email ?? '',
      password: this.loginForm.value.password ?? ''
    };

    this.http.post<Token>('http://localhost:8080/users/login', login)
      .subscribe({
        next: (response) => {
          this.authService.saveToken(response.token);
          this.router.navigate(['/home']);
        },
        error: (err) => {
          console.error('Error en login:', err);
          if (err.status === 403 || err.status === 401) {
            this.errorMessage.set('El usuario o la contraseña son incorrectos');
          } else if (err.status === 0) {
            this.errorMessage.set('No se pudo conectar con el servidor');
          } else {
            this.errorMessage.set('Ha ocurrido un error al iniciar sesión');
          }
          
          setTimeout(() => {
            this.errorMessage.set('');
          }, 5000);
        }
      });
  }
  
  mostrarRegistro() {
    this.isRegister.set(true);
  }

  mostrarLogin() {
    this.isRegister.set(false);
  }

  // Getters para validaciones en el template
  get emailInvalid() {
    return this.loginForm.get('email')?.invalid && this.loginForm.get('email')?.touched;
  }

  get passwordInvalid() {
    return this.loginForm.get('password')?.invalid && this.loginForm.get('password')?.touched;
  }
}