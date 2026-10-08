import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StatementUpload, UploadStatementRequest } from './upload.models';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly baseUrl = `${environment.apiBaseUrl}/uploads`;

  constructor(private readonly http: HttpClient) {}

  list(statementMonth?: string): Observable<StatementUpload[]> {
    let params = new HttpParams();
    if (statementMonth) {
      params = params.set('statementMonth', statementMonth);
    }
    return this.http.get<StatementUpload[]>(this.baseUrl, { params });
  }

  upload(request: UploadStatementRequest): Observable<StatementUpload> {
    const formData = new FormData();
    formData.set('bankAccountId', request.bankAccountId);
    formData.set('statementMonth', request.statementMonth);
    formData.set('file', request.file);
    if (request.pdfPassword) {
      formData.set('pdfPassword', request.pdfPassword);
    }
    formData.set('dateBasis', request.dateBasis);
    if (request.importStartDate) {
      formData.set('importStartDate', request.importStartDate);
    }
    if (request.importEndDate) {
      formData.set('importEndDate', request.importEndDate);
    }
    return this.http.post<StatementUpload>(`${this.baseUrl}/statements`, formData);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
