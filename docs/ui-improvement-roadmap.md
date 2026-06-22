# UI Improvement Roadmap

This document captures practical frontend improvements for UPI Expense Intel. The goal is to make the application feel like a focused personal finance workstation instead of a set of separate CRUD screens.

## Recommended Priority

1. Transactions review experience
2. Upload flow guidance
3. Dashboard polish
4. Session and auth UX
5. Merchant rule assistance
6. Reports page
7. App shell and navigation refinement

## 1. Transactions Review Experience

The Transactions page is the most important daily-use screen after statement upload. It should help users quickly review imported rows, fix categories, and identify transactions that need attention.

Suggested improvements:

- Add sticky filters so month, category, type, merchant search, ignored, and transfer filters stay visible while scrolling.
- Improve inline category editing so category changes feel fast and low-friction.
- Add quick filter chips for common states such as `UPI`, `Food`, `Ignored`, `Transfer`, and `Needs Review`.
- Highlight low-confidence categorization rows.
- Show a clear `Review` badge for fallback or low-confidence categories.
- Add better mobile layout for transaction rows.
- Later enhancement: add bulk actions for category update, ignored, transfer, and re-categorization.

## 2. Upload Flow Guidance

Statement upload is the primary data-entry workflow. The current page can be made more guided and confidence-building.

Suggested flow:

1. Choose bank account and statement month.
2. Select PDF and optional password.
3. Upload and parse statement.
4. Show parsing result summary.
5. Link directly to imported transactions for review.

Suggested improvements:

- Present upload as a step-based workflow.
- Show empty-account guidance when no bank account exists.
- Show parsed rows, skipped duplicate rows, failed rows, and upload status more prominently.
- Add a clear next action after successful upload: `Review transactions`.
- Improve error display for wrong password, unsupported PDF, duplicate upload, and parser failure.

## 3. Dashboard Polish

The Dashboard should give a fast monthly financial readout and point users toward useful next actions.

Suggested improvements:

- Add current month versus previous month comparison.
- Add savings trend or savings-rate trend.
- Add category change indicators.
- Show recent large transactions.
- Show an empty state when no statement is uploaded for the selected month.
- Add clearer chart states for loading, empty data, and errors.
- Consider adding a small `Needs review` summary if there are low-confidence or uncategorized transactions.

## 4. Session And Auth UX

When an old token expires, the app should feel intentional rather than broken.

Suggested improvements:

- After automatic logout from a `401` or `403`, show a login-page message such as `Your session expired. Please sign in again.`
- Preserve the originally requested route when redirecting to login.
- After successful login, redirect back to the original route if available.
- Add a lightweight loading state while auth-sensitive pages initialize.

## 5. Merchant Rule Assistance

Merchant rules should be connected directly to the transaction correction workflow.

Suggested improvements:

- When a user changes a transaction category, offer to create a matching merchant rule.
- Suggested prompt: `Always categorize similar transactions as Food?`
- Pre-fill rule keyword from merchant name or normalized description.
- Let the user choose match type and priority before saving.
- After creating or editing a rule, offer to re-run categorization for the selected month.

## 6. Reports Page

The original MVP includes monthly reports. The UI should support manual report generation before scheduled email automation.

Suggested page:

- Route: `/reports`
- Month picker.
- `Generate report` action.
- Report history list.
- Report preview with income, expense, savings, category breakdown, top merchants, and daily trend.
- Later enhancement: `Email report` action after backend email sending is implemented.

Suggested implementation order:

1. Add frontend route and navigation item.
2. Add report models and service.
3. Add report generation form.
4. Add report history and preview.
5. Add email action after backend support exists.

## 7. App Shell And Navigation Refinement

The authenticated app shell should feel compact, predictable, and suited for repeated use.

Suggested improvements:

- Make the sidebar more compact and scan-friendly.
- Improve active route indicators.
- Add consistent page headers with primary action placement.
- Add month/account context where useful.
- Keep dense data pages utilitarian and avoid marketing-style card layouts.
- Ensure responsive behavior is polished for mobile and tablet widths.

## Design Direction

The UI should feel:

- Quiet and work-focused.
- Dense but organized.
- Financially trustworthy.
- Fast to scan.
- Comfortable for repeated monthly review.

Avoid:

- Oversized hero sections inside the authenticated app.
- Decorative card-heavy layouts.
- One-note color palettes.
- Visual effects that reduce table readability.
- In-app instructional text that explains obvious controls.

## Suggested Next UI Task

Start with the Transactions page:

1. Add low-confidence and uncategorized visual states.
2. Improve filter layout and quick filter chips.
3. Make inline category editing more ergonomic.
4. Add a clear review workflow for transactions that need attention.

This gives the highest user value because transaction review is the core workflow after importing statements.
