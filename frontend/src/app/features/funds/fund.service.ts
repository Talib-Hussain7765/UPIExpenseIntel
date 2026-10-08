import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateFundRequest,
  FundActivityRequest,
  Fund,
  FundSummary,
  FundTransactionHistory,
  ReconcileFundRequest,
  UpdateFundRequest
} from './fund.model';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class FundService {

  constructor(private http: HttpClient) {}

  getSummary(): Observable<FundSummary> {
    return this.http.get<FundSummary>(
      `${environment.apiBaseUrl}/funds/summary`
    );
  }

  getTransactions(
    request: FundActivityRequest
  ): Observable<FundTransactionHistory> {
    let params = new HttpParams().set('period', request.period);

    if (request.fundId) {
      params = params.set('fundId', request.fundId);
    }

    if (request.period === 'CUSTOM') {
      if (request.startDate) {
        params = params.set('startDate', request.startDate);
      }

      if (request.endDate) {
        params = params.set('endDate', request.endDate);
      }
    }

    return this.http.get<FundTransactionHistory>(
      `${environment.apiBaseUrl}/funds/transactions`,
      { params }
    );
  }

  createFund(request: CreateFundRequest): Observable<Fund> {
    return this.http.post<Fund>(
      `${environment.apiBaseUrl}/funds`,
      request
    );
  }

  updateFund(
    fundId: string,
    request: UpdateFundRequest
  ): Observable<void> {

    return this.http.post<void>(
      `${environment.apiBaseUrl}/funds/${fundId}/transactions`,
      request
    );
  }

  reconcile(request: ReconcileFundRequest): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/funds/reconcile`,
      request
    );
  }
}
