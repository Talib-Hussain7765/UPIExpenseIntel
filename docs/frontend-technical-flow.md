# Frontend Technical Flow

This document explains the Angular frontend API and feature flow for the UPI Expense Intel application.

## Stack

- Angular standalone components.
- Angular Router for page navigation.
- Angular Material for UI controls.
- Angular reactive forms for create/edit/filter forms.
- Angular signals for local component state.
- Angular `HttpClient` services for backend API calls.
- Functional HTTP interceptor for JWT bearer token injection.
- ECharts through `ngx-echarts` for dashboard charts.

## App Bootstrap Flow

Main files:

- `frontend/src/app/app.config.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/app.component.ts`

`app.config.ts` wires global providers:

```text
provideRouter(routes)
provideAnimations()
provideHttpClient(withInterceptors([authTokenInterceptor]))
NgxEchartsModule.forRoot({ echarts })
```

Every HTTP request passes through `authTokenInterceptor`. If a token exists in local storage, it adds:

```text
Authorization: Bearer <token>
```

## Route Map

Routes are declared in `app.routes.ts`.

| Route | Component | Guard | Purpose |
| --- | --- | --- | --- |
| `/login` | `LoginComponent` | `guestGuard` | Login page. |
| `/register` | `RegisterComponent` | `guestGuard` | Registration page. |
| `/dashboard` | `DashboardComponent` inside `AppShellComponent` | `authGuard` | Monthly dashboard. |
| `/bank-accounts` | `BankAccountsComponent` inside `AppShellComponent` | `authGuard` | Add/edit/delete bank accounts. |
| `/uploads` | `UploadsComponent` inside `AppShellComponent` | `authGuard` | Upload PDF statements and view uploads. |
| `/transactions` | `TransactionsComponent` inside `AppShellComponent` | `authGuard` | Review, filter, edit, and re-categorize transactions. |
| `/categories` | `CategoriesComponent` inside `AppShellComponent` | `authGuard` | Manage categories. |
| `/merchant-rules` | `MerchantRulesComponent` inside `AppShellComponent` | `authGuard` | Manage custom categorization rules. |
| `/` | Redirect | `authGuard` | Redirects to `/dashboard`. |
| `/**` | Redirect | None | Redirects to `/`. |

## Auth Flow

Main files:

- `core/auth/auth.service.ts`
- `core/auth/auth-token.interceptor.ts`
- `core/auth/auth.guard.ts`
- `core/auth/guest.guard.ts`
- `features/auth/login/login.component.ts`
- `features/auth/register/register.component.ts`

### Login

```text
LoginComponent form submit
  -> AuthService.login(request)
  -> POST /api/auth/login
  -> storeAuth(response)
  -> localStorage access token
  -> currentUser signal set
  -> navigate to authenticated area
```

### Register

```text
RegisterComponent form submit
  -> AuthService.register(request)
  -> POST /api/auth/register
  -> backend creates user and default categories
  -> storeAuth(response)
  -> localStorage access token
  -> currentUser signal set
```

### Auth Guards

`authGuard` checks `AuthService.isAuthenticated()`.

- If token exists, protected route is allowed.
- If token is missing, user is redirected to `/login`.

`guestGuard` does the opposite.

- If user is not authenticated, login/register routes are allowed.
- If token exists, user is redirected to `/dashboard`.

### Token Storage

`AuthService` stores the token in local storage key:

```text
upi_expense_intel_access_token
```

Logout removes this key, clears the `currentUser` signal, and navigates to `/login`.

## Shared HTTP/Error Flow

Feature services use `HttpClient` directly. The interceptor adds JWT automatically.

Errors are displayed through:

- `shared/api-error.ts`

Typical component pattern:

```text
loading.set(true)
errorMessage.set(null)
service.apiCall().subscribe({
  next: update signals and stop loading,
  error: set apiErrorMessage(error) and stop loading
})
```

## Layout Flow

Main files:

- `core/layout/app-shell/app-shell.component.ts`
- `core/layout/app-shell/app-shell.component.html`

Authenticated pages render inside `AppShellComponent`. The shell owns the navigation links and contains the child route outlet.

Unauthenticated pages, login and register, render outside the shell.

## Feature Flow By Page

### Dashboard

Main files:

- `features/dashboard/dashboard.component.ts`
- `features/dashboard/dashboard.service.ts`
- `features/dashboard/dashboard.models.ts`

Backend endpoint:

```text
GET /api/dashboard/summary?month=yyyy-MM
```

Flow:

```text
DashboardComponent.ngOnInit
  -> loadSummary()
  -> DashboardService.summary(month)
  -> GET /api/dashboard/summary
  -> summary signal set
  -> template renders totals, category breakdown, merchants, chart
```

State:

