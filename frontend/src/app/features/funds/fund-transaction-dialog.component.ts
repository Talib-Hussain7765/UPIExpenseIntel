import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

export interface FundTransactionDialogData {
  fundName: string;
  type: 'ADD' | 'WITHDRAW';
}

export interface FundTransactionDialogResult {
  amount: number;
  note?: string;
  reconciledTotalBalance: number;
  reconciledDate: string;
}

@Component({
  selector: 'app-fund-transaction-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './fund-transaction-dialog.component.html',
  styleUrl: './fund-transaction-dialog.component.scss'
})
export class FundTransactionDialogComponent {

  readonly form = new FormGroup({
    reconciledTotalBalance: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(0)
    ]),
    reconciledDate: new FormControl(this.today(), {
      nonNullable: true,
      validators: [Validators.required]
    }),
    amount: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(0.01)
    ]),
    note: new FormControl('', { nonNullable: true })
  });

  constructor(
    private readonly dialogRef: MatDialogRef<FundTransactionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: FundTransactionDialogData
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (value.amount === null || value.reconciledTotalBalance === null) {
      return;
    }

    this.dialogRef.close({
      amount: value.amount,
      note: value.note.trim() || undefined,
      reconciledTotalBalance: value.reconciledTotalBalance,
      reconciledDate: value.reconciledDate
    } satisfies FundTransactionDialogResult);
  }

  actionLabel(): string {
    return this.data.type === 'ADD' ? 'Add to fund' : 'Withdraw from fund';
  }

  private today(): string {
    return new Date().toISOString().substring(0, 10);
  }
}
