import { CategoryType } from '../categories/category.models';

export type TransactionType = 'DEBIT' | 'CREDIT';
export type PaymentMode = 'UPI' | 'NEFT' | 'IMPS' | 'RTGS' | 'ATM' | 'POS' | 'CARD' | 'CHEQUE' | 'INTEREST' | 'CHARGES' | 'UNKNOWN';

export interface TransactionCategory {
  id: string;
  name: string;
  categoryType: CategoryType;
  color: string | null;
  icon: string | null;
}

export interface Transaction {
  id: string;
  bankAccountId: string;
  statementUploadId: string | null;
  transactionDate: string;
  valueDate: string | null;
  description: string;
  rawDescription: string;
  amount: number;
  transactionType: TransactionType;
  paymentMode: PaymentMode;
  merchantName: string | null;
  category: TransactionCategory | null;
  balanceAfterTransaction: number | null;
  referenceNumber: string | null;
  recurring: boolean;
  transfer: boolean;
  ignored: boolean;
  categorizationConfidence: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionFilters {
  month: string;
  transactionType?: TransactionType | '';
  paymentMode?: PaymentMode | '';
  categoryId?: string;
  merchant?: string;
  ignored?: boolean | '';
  transfer?: boolean | '';
}

export interface UpdateTransactionRequest {
  categoryId?: string | null;
  transfer?: boolean;
  ignored?: boolean;
}

export interface CategorizationResponse {
  updatedTransactions: number;
}
