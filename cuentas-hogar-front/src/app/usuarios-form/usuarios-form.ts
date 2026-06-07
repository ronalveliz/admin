import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RolName } from '../interfaces/rolName';
import { User } from '../interfaces/user';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-usuarios-form',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './usuarios-form.html',
  styleUrl: './usuarios-form.css',
})
export class UsuariosForm {
  users: User[] = [];
  roles = RolName;

  private http = inject(HttpClient);
  private router = inject(Router);

  usuariosForm = new FormGroup({
    id: new FormControl(0),
    nombre: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(8), Validators.maxLength(30)]),
    passwordConfirm: new FormControl('', [Validators.required, Validators.minLength(8), Validators.maxLength(30)]),
    phone: new FormControl('', [Validators.required, Validators.pattern('^[0-9]{9}$')]), // Cambiado a string para pattern
    rolname: new FormControl<RolName>(RolName.USER)
  }, { validators: this.passwordConfirmValidator });

  passwordConfirmValidator(control: AbstractControl) {
    const password = control.get('password')?.value;
    const confirm = control.get('passwordConfirm')?.value;
    return password === confirm ? null : { confirmError: true };
  }

  save() {
    if (this.usuariosForm.invalid) {
      this.usuariosForm.markAllAsTouched();
      return;
    }

    const user: User = this.usuariosForm.value as unknown as User;
    console.log('Enviando usuario:', user);
    
    const url = 'http://localhost:8080/users/register';
    this.http.post<User>(url, user).subscribe({
      next: (backendUser) => {
        console.log('Usuario registrado:', backendUser);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Error en registro:', err);
        alert('Error al registrar: ' + (err.error?.message || 'Intenta nuevamente'));
      }
    });
  }
}