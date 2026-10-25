// src/app/pages/login/login.ts

import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../../core/services/auth-service';
import { Router } from '@angular/router';
import { LoginRequest } from '../../dto/LoginRequest.dto';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [ReactiveFormsModule, CommonModule],
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
    tipoCuenta = signal<string>('FAMILIA');

    // ========== FORMULARIOS ==========
    loginForm: FormGroup;
    registroForm: FormGroup;

    // ========== TIPOS DE CUENTA ==========
    tiposCuenta = [
        {
            value: 'FAMILIA',
            label: '🏠 Familia',
            descripcion: 'Gestiona las finanzas de tu familia'
        },
        {
            value: 'EMPRESA',
            label: '🏢 Empresa',
            descripcion: 'Gestiona las finanzas de tu empresa'
        }
    ];

    // ========== COMPUTED ==========
    isLoginFormInvalid = computed(() => {
        return !this.loginForm || this.loginForm.invalid || this.isLoading();
    });

    isRegistroFormInvalid = computed(() => {
        return !this.registroForm || this.registroForm.invalid || this.isLoading();
    });

    constructor() {
        // ========== FORMULARIO DE LOGIN ==========
        this.loginForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]]
        }, { updateOn: 'blur' });

        // ========== FORMULARIO DE REGISTRO SIMPLIFICADO ==========
        this.registroForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            passwordConfirm: ['', [Validators.required]],
            telefono: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s]{9,15}$/)]],
            tipo: ['FAMILIA', [Validators.required]]
        }, {
            validators: this.passwordMatchValidator
        });

        // Escuchar cambios en el tipo
        this.registroForm.get('tipo')?.valueChanges.subscribe(value => {
            this.tipoCuenta.set(value);
        });

        // Efecto para limpiar errores
        effect(() => {
            if (this.loginForm?.dirty || this.registroForm?.dirty) {
                this.error.set(null);
            }
        });
    }

    ngOnInit(): void {
        if (this.authService.isAuthenticated()) {
            this.router.navigate(['/home']);
        }
    }

    // ========== VALIDADOR ==========
    passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
        const password = group.get('password')?.value;
        const confirm = group.get('passwordConfirm')?.value;
        if (!password || !confirm) return null;
        return password === confirm ? null : { passwordMismatch: true };
    }

    // ========== MÉTODOS DE UI ==========
    mostrarRegistro(): void {
        this.isRegister.set(true);
        this.error.set(null);
    }

    mostrarLogin(): void {
        this.isRegister.set(false);
        this.error.set(null);
    }

    togglePanel(): void {
        this.isRegister.update(val => !val);
    }

    seleccionarTipo(tipo: string): void {
        this.registroForm.patchValue({ tipo });
        this.tipoCuenta.set(tipo);
    }

    // ========== LOGIN ==========
    login(): void {
        if (this.loginForm.invalid) {
            Object.keys(this.loginForm.controls).forEach(key => {
                this.loginForm.get(key)?.markAsTouched();
            });
            this.error.set('Por favor, completa todos los campos correctamente');
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

    // ========== REGISTRO SIMPLIFICADO ==========
    registrar(): void {
        if (this.registroForm.invalid) {
            Object.keys(this.registroForm.controls).forEach(key => {
                this.registroForm.get(key)?.markAsTouched();
            });
            this.error.set('Por favor, completa todos los campos correctamente');
            return;
        }

        this.isLoading.set(true);
        this.error.set(null);

        const formValue = this.registroForm.value;

        // ✅ Generar nombre del grupo automáticamente
        const nombreGrupo = formValue.tipo === 'FAMILIA'
            ? `Familia ${formValue.email.split('@')[0]}`
            : `Empresa ${formValue.email.split('@')[0]}`;

        const request = {
            email: formValue.email,
            password: formValue.password,
            nombre: formValue.email.split('@')[0],  // Usar parte del email como nombre
            telefono: formValue.telefono || '',
            roleName: formValue.tipo,  // ✅ FAMILIA o EMPRESA
            nombreGrupo: nombreGrupo,
            descripcionGrupo: '',
            imgUser: ''
        };

        console.log('📝 Enviando registro:', request);

        this.authService.registrar(request).subscribe({
            next: (response) => {
                this.isLoading.set(false);
                console.log('✅ Registro exitoso:', response);
                this.mostrarLogin();
                this.loginForm.patchValue({
                    email: formValue.email,
                    password: formValue.password
                });
            },
            error: (err) => {
                this.isLoading.set(false);
                console.error('❌ Error en registro:', err);
                let errorMsg = 'Error al registrar. Intenta nuevamente.';
                if (err.error?.message) {
                    errorMsg = err.error.message;
                } else if (err.status === 409) {
                    errorMsg = 'El email ya está registrado. Usa otro email.';
                } else if (err.status === 400) {
                    errorMsg = 'Datos inválidos. Verifica la información.';
                }
                this.error.set(errorMsg);
            }
        });
    }

    // ========== GETTERS LOGIN ==========
    get emailControl() { return this.loginForm.get('email'); }
    get passwordControl() { return this.loginForm.get('password'); }

    // ========== GETTERS REGISTRO ==========
    get regEmailControl() { return this.registroForm.get('email'); }
    get regPasswordControl() { return this.registroForm.get('password'); }
    get passwordConfirmControl() { return this.registroForm.get('passwordConfirm'); }
    get telefonoControl() { return this.registroForm.get('telefono'); }
}