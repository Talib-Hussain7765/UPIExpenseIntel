 # UPI Expense Dashboard - Project Design Document

## 1. Project Summary

The UPI Expense Dashboard is a personal finance web application for tracking income, UPI expenses, bank credits, monthly spending patterns, and savings. The application will initially import bank statement files, normalize transactions, categorize expenses, generate monthly reports, and email the reports to the user.

Credit card expense tracking is planned as a later enhancement and is intentionally kept outside the MVP scope.

## 2. Goals

- Import bank transactions from uploaded statements.
- Detect UPI debits, UPI credits, salary credits, refunds, transfers, and other bank transactions.
- Categorize expenses automatically using merchant and keyword rules.
- Allow users to correct categories and define custom rules.
- Generate monthly income, expense, savings, and category reports.
- Send monthly email reports automatically.
- Provide a dashboard for expense trends, top merchants, top categories, and monthly comparison.
- Keep the system extensible for Gmail import, Account Aggregator integration, and credit card statements later.

## 3. Non-Goals for MVP

- Direct live integration with Google Pay, PhonePe, Paytm, or other UPI apps.
- Direct production Account Aggregator integration.
- Credit card statement import.
- Investment tracking.
- Tax filing.
- Multi-user family budgeting.
- AI-based financial advice.
- Mobile app or SMS parsing.

## 4. Why Direct UPI Integration Is Not the MVP Path

UPI does not generally provide a simple public API for a personal finance app to pull a user's complete transaction history from UPI apps. UPI APIs are typically used by banks, payment aggregators, payment service providers, and approved third-party app providers for payment flows.

For this project, the practical MVP data source is bank statement import. Bank statements contain UPI debit and credit entries, salary credits, refunds, transfers, ATM withdrawals, card payments, and other account activity.

Future integrations can include:

- Gmail transaction email parsing through OAuth.
- Account Aggregator integration through a regulated provider.
- Android SMS parsing through a separate mobile app.
- Credit card statement import.

## 5. Target User

Primary user:

- A salaried person in India who uses UPI frequently.
- Wants to understand monthly spending without manually entering every transaction.
- Wants a simple monthly report showing income, expenses, savings, categories, and unusual spending.

Secondary user:

- A developer or power user who wants custom categorization rules and searchable transaction history.

## 6. Recommended Tech Stack

### Frontend

- Angular 18+ or latest stable Angular version.
- TypeScript.
- Angular Material or PrimeNG for UI components.
- Apache ECharts through ngx-echarts for charts.
- RxJS for async state handling.
- Angular Reactive Forms for uploads, filters, rule forms, and category forms.

### Backend

- Java 21.
- Spring Boot 3+.
- Spring Web for REST APIs.
- Spring Security with JWT authentication.
- Spring Data JPA with Hibernate.
- Spring Scheduler for monthly report jobs.
- Spring Mail with an email provider such as AWS SES, SendGrid, or Resend.

### Database

- PostgreSQL.

### File Parsing

- Apache PDFBox for MVP PDF text extraction.
- Bank-specific parser implementations behind a common `StatementParser` interface.
- Optional PDF password support for protected statements.
- Apache Commons CSV or OpenCSV for future CSV statements.
- Apache POI for future Excel statements.
- Optional future Python parser service for difficult PDF statements using pdfplumber, pandas, Camelot, or tabula-py.

### Storage

- Local filesystem for development.
- S3-compatible object storage for production.

### DevOps

- Docker Compose for local PostgreSQL and optional mail testing.
- Flyway or Liquibase for schema migrations.
- GitHub Actions for CI later.

## 7. High-Level Architecture

```text
Angular Web App
    |
    | REST API over HTTPS
    v
Spring Boot Backend
    |
    | JPA
    v
PostgreSQL

Spring Scheduler
    |
    v
Monthly Report Generator
    |
    v
Email Provider

Statement File Storage
    ^
    |
Upload Service
```

## 8. Core Backend Modules

```text
auth
users
bankaccounts
uploads
parsing
transactions
categories
rules
reports
notifications
audit
```

### Module Responsibilities

`auth`

- Signup.
- Login.
- JWT generation.
- Password hashing.
- Auth filters.

`users`

- User profile.
- User preferences.
- Report email preferences.

`bankaccounts`

- User bank accounts.
- Bank name.
- Account nickname.
- Masked account number.
- Statement source configuration.

`uploads`

- Statement upload.
- File validation.
- Upload status tracking.
- Parser selection.
- Duplicate upload checks.

`parsing`

- Bank-specific parsers.
- CSV, XLSX, and PDF parsing.
- Conversion from raw rows into normalized transaction records.

`transactions`

- Transaction storage.
- Transaction search and filtering.
- Duplicate transaction detection.
- Merchant extraction.
- Payment mode detection.

`categories`

