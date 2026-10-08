import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MerchantRule, MerchantRuleRequest } from './merchant-rule.models';

@Injectable({ providedIn: 'root' })
export class MerchantRuleService {
  private readonly baseUrl = `${environment.apiBaseUrl}/merchant-rules`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<MerchantRule[]> {
    return this.http.get<MerchantRule[]>(this.baseUrl);
  }

  create(request: MerchantRuleRequest): Observable<MerchantRule> {
    return this.http.post<MerchantRule>(this.baseUrl, request);
  }

  update(id: string, request: MerchantRuleRequest): Observable<MerchantRule> {
    return this.http.put<MerchantRule>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