| Signal/control | Purpose |
| --- | --- |
| `monthControl` | Selected month input. |
| `loading` | Spinner state. |
| `errorMessage` | API error display. |
| `summary` | Dashboard response. |
| `dailyTrendOptions` | Computed ECharts config from `summary.dailyExpenseTrend`. |

The dashboard is read-only. It reflects backend transactions after excluding ignored and transfer rows.

### Bank Accounts

Main files:

- `features/bank-accounts/bank-accounts.component.ts`
- `features/bank-accounts/bank-account.service.ts`
- `features/bank-accounts/bank-account.models.ts`

Backend endpoints:

```text
GET    /api/bank-accounts
POST   /api/bank-accounts
PUT    /api/bank-accounts/{id}
DELETE /api/bank-accounts/{id}
```

Flow:

```text
BankAccountsComponent.ngOnInit
  -> loadAccounts()
  -> BankAccountService.list()
  -> accounts signal set
```

Create/update flow:

```text
User fills form
  -> submit()
  -> toRequest()
  -> create() or update()
  -> reload accounts
  -> reset form
```

Delete flow:

```text
User clicks delete
  -> confirm()
  -> BankAccountService.delete(id)
  -> backend soft deletes account
  -> reload accounts
```

Important UI state:

| Signal/control | Purpose |
| --- | --- |
| `accounts` | Active bank accounts. |
| `editingAccount` | Determines create vs edit mode. |
| `formTitle` | Computed title from edit state. |
| `saving` | Disables duplicate submits. |

### Uploads

Main files:

- `features/uploads/uploads.component.ts`
- `features/uploads/upload.service.ts`
- `features/uploads/upload.models.ts`

Backend endpoints:

```text
GET    /api/uploads?statementMonth=yyyy-MM
POST   /api/uploads/statements
DELETE /api/uploads/{id}
```

Initial load flow:

```text
UploadsComponent.ngOnInit
  -> loadInitialData()
  -> BankAccountService.list()
  -> set first account as selected
  -> loadUploads(false)
  -> UploadService.list(statementMonth)
```

Upload flow:

```text
User selects bank account, month, password, PDF file
  -> submit()
  -> validate form and selectedFile
  -> UploadService.upload(request)
  -> FormData with bankAccountId, statementMonth, file, optional pdfPassword
  -> POST /api/uploads/statements
  -> backend parses and categorizes transactions
  -> success message
  -> reload uploads
```

Delete flow:

```text
User clicks delete upload
  -> confirm()
  -> UploadService.delete(id)
  -> backend deletes upload and its transactions
  -> reload uploads
```

Important UI state:

| Signal/control | Purpose |
| --- | --- |
| `bankAccounts` | Available accounts for upload. |
| `uploads` | Upload history for selected month. |
| `selectedFile` | Current PDF file object. |
| `hasAccounts` | Computed signal used to guide the empty-account state. |
| `uploading` | Upload button/spinner state. |

### Transactions

Main files:

- `features/transactions/transactions.component.ts`
- `features/transactions/transaction.service.ts`
- `features/transactions/transaction.models.ts`

Backend endpoints:

```text
GET   /api/transactions
GET   /api/transactions/{id}
PATCH /api/transactions/{id}
POST  /api/transactions/categorize?month=yyyy-MM&overwrite=true|false
```

Initial load flow:

```text
TransactionsComponent.ngOnInit
  -> loadCategories()
  -> CategoryService.list()
  -> categories signal set

TransactionsComponent.ngOnInit
  -> loadTransactions()
  -> TransactionService.list(filters)
  -> transactions signal set
```

Filter form fields:

| Control | Sent as query parameter |
| --- | --- |
| `month` | `month` |
| `transactionType` | `transactionType` |
| `paymentMode` | `paymentMode` |
| `categoryId` | `categoryId` |
| `merchant` | `merchant` |
| `ignored` | `ignored` |
| `transfer` | `transfer` |

Transaction edit flow:

```text
User changes category
  -> updateCategory(transaction, categoryId)
  -> PATCH /api/transactions/{id}
  -> replace updated row in transactions signal
```

Ignored/transfer flow:

```text
User clicks ignored or transfer icon
  -> toggleIgnored() or toggleTransfer()
  -> PATCH /api/transactions/{id}
  -> replace updated row in transactions signal
```

Re-run categorization flow:

```text
User selects month
User optionally checks Overwrite existing
User clicks Re-run categorization
  -> rerunCategorization()
  -> TransactionService.categorize(month, overwrite)
  -> POST /api/transactions/categorize
  -> success message with updated count
  -> reload transactions
```

Important behavior:

- If `overwrite=false`, backend skips transactions that already have a category.
- If `overwrite=true`, backend can replace existing categories using merchant rules and built-in rules.
- The frontend sends `overwrite` as a query parameter.

Important UI state:

| Signal/control | Purpose |
| --- | --- |
| `filters` | Month, transaction filters, and overwrite checkbox. |
| `categories` | Category dropdown options. |
| `transactions` | Displayed rows. |
| `savingId` | Disables one row while PATCH is in flight. |
| `categorizing` | Disables re-run button while API call is in flight. |

