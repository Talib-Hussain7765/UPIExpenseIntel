export type CategoryType = 'EXPENSE' | 'INCOME' | 'TRANSFER' | 'SYSTEM';

export interface Category {
  id: string;
  name: string;
  categoryType: CategoryType;
  color: string | null;
  icon: string | null;
  displayOrder: number;
  defaultCategory: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  categoryType: CategoryType;
  color: string | null;
  icon: string | null;
  displayOrder: number | null;
}
