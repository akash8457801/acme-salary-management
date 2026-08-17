import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import {
  CompensationChangeRequest,
  CompensationEntry,
  CreateEmployeeRequest,
  EmployeeDetail,
  EmployeeListQuery,
  EmployeeSummary,
  InsightsDashboard,
  Page,
  ReferenceData,
  UpdateEmployeeRequest,
} from './models';

/**
 * The one place HTTP happens. Components talk in domain terms; URLs, params and response
 * shapes live here.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  /** Dropdown contents never change mid-session, so one fetch is shared by every subscriber. */
  readonly referenceData$: Observable<ReferenceData> = this.http
    .get<ReferenceData>(`${this.base}/reference-data`)
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  listEmployees(query: EmployeeListQuery): Observable<Page<EmployeeSummary>> {
    return this.http.get<Page<EmployeeSummary>>(`${this.base}/employees`, {
      params: this.toParams(query),
    });
  }

  employee(id: number): Observable<EmployeeDetail> {
    return this.http.get<EmployeeDetail>(`${this.base}/employees/${id}`);
  }

  createEmployee(request: CreateEmployeeRequest): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.base}/employees`, request);
  }

  updateEmployee(id: number, request: UpdateEmployeeRequest): Observable<EmployeeDetail> {
    return this.http.put<EmployeeDetail>(`${this.base}/employees/${id}`, request);
  }

  recordCompensationChange(
    employeeId: number,
    request: CompensationChangeRequest,
  ): Observable<CompensationEntry> {
    return this.http.post<CompensationEntry>(
      `${this.base}/employees/${employeeId}/compensation`,
      request,
    );
  }

  insightsDashboard(): Observable<InsightsDashboard> {
    return this.http.get<InsightsDashboard>(`${this.base}/insights/dashboard`);
  }

  /** The export downloads via the browser so it streams; this just assembles the URL. */
  exportUrl(query: EmployeeListQuery): string {
    const params = this.toParams(query).toString();
    return `${this.base}/employees/export${params ? '?' + params : ''}`;
  }

  /** Omits blank/undefined values so the URL only says what the user actually narrowed. */
  private toParams(query: EmployeeListQuery): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