- Default categories.
- User-defined categories.
- Category colors and display ordering.

`rules`

- Merchant and keyword categorization rules.
- Rule priority.
- Manual recategorization learning.

`reports`

- Monthly summary generation.
- Category breakdown.
- Merchant breakdown.
- Income and expense summaries.
- PDF or HTML report generation.

`notifications`

- Monthly email delivery.
- Manual report email.
- Email delivery logs.

`audit`

- Track sensitive actions such as login, upload, delete, and report generation.

## 9. MVP Functional Requirements

### Authentication

- User can register with email and password.
- User can log in.
- User can log out.
- Backend APIs must require authentication except signup and login.

### Bank Account Management

- User can add a bank account nickname.
- User can select bank name.
- User can optionally store masked account number.

### Statement Upload

- User can upload a PDF statement in MVP.
- MVP parser support starts with ICICI savings account PDF statements.
- User can select the bank account and statement month.
- User can provide an optional PDF password.
- Backend validates file type and size.
- Backend stores upload metadata.
- Backend extracts PDF text and parses the file synchronously in MVP.
- Backend records total rows, parsed rows, failed rows, upload status, and parser errors.
- User can review imported transactions.

### Transaction Management

- User can view transactions by month.
- User can filter by category, type, merchant, payment mode, and amount range.
- User can update category manually.
- User can mark a transaction as ignored.
- User can mark a transaction as transfer.

### Categorization

- System auto-categorizes transactions using keyword rules.
- User can create custom rules.
- User can re-run categorization for a month.

### Dashboard

- Show selected month summary.
- Show total income.
- Show total expenses.
- Show savings amount.
- Show savings percentage.
- Show category breakdown.
- Show top merchants.
- Show daily expense trend.
- Show UPI debit and credit totals.

### Monthly Report

- User can generate report for a month.
- User can preview report in the browser.
- User can send report to own email.
- System can send monthly report automatically based on user preference.

## 10. Future Functional Requirements

- Additional bank-specific PDF statement parsers.
- CSV statement import.
- Excel statement import.
- Gmail transaction email import.
- Account Aggregator-ready integration layer.
- Credit card statement import.
- Budget planning.
- Recurring subscription detection.
- Unusual spending detection.
- AI-generated monthly summary.
- Export transactions to CSV.
- Multi-account consolidated reports.
- Household shared budget.

## 11. Data Model

### users

```text
id
email
password_hash
full_name
timezone
currency
monthly_report_enabled
monthly_report_day
created_at
updated_at
```

### bank_accounts

```text
id
user_id
bank_name
account_nickname
masked_account_number
account_type
is_active
created_at
updated_at
```

### statement_uploads

```text
id
user_id
bank_account_id
original_file_name
stored_file_path
file_type
statement_month
status
total_rows
parsed_rows
failed_rows
error_message
created_at
updated_at
```

Upload statuses:

```text
UPLOADED
PARSING
PARSED
PARTIALLY_FAILED
FAILED
CONFIRMED
```

### transactions

```text
id
user_id
bank_account_id
statement_upload_id
transaction_date
value_date
description
raw_description
amount
transaction_type
payment_mode
merchant_name
category_id
balance_after_transaction
reference_number
is_recurring
is_transfer
is_ignored
categorization_confidence
created_at
updated_at
```

Transaction types:

```text
DEBIT
CREDIT
```

Payment modes:

```text
UPI
NEFT
IMPS
RTGS
ATM
POS
CARD
CHEQUE
INTEREST
CHARGES
UNKNOWN
```

### categories

```text
id
user_id
name
category_type
color
icon
is_default
is_active
created_at
updated_at
```

Category types:

```text
EXPENSE
INCOME
TRANSFER
SYSTEM
```

Default expense categories:

```text
Food
Groceries
Rent
Travel
Shopping
Bills
Subscriptions
Healthcare
Education
Entertainment
Investments
Cash Withdrawal
Bank Charges
Other Expense
```

Default income categories:

```text
Salary
Refund
Interest
Cashback
Other Income
```

### merchant_rules

```text
id
user_id
keyword
match_type
category_id
merchant_name
priority
is_active
created_at
updated_at
```

Match types:

```text
CONTAINS
STARTS_WITH
ENDS_WITH
REGEX
EXACT
```

### monthly_reports

```text
id
user_id
report_month
total_income
total_expense
savings_amount
savings_rate
report_html_path
report_pdf_path
generated_at
created_at
updated_at
```

### email_delivery_logs

```text
id
user_id
monthly_report_id
recipient_email
subject
status
provider_message_id
error_message
sent_at
created_at
```

Email statuses:

```text
PENDING
SENT
FAILED
RETRYING
```

## 12. Transaction Normalization

All bank-specific statement rows should be converted into one common transaction model.

Example normalized transaction:

