import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth-service';
import { Router } from '@angular/router';
import { LoginRequest } from '../../dto/LoginRequest.dto';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent implements OnInit {
    // ========== INYECCIÓN ==========
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);
    private router = inject(Router);

    // ========== SIGNALS ==========
    isRegister = signal<boolean>(false);
    isLoading = signal<boolean>(false);
    error = signal<string | null>(null);
    showPassword = signal<boolean>(false);

    // ========== FORMULARIOS ==========
    loginForm: FormGroup;
    usuariosForm: FormGroup;

    // ========== COMPUTED ==========
    isLoginFormInvalid = computed(() => {
        // CORREGIDO: Si loginForm no existe, devolvemos true. Si existe, validamos.
        return !this.loginForm || this.loginForm.invalid || this.isLoading();
    });

    constructor() {
        // ========== FORMULARIO DE LOGIN ==========
        // Añadimos updateOn: 'blur' para validar solo al perder el foco
        this.loginForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]]
        }, { updateOn: 'blur' });

        // ========== FORMULARIO DE REGISTRO ==========
        this.usuariosForm = this.fb.group({
            nombre: ['', [Validators.required, Validators.minLength(3)]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            passwordConfirm: ['', [Validators.required]],
            telefono: ['', [Validators.pattern(/^[0-9+\-\s]{9,15}$/)]]
        }, {
            validators: this.passwordMatchValidator
        });

        // Efecto para limpiar errores
        effect(() => {
            if (this.loginForm?.dirty) {
                this.error.set(null);
            }
        });
    }

    ngOnInit(): void {
        if (this.authService.isAuthenticated()) {
            this.router.navigate(['/home']);
        }
        
        // CORREGIDO: Forzamos la validación inicial al cargar el componente
        this.loginForm.markAllAsTouched();
    }

    // ========== VALIDADOR PERSONALIZADO SIN WARNINGS ==========
    passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
        const password = group.get('password')?.value;
        const confirm = group.get('passwordConfirm')?.value;
        if (!password || !confirm) return null; 
        return password === confirm ? null : { passwordMismatch: true };
    }

    // ========== MÉTODOS DE UI ==========
    mostrarRegistro(): void { this.isRegister.set(true); }
    mostrarLogin(): void { this.isRegister.set(false); }
    togglePasswordVisibility(): void { this.showPassword.update(value => !value); }

    // ========== LOGIN ==========
    login(): void {
        if (this.loginForm.invalid) {
            Object.keys(this.loginForm.controls).forEach(key => {
                this.loginForm.get(key)?.markAsTouched();
            });
            return;
        }

        this.isLoading.set(true);
        this.error.set(null);

        const request: LoginRequest = {
            email: this.loginForm.get('email')?.value,
            password: this.loginForm.get('password')?.value
        };

        this.authService.login(request).subscribe({
            next: () => {
                this.isLoading.set(false);
                this.router.navigate(['/home']);
            },
            error: (err) => {
                this.isLoading.set(false);
                let errorMsg = 'Error al iniciar sesión. Intenta nuevamente.';
                if (err.status === 401) errorMsg = 'Credenciales incorrectas. Verifica tu email y contraseña.';
                else if (err.status === 403) errorMsg = 'Usuario deshabilitado. Contacta al administrador.';
                else if (err.status === 404) errorMsg = 'Usuario no encontrado.';
                this.error.set(errorMsg);
            }
        });
    }

    // ========== REGISTRO ==========
    registrar(): void {
        if (this.usuariosForm.invalid) {
            Object.keys(this.usuariosForm.controls).forEach(key => {
                this.usuariosForm.get(key)?.markAsTouched();
            });
            return;
        }

        this.isLoading.set(true);
        this.error.set(null);

        const userData = {
            email: this.usuariosForm.get('email')?.value,
            password: this.usuariosForm.get('password')?.value,
            nombre: this.usuariosForm.get('nombre')?.value,
            telefono: this.usuariosForm.get('telefono')?.value || '',
            roleName: 'FAMILIA'
        };

        this.authService.registrar(userData).subscribe({
            next: (response) => {
                this.isLoading.set(false);
                console.log('✅ Registro exitoso:', response);
                this.mostrarLogin();
                this.loginForm.patchValue({
                    email: userData.email,
                    password: userData.password
                });
            },
            error: (err) => {
                this.isLoading.set(false);
                let errorMsg = 'Error al registrar. Intenta nuevamente.';
                if (err.error?.message) errorMsg = err.error.message;
                else if (err.status === 409) errorMsg = 'El email ya está registrado. Usa otro email.';
                this.error.set(errorMsg);
            }
        });
    }

    // ========== GETTERS SIN WARNINGS ==========
    get emailControl() { return this.loginForm.get('email'); }
    get passwordControl() { return this.loginForm.get('password'); }

    get emailErrors() {
        const control = this.loginForm.get('email');
        if (control && control.touched && control.invalid) {
            if (control.hasError('required')) return 'El email es obligatorio';
            if (control.hasError('email')) return 'Ingresa un email válido';
        }
        return null;
    }

    get passwordErrors() {
        const control = this.loginForm.get('password');
        if (control && control.touched && control.invalid) {
            if (control.hasError('required')) return 'La contraseña es obligatoria';
            if (control.hasError('minlength')) return 'La contraseña debe tener al menos 6 caracteres';
        }
        return null;
    }
}