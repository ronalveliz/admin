import { Component, computed, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth-service';

export function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirm = control.get('passwordConfirm');
    if (password && confirm && password.value !== confirm.value) {
        return { passwordMismatch: true };
    }
    return null;
}
@Component({
  selector: 'app-usuarios-form',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './usuarios-form.html',
  styleUrl: './usuarios-form.css',
})
export class UsuariosForm {
  // ========== INYECCIÓN ==========
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);

    // ========== INPUTS Y OUTPUTS ==========
    isLoading = input<boolean>(false);
    error = input<string | null>(null);
    registerSuccess = output<void>();
    registerError = output<string>();

    // ========== SIGNALS ==========
    showPassword = signal<boolean>(false);
    showConfirmPassword = signal<boolean>(false);
    selectedRole = signal<string>('FAMILIA');

    // ========== FORMULARIO ==========
    usuariosForm: FormGroup;

    // ========== ROLES ==========
    roles = [
        { value: 'FAMILIA', label: '🏠 Familia' },
        { value: 'EMPRESA', label: '🏢 Empresa' },
        { value: 'ADMINISTRADOR', label: '👑 Administrador' }
    ];

    // ========== COMPUTED ==========
    isFormValid = computed(() => {
        return this.usuariosForm?.valid && !this.isLoading();
    });

    get nombreControl() {
        return this.usuariosForm?.get('nombre');
    }

    get emailControl() {
        return this.usuariosForm?.get('email');
    }

    get passwordControl() {
        return this.usuariosForm?.get('password');
    }

    get passwordConfirmControl() {
        return this.usuariosForm?.get('passwordConfirm');
    }

    get telefonoControl() {
        return this.usuariosForm?.get('telefono');
    }

    constructor() {
        this.usuariosForm = this.fb.group({
            nombre: ['', [Validators.required, Validators.minLength(3)]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            passwordConfirm: ['', [Validators.required]],
            telefono: ['', [Validators.pattern(/^[0-9+\-\s]{9,15}$/)]],
            roleName: ['FAMILIA', [Validators.required]],
            nombreGrupo: [''],
            descripcionGrupo: ['']
        }, { validators: passwordMatchValidator });

        // Escuchar cambios en el rol
        this.usuariosForm.get('roleName')?.valueChanges.subscribe(value => {
            this.selectedRole.set(value);
            this.updateGrupoValidators(value);
        });
    }

    // ========== MÉTODOS ==========

    updateGrupoValidators(role: string): void {
        const nombreGrupoControl = this.usuariosForm.get('nombreGrupo');
        const descripcionGrupoControl = this.usuariosForm.get('descripcionGrupo');

        if (role === 'FAMILIA' || role === 'EMPRESA') {
            nombreGrupoControl?.setValidators([Validators.required]);
            nombreGrupoControl?.updateValueAndValidity();
            descripcionGrupoControl?.setValidators([]);
            descripcionGrupoControl?.updateValueAndValidity();
        } else {
            nombreGrupoControl?.clearValidators();
            nombreGrupoControl?.updateValueAndValidity();
            descripcionGrupoControl?.clearValidators();
            descripcionGrupoControl?.updateValueAndValidity();
        }
    }

    onSubmit(): void {
        if (this.usuariosForm.invalid) {
            Object.keys(this.usuariosForm.controls).forEach(key => {
                this.usuariosForm.get(key)?.markAsTouched();
            });
            return;
        }

        const formValue = this.usuariosForm.value;

        const userData = {
            email: formValue.email,
            password: formValue.password,
            nombre: formValue.nombre,
            telefono: formValue.telefono || '',
            roleName: formValue.roleName,
            nombreGrupo: formValue.nombreGrupo || undefined,
            descripcionGrupo: formValue.descripcionGrupo || undefined
        };

        this.authService.registrar(userData).subscribe({
            next: (response) => {
                console.log('✅ Registro exitoso:', response);
                this.registerSuccess.emit();
            },
            error: (err) => {
                console.error('❌ Error en registro:', err);
                let errorMsg = 'Error al registrar. Intenta nuevamente.';
                if (err.error?.message) {
                    errorMsg = err.error.message;
                } else if (err.status === 409) {
                    errorMsg = 'El email ya está registrado. Usa otro email.';
                }
                this.registerError.emit(errorMsg);
            }
        });
    }

    togglePasswordVisibility(): void {
        this.showPassword.update(value => !value);
    }

    toggleConfirmPasswordVisibility(): void {
        this.showConfirmPassword.update(value => !value);
    }

    // ========== GETTERS PARA ERRORES ==========

    get nombreError(): string | null {
        const control = this.nombreControl;
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) return 'El nombre es obligatorio';
            if (control.hasError('minlength')) return 'El nombre debe tener al menos 3 caracteres';
        }
        return null;
    }

    get emailError(): string | null {
        const control = this.emailControl;
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) return 'El email es obligatorio';
            if (control.hasError('email')) return 'Ingresa un email válido';
        }
        return null;
    }

    get passwordError(): string | null {
        const control = this.passwordControl;
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) return 'La contraseña es obligatoria';
            if (control.hasError('minlength')) return 'La contraseña debe tener al menos 6 caracteres';
        }
        return null;
    }

    get passwordConfirmError(): string | null {
        const control = this.passwordConfirmControl;
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) return 'Confirma tu contraseña';
        }
        if (this.usuariosForm?.hasError('passwordMismatch') && control?.touched) {
            return 'Las contraseñas no coinciden';
        }
        return null;
    }

    get telefonoError(): string | null {
        const control = this.telefonoControl;
        if (control?.touched && control?.invalid) {
            if (control.hasError('pattern')) return 'Ingresa un teléfono válido (ej: +34 612345678)';
        }
        return null;
    }

    get nombreGrupoError(): string | null {
        const control = this.usuariosForm.get('nombreGrupo');
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) return 'El nombre del grupo es obligatorio para este rol';
        }
        return null;
    }
    
}