```json
{
  "transactionDate": "2026-04-12",
  "description": "UPI/DR/410298/SWIGGY/ICICI",
  "amount": 482.0,
  "transactionType": "DEBIT",
  "paymentMode": "UPI",
  "merchantName": "Swiggy",
  "category": "Food",
  "referenceNumber": "410298"
}
```

## 13. UPI Description Parsing

Common UPI description patterns may look like:

```text
UPI/DR/4218/SWIGGY/...
UPI/CR/4219/JOHN DOE/...
UPI-ZOMATO-ORDER-...
UPI TO RAHUL
UPI FROM JOHN
```

Parser responsibilities:

- Select the correct bank-specific parser based on bank name, file type, and statement content.
- Detect whether the transaction is UPI.
- Detect debit or credit.
- Extract reference number where available.
- Extract possible merchant or sender name.
- Capture balance after transaction where available.
- Keep original raw description unchanged for auditability.

## 14. Salary and Credit Detection

Salary detection can use keywords:

```text
SALARY
PAYROLL
COMPENSATION
ACME PVT LTD
NEFT CR SAL
```

Rules:

- A recurring monthly credit with similar amount can be suggested as salary.
- User must be able to manually confirm salary category.
- Refunds and cashbacks should not be counted as salary.

## 15. Categorization Strategy

### Phase 1: Rule-Based

Use system rules and user rules.

Examples:

```text
SWIGGY -> Food
ZOMATO -> Food
UBER -> Travel
OLA -> Travel
AMAZON -> Shopping
FLIPKART -> Shopping
NETFLIX -> Subscriptions
SPOTIFY -> Subscriptions
ELECTRICITY -> Bills
RENT -> Rent
SALARY -> Salary
```

### Phase 2: Learning from User Corrections

When the user changes a transaction category, the app can offer:

```text
Always categorize transactions containing "SWIGGY" as Food?
```

### Phase 3: AI-Assisted Summary

AI can be used later to generate human-readable monthly insights. Financial decisions should remain user-controlled.

## 16. Duplicate Detection

Potential duplicate checks:

- Same user.
- Same bank account.
- Same transaction date.
- Same amount.
- Same transaction type.
- Same reference number, if available.
- Similar normalized description.

If a duplicate is detected, the system should skip the transaction or mark it for review.

## 17. Monthly Report Design

Monthly report should include:

- Month and year.
- Total income.
- Total expense.
- Savings amount.
- Savings rate.
- Category-wise expenses.
- Top merchants.
- Largest expenses.
- UPI debit total.
- UPI credit total.
- Recurring payments.
- Unusual spending notes.

Example summary:

```text
Income: Rs. 120,000
Expenses: Rs. 72,450
Savings: Rs. 47,550
Savings Rate: 39.6%

Top Categories:
Food: Rs. 13,200
Rent: Rs. 25,000
Shopping: Rs. 8,700
Travel: Rs. 5,400
```

## 18. Email Report Flow

```text
Scheduler runs daily
    |
Find users whose monthly report day is today
    |
Generate previous month's report
    |
Render HTML email
    |
Generate optional PDF
    |
Send email
    |
Store delivery log
```

For example, if the user selects report day 1, then on May 1, 2026 the app sends the April 2026 report.

## 19. REST API Design

### Auth

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

### Bank Accounts

```text
GET    /api/bank-accounts
POST   /api/bank-accounts
GET    /api/bank-accounts/{id}
PUT    /api/bank-accounts/{id}
DELETE /api/bank-accounts/{id}
```

### Uploads

```text
POST /api/uploads/statements
GET  /api/uploads
GET  /api/uploads/{id}
POST /api/uploads/{id}/confirm
POST /api/uploads/{id}/reparse
```

### Transactions

```text
GET   /api/transactions
GET   /api/transactions/{id}
PATCH /api/transactions/{id}
POST  /api/transactions/bulk-categorize
```

Example filters:

```text
/api/transactions?month=2026-04&type=DEBIT&category=Food&paymentMode=UPI
```

### Categories

