import { CategoryType } from '../categories/category.models';

export type RuleMatchType = 'CONTAINS' | 'STARTS_WITH' | 'ENDS_WITH' | 'REGEX' | 'EXACT';

export interface MerchantRuleCategory {
  id: string;
  name: string;
  categoryType: CategoryType;
  color: string | null;
  icon: string | null;
}

export interface MerchantRule {
  id: string;
  keyword: string;
  matchType: RuleMatchType;
  category: MerchantRuleCategory;
  merchantName: string | null;
  priority: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MerchantRuleRequest {
  keyword: string;
  matchType: RuleMatchType;
  categoryId: string;
  merchantName: string | null;
  priority: number | null;
  active: boolean | null;
}
