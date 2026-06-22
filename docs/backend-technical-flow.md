# Backend Technical Flow

This document explains the backend API and feature flow for the UPI Expense Intel application.

## Stack

- Java Spring Boot backend.
- Spring MVC REST controllers under `backend/src/main/java/com/upiexpense/intel`.
- Spring Security with stateless JWT authentication.
- Spring Data JPA repositories for database access.
- PostgreSQL schema managed by Flyway migrations in `backend/src/main/resources/db/migration`.
- PDF text extraction and ICICI statement parsing under the `parser` package.

## Package Map

| Package | Responsibility |
| --- | --- |
| `auth` | Register, login, current user API, JWT response creation. |
| `common.security` | JWT parsing, current-user loading, security filter chain. |
| `common.error` | API error response shape and exception mapping. |
| `bankaccount` | User bank account CRUD and masking. |
| `category` | Category CRUD, default category seeding. |
| `upload` | Statement upload lifecycle, duplicate checks, parser orchestration. |
| `parser` | PDF text extraction, bank parser selection, ICICI parser. |
| `transaction` | Transaction listing, update, re-categorization. |
| `rule` | Custom merchant categorization rules. |
| `dashboard` | Monthly dashboard summary aggregation. |
| `report`, `notification` | Database entities for later monthly report/email features. |

## Request Security Flow

1. Public APIs are only:
   - `POST /api/auth/register`
   - `POST /api/auth/login`
2. All other `/api/**` routes require authentication.
3. Frontend sends `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` reads the bearer token.
5. `JwtService` extracts the email and validates the token.
6. `CurrentUserService` loads the authenticated user.
7. Controllers receive the authenticated user through `@AuthenticationPrincipal CurrentUser currentUser`.

Main files:

- `common/security/SecurityConfig.java`
- `common/security/JwtAuthenticationFilter.java`
- `common/security/JwtService.java`
- `common/security/CurrentUserService.java`

## API Surface

### Auth

Base path: `/api/auth`

| Method | Path | Auth | Flow |
| --- | --- | --- | --- |
| `POST` | `/register` | Public | `AuthController.register` -> `AuthService.register` -> save user -> seed default categories -> return JWT. |
| `POST` | `/login` | Public | `AuthController.login` -> `AuthService.login` -> validate password -> return JWT. |
| `GET` | `/me` | Required | `AuthController.me` -> `AuthService.currentUser` -> return profile from database. |

Registration also calls `DefaultCategorySeeder.seedForUser(savedUser)`, so every new user starts with default categories.

### Bank Accounts

Base path: `/api/bank-accounts`

| Method | Path | Flow |
| --- | --- | --- |
| `GET` | `/` | List active bank accounts for current user. |
| `POST` | `/` | Create account, mask account number, save as active. |
| `GET` | `/{id}` | Load one active account owned by current user. |
| `PUT` | `/{id}` | Update owned active account. |
| `DELETE` | `/{id}` | Soft delete by setting `is_active=false`. |

Main files:

- `bankaccount/BankAccountController.java`
- `bankaccount/BankAccountService.java`
- `bankaccount/AccountNumberMasker.java`

### Categories

Base path: `/api/categories`

| Method | Path | Flow |
| --- | --- | --- |
| `GET` | `/` | List active categories. If none exist, seed defaults first. |
| `POST` | `/` | Create custom category after duplicate-name validation. |
| `PUT` | `/{id}` | Update active owned category. |
| `DELETE` | `/{id}` | Soft delete custom category. Default categories cannot be deleted. |

Categories are user-scoped. The categorization logic resolves categories by name, so default category names such as `Food`, `Groceries`, `Other Expense`, `Salary`, and `Other Income` matter.

Main files:

- `category/CategoryController.java`
- `category/CategoryService.java`
- `category/DefaultCategorySeeder.java`

### Uploads

Base path: `/api/uploads`

| Method | Path | Flow |
| --- | --- | --- |
| `POST` | `/statements` | Upload PDF statement, parse it, create transactions, categorize them. |
| `GET` | `/` | List uploads, optionally filtered by `statementMonth=yyyy-MM`. |
| `GET` | `/{id}` | Get one owned upload. |
| `DELETE` | `/{statement_id}` | Delete upload and its transactions. |

Upload request parameters:

