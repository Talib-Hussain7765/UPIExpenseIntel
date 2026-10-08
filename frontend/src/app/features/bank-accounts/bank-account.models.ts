export interface BankAccount {
  id: string;
  bankName: string;
  accountNickname: string;
  maskedAccountNumber: string | null;
  accountType: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BankAccountRequest {
  bankName: string;
  accountNickname: string;
  maskedAccountNumber: string | null;
  accountType: string | null;
}
