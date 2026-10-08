import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../../shared/api-error';
import { Category } from '../categories/category.models';
import { CategoryService } from '../categories/category.service';
import { MerchantRule, MerchantRuleRequest, RuleMatchType } from './merchant-rule.models';
import { MerchantRuleService } from './merchant-rule.service';

@Component({
  selector: 'app-merchant-rules',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './merchant-rules.component.html',
  styleUrl: './merchant-rules.component.scss'
})
export class MerchantRulesComponent implements OnInit {
  readonly matchTypes: RuleMatchType[] = ['CONTAINS', 'STARTS_WITH', 'ENDS_WITH', 'EXACT', 'REGEX'];
  readonly categories = signal<Category[]>([]);
  readonly rules = signal<MerchantRule[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly editingRule = signal<MerchantRule | null>(null);
  readonly formTitle = computed(() => this.editingRule() ? 'Edit merchant rule' : 'Add merchant rule');

  readonly form = this.formBuilder.nonNullable.group({
    keyword: ['', [Validators.required, Validators.maxLength(255)]],
    matchType: ['CONTAINS' as RuleMatchType, [Validators.required]],
    categoryId: ['', [Validators.required]],
    merchantName: ['', [Validators.maxLength(180)]],
    priority: [100],
    active: [true]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly categoryService: CategoryService,
    private readonly merchantRuleService: MerchantRuleService
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.categoryService.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        if (categories.length && !this.form.controls.categoryId.value) {
          this.form.controls.categoryId.setValue(categories[0].id);
        }
        this.loadRules(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  loadRules(showSpinner = true): void {
    if (showSpinner) {
      this.loading.set(true);
    }
    this.errorMessage.set(null);
    this.merchantRuleService.list().subscribe({
      next: (rules) => {
        this.rules.set(rules);
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
    const editingRule = this.editingRule();
    const save$ = editingRule
      ? this.merchantRuleService.update(editingRule.id, request)
      : this.merchantRuleService.create(request);

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    save$.subscribe({
      next: () => {
        this.successMessage.set(editingRule ? 'Rule updated' : 'Rule created');
        this.saving.set(false);
        this.resetForm();
        this.loadRules(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.saving.set(false);
      }
    });
  }

  edit(rule: MerchantRule): void {
    this.editingRule.set(rule);
    this.form.setValue({
      keyword: rule.keyword,
      matchType: rule.matchType,
      categoryId: rule.category.id,
      merchantName: rule.merchantName ?? '',
      priority: rule.priority,
      active: rule.active
    });
  }

  deactivate(rule: MerchantRule): void {
    if (!confirm(`Deactivate rule "${rule.keyword}"?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.merchantRuleService.delete(rule.id).subscribe({
      next: () => {
        this.successMessage.set('Rule deactivated');
        this.loadRules();
      },
      error: (error) => this.errorMessage.set(apiErrorMessage(error))
    });
  }

  resetForm(): void {
    this.editingRule.set(null);
    this.form.reset({
      keyword: '',
      matchType: 'CONTAINS',
      categoryId: this.categories()[0]?.id ?? '',
      merchantName: '',
      priority: 100,
      active: true
    });
  }

  private toRequest(): MerchantRuleRequest {
    const value = this.form.getRawValue();
    return {
      keyword: value.keyword.trim(),
      matchType: value.matchType,
      categoryId: value.categoryId,
      merchantName: this.trimToNull(value.merchantName),
      priority: value.priority,
      active: value.active
    };
  }

  private trimToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
