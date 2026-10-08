import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Category, CategoryRequest } from './category.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly baseUrl = `${environment.apiBaseUrl}/categories`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl);
  }

  create(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, request);
  }

  update(id: string, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
