export type UploadStatus = 'UPLOADED' | 'PARSING' | 'PARSED' | 'PARTIALLY_FAILED' | 'FAILED' | 'CONFIRMED';
export type StatementImportDateBasis = 'TRANSACTION_DATE' | 'STATEMENT_MONTH_ONLY' | 'CUSTOM_RANGE';

export interface StatementUpload {
  id: string;
  bankAccountId: string;
  originalFileName: string;
  fileType: string;
  statementMonth: string;
  status: UploadStatus;
  totalRows: number;
  parsedRows: number;
  skippedRows: number;
  duplicateRows: number;
  failedRows: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UploadStatementRequest {
  bankAccountId: string;
  statementMonth: string;
  file: File;
  pdfPassword: string | null;
  dateBasis: StatementImportDateBasis;
  importStartDate: string | null;
  importEndDate: string | null;
}
