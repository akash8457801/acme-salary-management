/**
 * The API contract, mirrored as types. One file on purpose: this is the seam between backend and
 * frontend, and being able to read the whole contract in one place is worth more than one-file-
 * per-type ceremony.
 */

export interface Money {
  amount: number;
  currency: string;
}

export interface EmployeeSummary {
  id: number;
  employeeCode: string;
  fullName: string;
  email: string;
  departmentId: number;
  department: string;
  countryCode: string;
  country: string;
  jobTitle: string;
  level: string;
  levelTitle: string;
  status: string;
  hireDate: string;
  salary: Money | null;
  annualUsd: Money | null;
}

export interface CompensationEntry {
  id: number;
  salary: Money;
  annualUsd: Money;
  effectiveFrom: string;
  effectiveTo: string | null;
  reason: string;
  reasonLabel: string;
  note: string | null;
  recordedAt: string;
  changePercent: number | null;
  current: boolean;
}

export interface EmployeeDetail {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  departmentId: number;
  department: string;
  countryCode: string;
  country: string;
  payrollCurrency: string;
  jobTitle: string;
  level: string;
  levelTitle: string;
  gender: string;
  managerId: number | null;
  managerName: string | null;
  hireDate: string;
  status: string;
  currentCompensation: CompensationEntry | null;
  compensationHistory: CompensationEntry[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Option {
  value: string;
  label: string;
}

export interface ReferenceData {
  departments: { id: number; name: string }[];
  countries: { code: string; name: string; currency: string }[];
  levels: Option[];
  statuses: Option[];
  changeReasons: Option[];
  genders: Option[];
}

export interface OrgOverview {
  headcount: number;
  totalAnnualUsd: number;
  averageAnnualUsd: number;
  medianAnnualUsd: number;
  countryCount: number;
  departmentCount: number;
}

export interface BreakdownRow {
  key: string;
  label: string;
  headcount: number;
  totalAnnualUsd: number;
  averageAnnualUsd: number;
  medianAnnualUsd: number;
  p25AnnualUsd: number;
  p75AnnualUsd: number;
}

export interface SalaryBand {
  lowerBoundUsd: number;
  upperBoundUsd: number | null;
  headcount: number;
}

export interface PayEquityRow {
  level: string;
  femaleCount: number;
  femaleMedianUsd: number | null;
  maleCount: number;
  maleMedianUsd: number | null;
  gapPercent: number | null;
  otherCount: number;
}

export interface InsightsDashboard {
  overview: OrgOverview;
  byDepartment: BreakdownRow[];
  byCountry: BreakdownRow[];
  byLevel: BreakdownRow[];
  distribution: SalaryBand[];
  payEquity: PayEquityRow[];
}

/** What the directory is currently narrowed to. Also the shape of the list's query params. */
export interface EmployeeListQuery {
  q?: string;
  departmentId?: number;
  countryCode?: string;
  level?: string;
  status?: string;
  minAnnualUsd?: number;
  maxAnnualUsd?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

export interface CompensationChangeRequest {
  amount: number;
  currency: string;
  effectiveFrom: string;
  reason: string;
  note?: string;
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  departmentId: number;
  countryCode: string;
  jobTitle: string;
  level: string;
  gender: string;
  managerId?: number | null;
  hireDate: string;
  startingSalary: number;
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  departmentId: number;
  jobTitle: string;
  level: string;
  managerId?: number | null;
  status: string;
}

export interface ApiError {
  message: string;
  fieldErrors?: Record<string, string>;
}