```text
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

### Rules

```text
GET    /api/rules
POST   /api/rules
PUT    /api/rules/{id}
DELETE /api/rules/{id}
POST   /api/rules/apply
```

### Reports

```text
GET  /api/reports/monthly
POST /api/reports/monthly/generate
POST /api/reports/monthly/{id}/email
GET  /api/reports/monthly/{id}
```

## 20. Frontend Pages

```text
/login
/register
/dashboard
/transactions
/uploads
/reports
/categories
/rules
/settings
```

### Dashboard

Widgets:

- Month selector.
- Income card.
- Expense card.
- Savings card.
- Savings rate card.
- Expense by category chart.
- Daily spending trend.
- Top merchants table.
- Recent transactions table.

### Transactions Page

Features:

- Search.
- Filters.
- Sort.
- Category edit.
- Mark ignored.
- Mark transfer.
- Export CSV later.

### Uploads Page

Features:

- Upload statement.
- Select bank account.
- Select month.
- View parsing result.
- Confirm import.
- View failed rows.

### Reports Page

Features:

- Generate report.
- Preview report.
- Email report.
- View report history.

### Rules Page

Features:

- Create keyword rule.
- Choose match type.
- Assign category.
- Set merchant display name.
- Enable or disable rule.

## 21. Security and Privacy

Financial data is sensitive. The system should follow these rules:

- Use HTTPS in production.
- Hash passwords with BCrypt or Argon2.
- Do not log full transaction descriptions unnecessarily.
- Do not store raw uploaded files forever unless the user allows it.
- Mask account numbers.
- Keep user data isolated by `user_id`.
- Validate file type and size.
- Scan uploaded files in production if possible.
- Encrypt sensitive environment variables.
- Avoid sending full transaction lists in email unless user enables it.
- Add audit logs for upload, delete, report generation, and email sending.

## 22. Error Handling

Common errors:

- Unsupported file type.
- Invalid statement format.
- Empty statement.
- Duplicate statement upload.
- Parser failed for some rows.
- Email provider failure.
- Report generation failure.

The app should show human-readable errors and keep technical details in backend logs.

## 23. Testing Strategy

### Backend Tests

- Unit tests for parsers.
- Unit tests for categorization rules.
- Unit tests for duplicate detection.
- Repository tests for report queries.
- Controller tests for major APIs.
- Integration tests with PostgreSQL using Testcontainers later.

### Frontend Tests

- Component tests for dashboard widgets.
- Form tests for upload and rules pages.
- Service tests for API clients.
- E2E tests for login, upload, categorize, and report generation later.

### Test Data

Maintain anonymized sample statements:

```text
sample-icici-savings.pdf
sample-hdfc-upi.csv
sample-sbi-upi.csv
```

Do not commit real personal financial data.

## 24. Development Phases

### Phase 1: MVP

- Spring Boot project setup.
- Angular project setup.
- PostgreSQL schema.
- Authentication.
- Bank account management.
- PDF statement upload.
- ICICI savings PDF transaction parsing.
- Transaction listing.
- Basic categorization.
- Dashboard summary.
- Manual monthly report generation.
- Manual email report sending.

### Phase 2: Strong Portfolio Version

- CSV import.
- Excel import.
- More bank-specific PDF parsers.
- User-defined rules.
- Duplicate detection.
- Scheduled monthly emails.
- Report history.
- Better dashboard charts.
- Docker Compose setup.
- Backend and frontend tests.

### Phase 3: Advanced Features

- Gmail transaction email import.
- Account Aggregator adapter design.
- Recurring payment detection.
- Budget alerts.
- Anomaly detection.
- AI-generated monthly summary.
- Credit card statement import.

## 25. Credit Card Extension Plan

Credit card support should be added after the bank statement workflow is stable.

Required additions:

- `credit_card_accounts` table.
- Credit card statement upload.
- Credit card transaction parser.
- Billing cycle support.
- Due date tracking.
- Payment detection from bank account transactions.
- Outstanding balance tracking.
- Category reuse from bank transactions.

Potential credit card tables:

```text
credit_card_accounts
credit_card_statement_uploads
credit_card_transactions
credit_card_bills
```

Credit card reports should show:

- Billing cycle spend.
- Statement amount.
- Due date.
- Minimum amount due.
- Payment status.
- Category-wise card expenses.

## 26. Suggested Package Structure

```text
com.example.expensedashboard
  auth
  user
  bankaccount
  upload
  parser
  transaction
  category
  rule
  report
  notification
  audit
  common
```

## 27. Suggested Angular Structure

```text
src/app
  core
    auth
    interceptors
    guards
  shared
    components
    pipes
    models
  features
    dashboard
    transactions
    uploads
    reports
    categories
    rules
    settings
```

## 28. Key Engineering Decisions

- Start with bank statement upload because it is realistic and does not require regulated UPI access.
- Use Angular and Spring Boot because they match the developer's strengths and fit dashboard plus backend-heavy workflows.
- Use PostgreSQL because transactions, categories, accounts, reports, and users are relational.
- Use rule-based categorization first because it is explainable, testable, and user-controllable.
- Keep Python optional for difficult PDF parsing rather than making the MVP polyglot.
- Use scheduled jobs only after manual report generation works.
- Add credit card tracking later to avoid expanding the data model too early.

## 29. MVP Success Criteria

The MVP is successful when:

- A user can register and log in.
- A user can add a bank account.
- A user can upload an ICICI PDF bank statement.
- The system imports and normalizes transactions.
- UPI debits and credits are detected.
- Salary and common expenses are categorized.
- The dashboard shows monthly income, expense, and savings.
- The user can generate and email a monthly report.