### Categories

Main files:

- `features/categories/categories.component.ts`
- `features/categories/category.service.ts`
- `features/categories/category.models.ts`

Backend endpoints:

```text
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

Flow:

```text
CategoriesComponent.ngOnInit
  -> loadCategories()
  -> CategoryService.list()
  -> categories signal set
```

Create/update flow:

```text
User fills category form
  -> submit()
  -> toRequest()
  -> create() or update()
  -> success message
  -> reset form
  -> reload categories
```

Delete flow:

```text
User clicks delete
  -> if defaultCategory, show error locally
  -> otherwise confirm()
  -> CategoryService.delete(id)
  -> reload categories
```

Important behavior:

- The frontend prevents default category delete before calling backend.
- The backend also rejects default category delete, so this is protected in both layers.
- Category `icon` stores the Material icon name shown by the UI.

### Merchant Rules

Main files:

- `features/merchant-rules/merchant-rules.component.ts`
- `features/merchant-rules/merchant-rule.service.ts`
- `features/merchant-rules/merchant-rule.models.ts`

Backend endpoints:

```text
GET    /api/merchant-rules
POST   /api/merchant-rules
PUT    /api/merchant-rules/{id}
DELETE /api/merchant-rules/{id}
```

Initial load flow:

```text
MerchantRulesComponent.ngOnInit
  -> loadInitialData()
  -> CategoryService.list()
  -> set first category as default selection
  -> loadRules(false)
  -> MerchantRuleService.list()
```

Create/update flow:

```text
User fills keyword, match type, category, optional merchant name, priority, active
  -> submit()
  -> toRequest()
  -> create() or update()
  -> success message
  -> reset form
  -> reload rules
```

Deactivate flow:

```text
User clicks deactivate
  -> confirm()
  -> MerchantRuleService.delete(id)
  -> backend sets active=false
  -> reload rules
```

Important behavior:

- Rules do not automatically modify old transactions when saved.
- After creating or editing rules, go to Transactions and run re-categorization.
- Use `Overwrite existing` if existing `Other Expense` or any old category should be replaced.
- Lower priority number runs first on the backend.

## Frontend-To-Backend Feature Flow

### Full Manual User Flow

```text
Register/Login
  -> AuthService
  -> backend auth API
  -> token stored
  -> interceptor authenticates future requests

Add bank account
  -> BankAccountsComponent
  -> BankAccountService
  -> /api/bank-accounts

Upload PDF
  -> UploadsComponent
  -> UploadService
  -> /api/uploads/statements
  -> backend parses and categorizes

Review transactions
  -> TransactionsComponent
  -> TransactionService.list
  -> /api/transactions

Create merchant rules
  -> MerchantRulesComponent
  -> MerchantRuleService
  -> /api/merchant-rules

Re-run categorization
  -> TransactionsComponent.rerunCategorization
  -> TransactionService.categorize
  -> /api/transactions/categorize

View dashboard
  -> DashboardComponent
  -> DashboardService
  -> /api/dashboard/summary
```

## Service Map

| Service | Base URL | Used By |
| --- | --- | --- |
| `AuthService` | `/api/auth` | Login, register, guards, interceptor. |
| `DashboardService` | `/api/dashboard` | Dashboard page. |
| `BankAccountService` | `/api/bank-accounts` | Bank accounts page, uploads page. |
| `UploadService` | `/api/uploads` | Uploads page. |
| `TransactionService` | `/api/transactions` | Transactions page. |
| `CategoryService` | `/api/categories` | Categories page, transactions page, merchant rules page. |
| `MerchantRuleService` | `/api/merchant-rules` | Merchant rules page. |

## State Management Pattern

This app does not use a global store. Each feature component owns its page state with Angular signals and reactive forms.

Common signal names:

| Signal | Purpose |
| --- | --- |
| `loading` | Main page load spinner. |
| `saving` | Create/update form spinner. |
| `uploading` | Upload-specific spinner. |
| `categorizing` | Transaction categorization spinner. |
| `errorMessage` | API or validation error message. |
| `successMessage` | Success status message. |

Data is refreshed after mutations by calling the page's load method again, except transaction row PATCH updates replace the row locally.

## Adding A New Frontend Feature

Follow the existing pattern:

1. Add models under `features/<feature>/<feature>.models.ts`.
2. Add an injectable service under `features/<feature>/<feature>.service.ts`.
3. Add a standalone component under `features/<feature>/<feature>.component.ts`.
4. Import only the Angular Material modules that component needs.
5. Add a route in `app.routes.ts` under the authenticated shell if it requires login.
6. Add navigation in `AppShellComponent` if users need direct access.
7. Use `apiErrorMessage(error)` for API errors.
8. Use the existing signal names for loading, saving, error, and success state.

