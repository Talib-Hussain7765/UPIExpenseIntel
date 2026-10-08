import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategorizationResponse, Transaction, TransactionFilters, UpdateTransactionRequest } from './transaction.models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/transactions`;

  constructor(private readonly http: HttpClient) {}

  list(filters: TransactionFilters): Observable<Transaction[]> {
    let params = new HttpParams().set('month', filters.month);
    if (filters.transactionType) {
      params = params.set('transactionType', filters.transactionType);
    }
    if (filters.paymentMode) {
      params = params.set('paymentMode', filters.paymentMode);
    }
    if (filters.categoryId) {
      params = params.set('categoryId', filters.categoryId);
    }
    if (filters.merchant) {
      params = params.set('merchant', filters.merchant);
    }
    if (filters.ignored !== '' && filters.ignored !== undefined) {
      params = params.set('ignored', String(filters.ignored));
    }
    if (filters.transfer !== '' && filters.transfer !== undefined) {
      params = params.set('transfer', String(filters.transfer));
    }
    return this.http.get<Transaction[]>(this.baseUrl, { params });
  }

  update(id: string, request: UpdateTransactionRequest): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.baseUrl}/${id}`, request);
  }

  categorize(month: string, overwrite: boolean): Observable<CategorizationResponse> {
    const params = new HttpParams()
      .set('month', month)
      .set('overwrite', String(overwrite));
    return this.http.post<CategorizationResponse>(`${this.baseUrl}/categorize`, null, { params });
  }
}
