// src/app/pages/movimientos/movimiento-form/movimiento-form.component.ts

import { Component, OnInit, inject, signal, computed, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';
import { MovimientoRequest } from '../../../dto/MovimientoRequest.dto';
import { MovimientoService } from '../../../core/services/movimiento-service';
import { PerfilService } from '../../../core/services/perfil-service';
import { AuthService } from '../../../core/services/auth-service';

@Component({
    selector: 'app-movimiento-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './movimiento-form.html',
    styleUrls: ['./movimiento-form.css']
})
export class MovimientoForm implements OnInit {
    // ========== INYECCIÓN ==========
    private fb = inject(FormBuilder);
    private movimientoService = inject(MovimientoService);
    private perfilService = inject(PerfilService);
    authService = inject(AuthService);
    router = inject(Router);
    private route = inject(ActivatedRoute);

    // ========== SIGNALS ==========
    isLoading = signal<boolean>(false);
    isEditing = signal<boolean>(false);
    movimientoId = signal<number | null>(null);
    error = signal<string | null>(null);
    success = signal<string | null>(null);

    // ========== FORMULARIO ==========
    movimientoForm: FormGroup;

    // Signals que dependen del form: se inicializan en el constructor
    private formStatus!: Signal<string>;
    isFormValid!: Signal<boolean>;
    categoriasFiltradas!: Signal<{ value: string; label: string; tipo: string }[]>;

    // ========== DATOS PARA SELECTS ==========
    perfiles = signal<any[]>([]);

    tipos = [
        { value: 'INGRESO', label: '💰 Ingreso' },
        { value: 'GASTO', label: '💸 Gasto' },
        { value: 'TRANSFERENCIA', label: '🔄 Transferencia' }
    ];

    categorias = [
        { value: 'SALARIO', label: '💰 Salario', tipo: 'INGRESO' },
        { value: 'AHORRO', label: '🏦 Ahorro', tipo: 'INGRESO' },
        { value: 'INVERSION', label: '📈 Inversión', tipo: 'INGRESO' },
        { value: 'OTROS_INGRESOS', label: '📦 Otros Ingresos', tipo: 'INGRESO' },

        { value: 'ALIMENTACION', label: '🍎 Alimentación', tipo: 'GASTO' },
        { value: 'TRANSPORTE', label: '🚗 Transporte', tipo: 'GASTO' },
        { value: 'VIVIENDA', label: '🏠 Vivienda', tipo: 'GASTO' },
        { value: 'SERVICIOS', label: '📡 Servicios', tipo: 'GASTO' },
        { value: 'EDUCACION', label: '📚 Educación', tipo: 'GASTO' },
        { value: 'SALUD', label: '🏥 Salud', tipo: 'GASTO' },
        { value: 'ENTRETENIMIENTO', label: '🎮 Entretenimiento', tipo: 'GASTO' },
        { value: 'IMPUESTOS', label: '📋 Impuestos', tipo: 'GASTO' },
        { value: 'OTROS_GASTOS', label: '📦 Otros Gastos', tipo: 'GASTO' }
    ];

    // ========== TÍTULO ==========
    get titulo(): string {
        const tipo = this.movimientoForm?.get('tipo')?.value;
        if (this.isEditing()) {
            return `✏️ Editar ${tipo === 'INGRESO' ? 'Ingreso' : 'Movimiento'}`;
        }
        return tipo === 'INGRESO' ? '💰 Nuevo Ingreso' : '💸 Nuevo Gasto';
    }

    constructor() {
        // 1. Crear el formulario PRIMERO
        this.movimientoForm = this.fb.group({
            tipo: ['INGRESO', [Validators.required]],
            monto: ['', [Validators.required, Validators.min(0.01)]],
            categoria: ['', [Validators.required]],
            descripcion: ['', [Validators.required, Validators.minLength(3)]],
            fecha: [new Date().toISOString().split('T')[0], [Validators.required]],
            perfilId: [null]
        });

        // 2. Ahora ya podemos leer this.movimientoForm
        this.formStatus = toSignal(
            this.movimientoForm.statusChanges.pipe(startWith(this.movimientoForm.status)),
            { initialValue: this.movimientoForm.status }
        );

        this.isFormValid = computed(() =>
            this.formStatus() === 'VALID' && !this.isLoading()
        );

        // Signal reactiva del valor "tipo" para categoriasFiltradas
        const tipoValue = toSignal(
            this.movimientoForm.get('tipo')!.valueChanges.pipe(
                startWith(this.movimientoForm.get('tipo')!.value)
            ),
            { initialValue: this.movimientoForm.get('tipo')!.value }
        );

        this.categoriasFiltradas = computed(() => {
            const tipo = tipoValue();
            if (!tipo) return [];
            return this.categorias.filter(c => c.tipo === tipo);
        });

        // 3. Suscripción: al cambiar tipo, resetea categoría
        this.movimientoForm.get('tipo')?.valueChanges.subscribe(() => {
            this.movimientoForm.patchValue({ categoria: '' });
        });
    }

    ngOnInit(): void {
        this.cargarPerfiles();

        const id = this.route.snapshot.params['id'];
        if (id) {
            this.isEditing.set(true);
            this.movimientoId.set(Number(id));
            this.cargarMovimiento(Number(id));
        }
    }

    // ========== CARGA DE DATOS ==========

    cargarPerfiles(): void {
        this.perfilService.getMisPerfiles().subscribe({
            next: (perfiles: any) => {
                this.perfiles.set(perfiles);
            },
            error: (err: any) => {
                console.error('❌ Error cargando perfiles:', err);
            }
        });
    }

    cargarMovimiento(id: number): void {
        this.isLoading.set(true);
        this.movimientoService.getMovimiento(id).subscribe({
            next: (mov: any) => {
                this.movimientoForm.patchValue({
                    tipo: mov.tipo,
                    monto: mov.monto,
                    categoria: mov.categoria,
                    descripcion: mov.descripcion,
                    fecha: mov.fecha,
                    perfilId: mov.perfilId
                });
                this.isLoading.set(false);
            },
            error: (err: any) => {
                this.isLoading.set(false);
                this.error.set('Error al cargar el movimiento');
                console.error('❌ Error:', err);
            }
        });
    }

    // ========== GUARDAR ==========

    onSubmit(): void {
        if (this.movimientoForm.invalid) {
            Object.keys(this.movimientoForm.controls).forEach(key => {
                this.movimientoForm.get(key)?.markAsTouched();
            });
            return;
        }

        this.isLoading.set(true);
        this.error.set(null);
        this.success.set(null);

        const formValue = this.movimientoForm.value;
        const request: MovimientoRequest = {
            monto: formValue.monto,
            tipo: formValue.tipo,
            categoria: formValue.categoria,
            descripcion: formValue.descripcion,
            fecha: formValue.fecha,
            perfilId: formValue.perfilId || undefined
        };

        const operation = this.isEditing()
            ? this.movimientoService.actualizar(this.movimientoId()!, request)
            : this.movimientoService.crear(request);

        operation.subscribe({
            next: (_response: any) => {
                this.isLoading.set(false);
                this.success.set(
                    this.isEditing()
                        ? '✅ Movimiento actualizado exitosamente'
                        : '✅ Movimiento creado exitosamente'
                );

                setTimeout(() => {
                    this.router.navigate(['/movimientos']);
                }, 1500);
            },
            error: (err: any) => {
                this.isLoading.set(false);
                let errorMsg = 'Error al guardar el movimiento';
                if (err.error?.message) {
                    errorMsg = err.error.message;
                }
                this.error.set(errorMsg);
                console.error('❌ Error:', err);
            }
        });
    }

    // ========== UTILIDADES ==========

    formatCurrency(value: number): string {
        return new Intl.NumberFormat('es-ES', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2
        }).format(value);
    }

    getErrorMessage(controlName: string): string {
        const control = this.movimientoForm.get(controlName);
        if (control?.touched && control?.invalid) {
            if (control.hasError('required')) {
                return 'Este campo es obligatorio';
            }
            if (control.hasError('min')) {
                return 'El monto debe ser mayor a 0';
            }
            if (control.hasError('minlength')) {
                return 'Mínimo 3 caracteres';
            }
        }
        return '';
    }

    // ========== GETTERS ==========
    get montoControl() {
        return this.movimientoForm.get('monto');
    }

    get descripcionControl() {
        return this.movimientoForm.get('descripcion');
    }

    get fechaControl() {
        return this.movimientoForm.get('fecha');
    }
}