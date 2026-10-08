export type DashboardDateBasis = 'MONTH' | 'CUSTOM_RANGE';

export interface DashboardSummaryRequest {
  dateBasis: DashboardDateBasis;
  month: string;
  startDate: string | null;
  endDate: string | null;
}

export interface DashboardSummaryResponse {
  month: string;
  totalIncome: number;
  totalExpenses: number;
  savingsAmount: number;
  savingsRate: number;
  upiDebitTotal: number;
  upiCreditTotal: number;
  transactionCount: number;
  uncategorizedCount: number;
  categoryBreakdown: CategoryBreakdownItem[];
  topMerchants: MerchantBreakdownItem[];
  dailyExpenseTrend: DailyExpenseTrendItem[];
  closingBalance: number;
}

export interface CategoryBreakdownItem {
  categoryId: string | null;
  categoryName: string;
  color: string | null;
  amount: number;
  transactionCount: number;
}

export interface MerchantBreakdownItem {
  merchantName: string;
  amount: number;
  transactionCount: number;
}

export interface DailyExpenseTrendItem {
  date: string;
  amount: number;
  transactionCount: number;
}
