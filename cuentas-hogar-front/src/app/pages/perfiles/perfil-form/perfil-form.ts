// src/app/pages/perfiles/perfil-form/perfil-form.component.ts

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PerfilService } from '../../../core/services/perfil-service';
import { AuthService } from '../../../core/services/auth-service';
import { PerfilRequest } from '../../../dto/PerfilRequest.dto';

@Component({
    selector: 'app-perfil-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './perfil-form.html',
    styleUrls: ['./perfil-form.css']
})
export class PerfilFormComponent implements OnInit {
    // ========== INYECCIÓN ==========
    private fb = inject(FormBuilder);
    private perfilService = inject(PerfilService);
    private authService = inject(AuthService);
    public router = inject(Router);
    private route = inject(ActivatedRoute);

    // ========== SIGNALS ==========
    isLoading = signal<boolean>(false);
    isEditing = signal<boolean>(false);
    perfilId = signal<number | null>(null);
    error = signal<string | null>(null);
    success = signal<string | null>(null);

    // ========== FORMULARIO ==========
    perfilForm: FormGroup;

    // ========== DATOS PARA SELECTS ==========
    tipos = [
        { value: 'PERSONAL', label: '👤 Personal' },
        { value: 'MIEMBRO', label: '👥 Miembro' },
        { value: 'EMPLEADO', label: '👔 Empleado' }
    ];

    // ========== COMPUTED ==========
    isAdmin = computed(() => {
        const user = this.authService.getCurrentUser();
        return user?.usuario?.rol === 'ADMINISTRADOR' ||
               user?.usuario?.rol === 'FAMILIA' ||
               user?.usuario?.rol === 'EMPRESA';
    });

    // Getter para el título
    get titulo(): string {
        return this.isEditing() ? '✏️ Editar Perfil' : '👤 Nuevo Perfil';
    }

    // Getter de validez (se evalúa en cada ciclo de detección)
    get isFormValid(): boolean {
        return this.perfilForm?.valid === true && !this.isLoading();
    }

    // ========== CONSTRUCTOR ==========
    constructor() {
        this.perfilForm = this.fb.group({
            nombre: ['', [Validators.required, Validators.minLength(2)]],
            apellidos: [''],
            email: ['', [Validators.required, Validators.email]],
            telefono: ['', [Validators.pattern(/^[0-9+\-\s]{9,15}$/)]],
            direccion: [''],
            fotoPerfil: [''],
            fechaNacimiento: [''],
            tipo: ['', [Validators.required]]
        });
    }

    // ========== NGONINIT ==========
    ngOnInit(): void {
        if (!this.isAdmin()) {
            this.router.navigate(['/perfiles']);
            return;
        }

        const id = this.route.snapshot.params['id'];
        if (id) {
            this.isEditing.set(true);
            this.perfilId.set(Number(id));
            this.cargarPerfil(Number(id));
        }
    }

    // ========== CARGA DE DATOS ==========

    cargarPerfil(id: number): void {
        this.isLoading.set(true);
        this.perfilService.getPerfil(id).subscribe({
            next: (perfil) => {
                this.perfilForm.patchValue({
                    nombre: perfil.nombre,
                    apellidos: perfil.apellidos || '',
                    email: perfil.email || '',
                    telefono: perfil.telefono || '',
                    direccion: perfil.direccion || '',
                    fotoPerfil: perfil.fotoPerfil || '',
                    fechaNacimiento: perfil.fechaNacimiento || '',
                    tipo: perfil.tipo
                });
                this.isLoading.set(false);
            },
            error: (err) => {
                this.isLoading.set(false);
                this.error.set('Error al cargar el perfil');
                console.error('❌ Error:', err);
            }
        });
    }

    // ========== GUARDAR ==========

    onSubmit(): void {
        if (this.perfilForm.invalid) {
            Object.keys(this.perfilForm.controls).forEach(key => {
                this.perfilForm.get(key)?.markAsTouched();
            });
            return;
        }

        this.isLoading.set(true);
        this.error.set(null);
        this.success.set(null);

        const formValue = this.perfilForm.value;
        const request: PerfilRequest = {
            nombre: formValue.nombre,
            apellidos: formValue.apellidos || undefined,
            email: formValue.email || undefined,
            telefono: formValue.telefono || undefined,
            direccion: formValue.direccion || undefined,
            fotoPerfil: formValue.fotoPerfil || undefined,
            fechaNacimiento: formValue.fechaNacimiento || undefined,
            tipo: formValue.tipo
        };

        const operation = this.isEditing()
            ? this.perfilService.actualizar(this.perfilId()!, request)
            : this.perfilService.crear(request);

        operation.subscribe({
            next: () => {
                this.isLoading.set(false);
                this.success.set(
                    this.isEditing()
                        ? '✅ Perfil actualizado exitosamente'
                        : '✅ Perfil creado exitosamente. Se enviarán las credenciales por email si el perfil tiene acceso independiente.'
                );

                setTimeout(() => {
                    this.router.navigate(['/perfiles']);
                }, 2000);
            },
            error: (err) => {
                this.isLoading.set(false);
                let errorMsg = 'Error al guardar el perfil';
                if (err.error?.message) {
                    errorMsg = err.error.message;
                }
                this.error.set(errorMsg);
                console.error('❌ Error:', err);
            }
        });
    }

    // ========== UTILIDADES ==========

    getErrorMessage(controlName: string): string {
        const control = this.perfilForm.get(controlName);
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) {
                return 'Este campo es obligatorio';
            }
            if (control.hasError('minlength')) {
                return 'Mínimo 2 caracteres';
            }
            if (control.hasError('email')) {
                return 'Ingresa un email válido';
            }
            if (control.hasError('pattern')) {
                return 'Ingresa un teléfono válido';
            }
        }
        return '';
    }

    // ========== GETTERS ==========
    get nombreControl() { return this.perfilForm.get('nombre')!; }
    get emailControl() { return this.perfilForm.get('email')!; }
    get telefonoControl() { return this.perfilForm.get('telefono')!; }
    get tipoControl() { return this.perfilForm.get('tipo')!; }

    debugForm(): void {
        console.log('=== DEBUG FORMULARIO ===');
        console.log('Válido:', this.perfilForm.valid);
        console.log('Valor:', JSON.stringify(this.perfilForm.value, null, 2));

        Object.keys(this.perfilForm.controls).forEach(key => {
            const control = this.perfilForm.get(key);
            console.log(`🔍 ${key}:`, {
                valor: control?.value,
                valido: control?.valid,
                errores: control?.errors ? JSON.stringify(control.errors) : 'ninguno',
                touched: control?.touched
            });
        });
    }
}