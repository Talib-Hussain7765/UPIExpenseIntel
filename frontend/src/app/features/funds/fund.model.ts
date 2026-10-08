export interface Fund {
  id: string;
  name: string;
  balance: number;
}

export interface FundSummary {
  totalBalance: number;
  allocatedBalance: number;
  availableBalance: number;
  reconciledDate: string | null;
  funds: Fund[];
}

export interface CreateFundRequest {
  name: string;
}

export interface UpdateFundRequest {
  amount: number;
  type: 'ADD' | 'WITHDRAW';
  transactionDate: string;
  note?: string;
}

export interface ReconcileFundRequest {
  totalBalance: number;
  reconciledDate: string;
}

export type FundActivityPeriod =
  | 'LAST_MONTH'
  | 'LAST_3_MONTHS'
  | 'CUSTOM';

export interface FundTransaction {
  id: string;
  fundId: string;
  fundName: string;
  amount: number;
  type: 'ADD' | 'WITHDRAW';
  transactionDate: string;
  note: string | null;
  createdAt: string;
}

export interface FundTransactionHistory {
  periodStart: string;
  periodEnd: string;
  totalAdded: number;
  totalWithdrawn: number;
  netMovement: number;
  transactions: FundTransaction[];
}

export interface FundActivityRequest {
  fundId: string | null;
  period: FundActivityPeriod;
  startDate: string | null;
  endDate: string | null;
}