| Parameter | Type | Notes |
| --- | --- | --- |
| `bankAccountId` | UUID | Must belong to current user and be active. |
| `statementMonth` | `yyyy-MM` | Stored as first day of that month. |
| `file` | multipart file | Only PDF is supported. |
| `pdfPassword` | string, optional | Used by PDF extraction. |

Upload processing flow:

1. Validate file exists and is a PDF.
2. Read file bytes.
3. Compute SHA-256 hash.
4. Parse `statementMonth`.
5. Load current user's bank account.
6. Reject exact duplicate upload for same user, account, month, and file hash.
7. Create `statement_uploads` row with status `PARSING`.
8. Extract PDF text through `PdfTextExtractor`.
9. Build `StatementParseContext`.
10. Select parser through `StatementParserSelector`.
11. Parse rows through the selected `StatementParser`.
12. Compare parsed rows with existing transaction signatures for the same user/account/month.
13. Save only new transactions.
14. Run `CategorizationService.categorize(userId, transactions, false)` on newly parsed rows.
15. Save transactions.
16. Update upload status to `PARSED`, `PARTIALLY_FAILED`, or `FAILED`.

Main files:

- `upload/StatementUploadController.java`
- `upload/StatementUploadService.java`
- `parser/PdfTextExtractor.java`
- `parser/StatementParserSelector.java`
- `parser/IciciPdfStatementParser.java`

### Transactions

Base path: `/api/transactions`

| Method | Path | Flow |
| --- | --- | --- |
| `GET` | `/` | List transactions for a month with optional filters. |
| `GET` | `/{id}` | Load one owned transaction. |
| `PATCH` | `/{id}` | Update category, ignored flag, or transfer flag. |
| `POST` | `/categorize` | Re-run categorization for a month. |

List query parameters:

| Parameter | Notes |
| --- | --- |
| `month` | `yyyy-MM`; defaults to current month when absent. |
| `transactionType` | `DEBIT` or `CREDIT`. |
| `paymentMode` | `UPI`, `NEFT`, `IMPS`, `RTGS`, `ATM`, `POS`, `CARD`, `CHEQUE`, `INTEREST`, `CHARGES`, `UNKNOWN`. |
| `categoryId` | Filters by category UUID. |
| `merchant` | Case-insensitive search against merchant name or description. |
| `ignored` | `true` or `false`. |
| `transfer` | `true` or `false`. |

Re-categorization query parameters:

| Parameter | Notes |
| --- | --- |
| `month` | Required `yyyy-MM`. |
| `overwrite` | Defaults to `false`. When false, transactions with an existing category are skipped. |

Main files:

- `transaction/TransactionController.java`
- `transaction/TransactionService.java`
- `transaction/CategorizationService.java`

### Merchant Rules

Base path: `/api/merchant-rules`

| Method | Path | Flow |
| --- | --- | --- |
| `GET` | `/` | List current user's rules ordered by priority and creation time. |
| `POST` | `/` | Create rule against an active owned category. |
| `PUT` | `/{id}` | Update owned rule. |
| `DELETE` | `/{id}` | Deactivate rule. |

Rule fields:

