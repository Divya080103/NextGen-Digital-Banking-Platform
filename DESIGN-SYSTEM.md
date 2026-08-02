# NextGen Digital Banking Platform — Refreshed Design System

**Read this before writing any UI code.** This is the single source of truth for color, type, spacing, and component behavior across the whole platform.

## 0. Refreshed Direction: Dark-Nav / Warm-Canvas Fintech

Our refreshed aesthetic synthesizes a modern SaaS fintech feel (inspired by modern digital banking dashboards):
- **Dark Pine Sidebar / Navigation**: Solid dark pine (`#0F241C` / `#1B3A2C`) for navigation frame and dark chrome.
- **Warm Canvas Content Area**: Warm cream canvas (`#F6F2E8` / `#EEE7D6`) for all tables, forms, and cards — dense financial data stays legible on high-contrast light backgrounds.
- **Gold Accents & Highlights**: Muted gold (`#C9974B`) for primary actions, active tabs, and hero moments.
- **Functional AI Surface**: Floating glass pill (`Ask NextGen AI`) for conversational risk analysis and intelligent search.

---

## 1. Color Tokens

```css
:root {
  /* Nav / Dark Surface */
  --color-pine-950: #0F241C;
  --color-pine-800: #1B3A2C;

  /* Content Canvas — Warm, not stark white */
  --color-cream-50: #F6F2E8;
  --color-cream-100: #EEE7D6;
  --color-cream-200: #E2D8BF;

  /* Text */
  --color-ink-900: #14171C;
  --color-text-secondary: #5C6259;
  --color-text-inverse: #F3EFE4;
  --color-text-muted-inverse: #9CA89D;

  /* Primary Accent */
  --color-gold-500: #C9974B;
  --color-gold-600: #A8792F;
  --color-gold-100: #F1E4C9;

  /* Data-Viz / Secondary Accents */
  --color-cobalt-500: #3B6FE0;
  --color-rose-500: #E85C8A;
  --color-emerald-500: #2E9E6D;

  /* Semantic Status */
  --color-success-600: #2E9E6D;  --color-success-100: #DCEFE4;
  --color-warning-600: #B9861F;  --color-warning-100: #F3E7C9;
  --color-danger-600:  #B4453A;  --color-danger-100:  #F3DEDB;
  --color-info-600:    #3B6FE0;  --color-info-100:    #DEE6F7;

  /* Glass Surfaces */
  --glass-fill: rgba(255, 255, 255, 0.65);
  --glass-blur: 16px;
}
```

---

## 2. Typography

| Role | Typeface | Usage |
|---|---|---|
| **Display / Headlines** | *General Sans* (weight 600–700, geometric sans) | Page titles, hero balance figures, big confident numerical summaries. |
| **Body / UI** | *Inter* | All standard UI text: labels, buttons, form fields, table headers, nav items. |
| **Numerals / Tabular** | *IBM Plex Mono* (tabular figures) | Account numbers, transaction IDs, reference numbers, and data table columns. |

---

## 3. Signature Components

### 1. Horizon Card (`HorizonCard`)
- Hero balance card for customer dashboard.
- Background: `--color-pine-950` (`#0F241C`), text: `--color-text-inverse`.
- Organic asymmetrical border-radius curve with abstract vector horizon SVG & gold sun graphic in top-right.
- Headline balance figure displayed in bold geometric sans (`General Sans` / `Plus Jakarta Sans`).
- Variant `"compact"` for secondary account cards without landscape vector artwork.

### 2. Ask NextGen AI Bar (`AskAIBar`)
- Floating pill-shaped bar anchored at the bottom of views.
- Glass surface: `background: var(--glass-fill)`, `backdrop-filter: blur(var(--glass-blur))`.
- Fallback solid background for `prefers-reduced-transparency`.
- Wired to conversational AI loan risk evaluation data on Loan screens (`why was I marked medium risk?`), disabled with "Coming soon" state on other views.

---

## 4. Accessibility Rules

- WCAG AA contrast ratio (> 4.5:1) maintained for `--color-text-secondary` (`#5C6259`) on `--color-cream-100` (`#EEE7D6`).
- Visible focus rings (`2px solid var(--color-gold-500)`) on all interactive inputs/buttons.
- Status is never communicated by color alone — always paired with explicit text.
- Fallback solid background for glass elements under `prefers-reduced-transparency`.
