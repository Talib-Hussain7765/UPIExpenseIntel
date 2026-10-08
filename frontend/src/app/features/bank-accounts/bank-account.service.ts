import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BankAccount, BankAccountRequest } from './bank-account.models';

@Injectable({ providedIn: 'root' })
export class BankAccountService {
  private readonly baseUrl = `${environment.apiBaseUrl}/bank-accounts`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<BankAccount[]> {
    return this.http.get<BankAccount[]>(this.baseUrl);
  }

  create(request: BankAccountRequest): Observable<BankAccount> {
    return this.http.post<BankAccount>(this.baseUrl, request);
  }

  update(id: string, request: BankAccountRequest): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
