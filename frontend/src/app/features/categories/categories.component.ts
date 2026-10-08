import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../../shared/api-error';
import { Category, CategoryRequest, CategoryType } from './category.models';
import { CategoryService } from './category.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.scss'
})
export class CategoriesComponent implements OnInit {
  readonly categoryTypes: CategoryType[] = ['EXPENSE', 'INCOME', 'TRANSFER', 'SYSTEM'];
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly editingCategory = signal<Category | null>(null);
  readonly formTitle = computed(() => this.editingCategory() ? 'Edit category' : 'Add category');

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    categoryType: ['EXPENSE' as CategoryType, [Validators.required]],
    color: ['#64748B', [Validators.maxLength(20)]],
    icon: ['circle-help', [Validators.maxLength(80)]],
    displayOrder: [0]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly categoryService: CategoryService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.categoryService.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toRequest();
    const editingCategory = this.editingCategory();
    const save$ = editingCategory
      ? this.categoryService.update(editingCategory.id, request)
      : this.categoryService.create(request);

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    save$.subscribe({
      next: () => {
        this.successMessage.set(editingCategory ? 'Category updated' : 'Category created');
        this.saving.set(false);
        this.resetForm();
        this.loadCategories();
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.saving.set(false);
      }
    });
  }

  edit(category: Category): void {
    this.editingCategory.set(category);
    this.form.setValue({
      name: category.name,
      categoryType: category.categoryType,
      color: category.color ?? '#64748B',
      icon: category.icon ?? 'circle-help',
      displayOrder: category.displayOrder
    });
  }

  delete(category: Category): void {
    if (category.defaultCategory) {
      this.errorMessage.set('Default categories cannot be deleted');
      return;
    }
    if (!confirm(`Delete ${category.name}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.categoryService.delete(category.id).subscribe({
      next: () => {
        this.successMessage.set('Category deleted');
        this.loadCategories();
      },
      error: (error) => this.errorMessage.set(apiErrorMessage(error))
    });
  }

  resetForm(): void {
    this.editingCategory.set(null);
    this.form.reset({
      name: '',
      categoryType: 'EXPENSE',
      color: '#64748B',
      icon: 'circle-help',
      displayOrder: 0
    });
  }

  private toRequest(): CategoryRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name.trim(),
      categoryType: value.categoryType,
      color: this.trimToNull(value.color),
      icon: this.trimToNull(value.icon),
      displayOrder: value.displayOrder
    };
  }

  private trimToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