| Field | Notes |
| --- | --- |
| `keyword` | Text or regex to match. Trimmed before save. |
| `matchType` | `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `EXACT`, or `REGEX`. |
| `categoryId` | Must be an active category owned by the user. |
| `merchantName` | Optional replacement merchant name when the rule matches. |
| `priority` | Lower number runs first. Default is `100`. |
| `active` | Inactive rules are not used by categorization. |

Main files:

- `rule/MerchantRuleController.java`
- `rule/MerchantRuleService.java`
- `rule/MerchantRuleRepository.java`

### Dashboard

Base path: `/api/dashboard`

| Method | Path | Flow |
| --- | --- | --- |
| `GET` | `/summary?month=yyyy-MM` | Aggregate income, expenses, savings, UPI totals, category breakdown, top merchants, and daily expense trend. |

Dashboard excludes transactions marked `ignored=true` or `transfer=true`.

Main files:

- `dashboard/DashboardController.java`
- `dashboard/DashboardService.java`
- `dashboard/DashboardSummaryResponse.java`

## Categorization Flow

`CategorizationService.categorize(userId, transactions, overwrite)` is used from:

- `StatementUploadService.uploadStatement`, with `overwrite=false` for new parsed transactions.
- `TransactionService.categorizeMonth`, with user-selected `overwrite`.

Flow:

1. Load active categories for the user.
2. Build a lookup map by normalized category name.
3. Load active merchant rules ordered by `priority asc, createdAt asc`.
4. For each transaction:
   - If `overwrite=false` and transaction already has a category, skip it.
   - Try custom merchant rules first.
   - If no custom rule matches, apply built-in category rules.
   - If matched, set category, optional merchant name, and confidence.

Custom rule matching uses this searchable text:

```text
description + rawDescription + merchantName + referenceNumber
```

The text is uppercased before matching. `CONTAINS` with keyword `GREENS` will match any searchable text containing `GREENS`.

Built-in fallback behavior:

- Credits become `Salary`, `Interest`, `Refund`, `Cashback`, or `Other Income`.
- Debits become categories like `Food`, `Groceries`, `Shopping`, `Bills`, `Travel`, `Subscriptions`, `Healthcare`, `Education`, or `Other Expense`.

Confidence values:

| Source | Confidence |
| --- | --- |
| Custom merchant rule | `0.95` |
| Strong built-in rule | `0.90` |
| Medium built-in rule | `0.75` |
| Fallback category | `0.50` |

## Statement Parsing Flow

The backend currently supports PDF statement parsing. Upload flow extracts text first, then selects a parser.

Parser abstractions:

| Type | Purpose |
| --- | --- |
| `StatementParseContext` | Bank name, file type, extracted text, statement month. |
| `StatementParserSelector` | Chooses a parser that supports the context. |
| `StatementParser` | Interface implemented by bank parsers. |
| `IciciPdfStatementParser` | ICICI PDF parser. |
| `ParsedTransactionRow` | Parser output row before database entity conversion. |
| `StatementParseResult` | Parsed rows, total row count, failed row count, errors. |

Parsed rows are converted to `Transaction` entities in `StatementUploadService.toTransaction`.

## Database Flow

Flyway migration `V1__create_core_tables.sql` creates:

- `users`
- `bank_accounts`
- `categories`
- `statement_uploads`
- `transactions`
- `merchant_rules`
- `monthly_reports`
- `email_delivery_logs`

Migration `V2__add_statement_upload_duplicate_protection.sql` adds duplicate upload protection using `statement_uploads.file_sha256`.

Ownership is enforced at repository/service level by querying with both resource ID and current `userId`.

## Common Error Flow

Services throw domain exceptions:

- `BadRequestException`
- `NotFoundException`
- `UnauthorizedException`

`GlobalExceptionHandler` converts these into consistent API error responses.

Frontend reads these errors through `apiErrorMessage`.

## End-to-End Backend Feature Flow

### Register/Login

```text
POST /api/auth/register
  -> AuthController
  -> AuthService
  -> UserRepository.save
  -> DefaultCategorySeeder.seedForUser
  -> JwtService.generateToken
  -> AuthResponse
```

### Add Bank Account

```text
POST /api/bank-accounts
  -> BankAccountController
  -> BankAccountService.create
  -> AccountNumberMasker.mask
  -> BankAccountRepository.save
  -> BankAccountResponse
```

### Upload PDF Statement

```text
POST /api/uploads/statements
  -> StatementUploadController
  -> StatementUploadService.uploadStatement
  -> PdfTextExtractor.extract
  -> StatementParserSelector.select
  -> IciciPdfStatementParser.parse
  -> duplicate transaction signature filtering
  -> CategorizationService.categorize
  -> TransactionRepository.saveAll
  -> StatementUploadRepository.save
  -> StatementUploadResponse
```

### Review Transactions

```text
GET /api/transactions?month=yyyy-MM
  -> TransactionController
  -> TransactionService.list
  -> TransactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc
  -> in-memory filters
  -> TransactionResponse list
```

### Re-run Categorization

```text
POST /api/transactions/categorize?month=yyyy-MM&overwrite=true
  -> TransactionController
  -> TransactionService.categorizeMonth
  -> load month transactions
  -> CategorizationService.categorize
  -> TransactionRepository.saveAll
  -> CategorizationResponse
```

### Dashboard

```text
GET /api/dashboard/summary?month=yyyy-MM
  -> DashboardController
  -> DashboardService.summary
  -> load month transactions
  -> exclude ignored and transfer rows
  -> aggregate totals and breakdowns
  -> DashboardSummaryResponse
```

