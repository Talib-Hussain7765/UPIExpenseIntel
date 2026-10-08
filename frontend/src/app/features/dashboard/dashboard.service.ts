import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardSummaryRequest, DashboardSummaryResponse } from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly http: HttpClient) {}

  summary(request: DashboardSummaryRequest): Observable<DashboardSummaryResponse> {
    let params = new HttpParams().set('dateBasis', request.dateBasis);
    if (request.dateBasis === 'CUSTOM_RANGE') {
      params = params
        .set('startDate', request.startDate ?? '')
        .set('endDate', request.endDate ?? '');
    } else {
      params = params.set('month', request.month);
    }

    return this.http.get<DashboardSummaryResponse>(`${environment.apiBaseUrl}/dashboard/summary`, {
      params
    });
  }
}
