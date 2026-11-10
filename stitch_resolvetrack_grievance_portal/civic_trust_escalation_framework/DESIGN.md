---
name: Civic Trust & Escalation Framework
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#464553'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#777584'
  outline-variant: '#c8c4d5'
  surface-tint: '#544fc0'
  primary: '#1f108e'
  on-primary: '#ffffff'
  primary-container: '#3730a3'
  on-primary-container: '#a9a7ff'
  inverse-primary: '#c3c0ff'
  secondary: '#006a61'
  on-secondary: '#ffffff'
  secondary-container: '#86f2e4'
  on-secondary-container: '#006f66'
  tertiary: '#3a0085'
  on-tertiary: '#ffffff'
  tertiary-container: '#5500bc'
  on-tertiary-container: '#bf9fff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3b35a7'
  secondary-fixed: '#89f5e7'
  secondary-fixed-dim: '#6bd8cb'
  on-secondary-fixed: '#00201d'
  on-secondary-fixed-variant: '#005049'
  tertiary-fixed: '#eaddff'
  tertiary-fixed-dim: '#d2bbff'
  on-tertiary-fixed: '#25005a'
  on-tertiary-fixed-variant: '#5a00c6'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.025em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.005em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies the operational precision, transparent governance, and decisive accountability required for high-stakes public grievance resolution. It serves civil servants, grievance redressal officers, and citizen advocates navigating time-critical citizen appeals. 

The aesthetic is Modern GovTech: institutional authority distilled through contemporary digital product craftsmanship. Rather than bureaucratic clutter and visual noise, the UI communicates clarity, calmness under high caseloads, and systemic velocity. High structural contrast, disciplined density, and unambiguous status semantics eliminate cognitive burden, ensuring zero ambiguity regarding resolution timelines, SLA breaches, and escalation chains.

Bilingual considerations (English and Kannada) are woven natively into layout mechanics, providing balanced visual weight across both scripts without awkward reflow or mismatched line-height baselines.

## Colors

The palette balances administrative gravitas with instant, non-negotiable status feedback.

- **Primary (`#3730A3` - Deep Indigo):** Imparts constitutional authority, stability, and structure. Used for global navigation bars, top-tier interactive components, primary state actions, and formal identifiers.
- **Secondary (`#0D9488` - Deep Teal):** Directs positive workflow navigation, actionable citizen interventions, affirmative filters, and verification workflows.
- **Tertiary (`#7C3AED` - Institutional Violet):** Reserved exclusively for escalation tiers, high-level administrative reviews, and senior secretariat interventions.
- **Neutral Canvas (`#F8FAFC` - Slate Neutral):** A low-strain, soft background that reduces ocular fatigue during prolonged case review sessions.
- **Surface Elevation (`#FFFFFF` - Pure White):** Used on case cards, data tables, and modal dialogs to establish definitive contrast against the canvas.
- **Structural Borders (`#E2E8F0` - Subtle Slate):** Creates crisp separation between dense data modules without visual heaviness.

### Operational State Tokens
The system enforces strict paired token assignments (Foreground Text / Tinted Surface) for case lifecycle auditing:
- **New / Unassigned:** Slate Foreground `#4B5563` on Background `#F3F4F6`.
- **In Progress / Investigation:** Royal Blue Foreground `#2563EB` on Background `#DBEAFE`.
- **Attention / Due Soon (< 48 hrs):** Amber Foreground `#D97706` on Background `#FEF3C7`.
- **Overdue / SLA Breach:** Red Foreground `#DC2626` on Background `#FEE2E2`.
- **Escalated (L1/L2/L3 Secretariat):** Purple Foreground `#7C3AED` on Background `#EDE9FE`.
- **Resolved / Closed:** Emerald Green Foreground `#16A34A` on Background `#DCFCE7`.

## Typography

The typography uses Inter across headlines, body copy, and micro-labels. Inter delivers optical balance, open aperture design, and high disambiguation (e.g., distinguishability between `1`, `l`, and `I`), critical when reading tracking numbers, case IDs, and department codes.

### Bilingual Handling (English and Kannada)
- When secondary Kannada sub-labels accompany English primary labels (e.g., `Department / ಇಲಾಖೆ`), Kannada text is sized 1px smaller than English or rendered with `body-sm` / `label-md` to maintain vertical bounding-box balance.
- Line heights across bilingual input fields and grievance summary blocks must provide at least 150% line-height to accommodate Kannada vowel signs and conjuncts (ottu) without baseline clipping.
- Numeric data, SLA countdown clocks, case health scores, and Grievance IDs must use tabular lining numerals (`font-feature-settings: "tnum"`) to maintain horizontal stability during real-time sorting and metric updates.

## Layout & Spacing

The layout operates on a strict 12-column responsive fluid grid designed for complex supervisory dashboards, operational queues, and split-screen dossier inspection.

### Responsive Breakpoints & Layout Rules
- **Desktop (≥ 1280px):** 12-column grid. Left vertical master navigation (280px fixed width), flexible content canvas with `2rem` outer margins and `1.5rem` gutters. Grievance inspection leverages a 60/40 master-detail column split or a 3-column triage configuration.
- **Tablet (768px – 1279px):** 8-column layout. Master navigation collapses to an icon-rail (72px) or off-canvas drawer. Outer margins reduce to `1.5rem`, gutters maintain `1.25rem`. Complex data grids convert to stacked metric summary cards.
- **Mobile (< 768px):** 4-column layout. Margin shrinks to `1rem` and gutter to `1rem`. Dual-pane inspection views flatten into tabbed linear steps (Details, History, Escalation Actions). Full-width fixed bottom action sheets for field officers handling immediate status resolutions.

All spatial increments follow a strict 4px / 8px linear rhythm. Padding inside operational elements conforms to:
- Compact data tables and filter bars: `space-sm` (8px) vertical, `space-md` (16px) horizontal.
- Inspection panels and summary cards: `space-lg` (24px) internal boundary padding.

## Elevation & Depth

This design system avoids heavy shadows, instead using subtle ambient light occlusion paired with crisp surface boundary strokes (`#E2E8F0`). This creates clear layering without sacrificing clean public-sector utility.

- **Level 0 (Flat / Canvas):** Applied to the base canvas (`#F8FAFC`). No shadow, no outline.
- **Level 1 (Card / Resting Surface):** Applied to grievance records, statistic panels, and filter modules. Border: `1px solid #E2E8F0`. Shadow: `0 1px 3px 0 rgba(15, 23, 42, 0.05), 0 1px 2px -1px rgba(15, 23, 42, 0.05)`.
- **Level 2 (Hover / Active Row / Dropdowns):** Applied to active list cards during review and selector menus. Border: `1px solid #CBD5E1`. Shadow: `0 4px 6px -1px rgba(15, 23, 42, 0.07), 0 2px 4px -2px rgba(15, 23, 42, 0.05)`.
- **Level 3 (Modals / Escalation Drawers):** Applied to formal escalation assignment dialogs and audit logs. Border: `1px solid #E2E8F0`. Shadow: `0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.05)`.

Interactive elements under focus must project a non-offset focus ring: `box-shadow: 0 0 0 3px rgba(55, 48, 163, 0.2)`.

## Shapes

The interface balances welcoming civic accessibility with institutional discipline. Surfaces and structural containers adhere to a standardized 12px corner geometry (`rounded-xl` / `0.75rem`), avoiding both cold industrial sharpness and overly playful roundedness.

- **Structural Containers & Cards:** Strictly `12px` (`0.75rem`) border-radius.
- **Interactive Form Inputs & Buttons:** Scaled to `8px` (`0.5rem`) border-radius (`rounded-lg`) to preserve crisp mechanical affordance.
- **Pill Badges & Metrics (Status Tags, Health Index):** Fully rounded pill geometry (`9999px`) to immediately set status chips apart from structural rectangular cards.
- **Language Switchers & Segmented Switches:** Outer container `8px`, internal pill active toggles `6px`.

## Components

### Buttons
- **Primary:** Deep Indigo (`#3730A3`) fill with white (`#FFFFFF`) label, medium font weight (`font-semibold`). Hover state shifts to `#312E81`. Focus ring: 3px Indigo at 25% opacity.
- **Secondary / Action:** Teal (`#0D9488`) fill with white text for decisive field interventions (e.g., "Assign Field Officer", "Submit Verification"). Hover shifts to `#0F766E`.
- **Outline / Neutral:** Pure white fill, border `1px solid #E2E8F0`, text slate `#334155`. Hover shifts to `#F1F5F9`.
- **Destructive / Escalation:** Deep Red (`#DC2626`) fill for critical rejection or formal breach tagging; Purple (`#7C3AED`) fill for invoking Secretariat escalation protocols.

### Status Pills & Health Score Badges
- **Status Pills:** Pill-shaped (`rounded-full`), padding `4px 10px`, typography `label-sm`. Always rendered with high-contrast pairing:
  - *Resolved:* `#16A34A` text on `#DCFCE7` background.
  - *Due Soon:* `#D97706` text on `#FEF3C7` background.
  - *Overdue:* `#DC2626` text on `#FEE2E2` background.
  - *In Progress:* `#2563EB` text on `#DBEAFE` background.
  - *Escalated:* `#7C3AED` text on `#EDE9FE` background.
- **Health Score Indicator (0-100):** Visual meter and badge combo. Scores 80-100 (Healthy) use Emerald; 50-79 (Warning) Amber; 0-49 (Critical) Crimson Red. Displays metric integer alongside a small circular dot indicator.

### Input Fields & Search
- Background `#FFFFFF`, border `1px solid #E2E8F0`, text `#0F172A`, placeholder `#94A3B8`.
- Height: 40px (Desktop), 44px (Mobile).
- Includes optional right-aligned Kannada hint helper text (e.g., "Enter Grievance ID / ಕುಂದುಕೊರತೆ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ").

### Case Cards
- Surface `#FFFFFF`, border `1px solid #E2E8F0`, rounded `12px`, internal padding `20px`.
- Left-edge accent spine: A 4px vertical border segment on the card's leftmost edge, color-coded by grievance health/escalation level for instant scanning down a high-density queue.

### Bilingual Toggle (Language Switcher)
- Segmented control format. Container styled in `#F1F5F9` with `4px` padding and `8px` rounded corners.
- Inactive segment: Text `#64748B`, transparent background.
- Active segment: `#FFFFFF` surface with `Level 1` shadow, text `#3730A3` (`font-semibold`), label: "English | ಕನ್ನಡ".

### Escalation Stepper / Timeline
- Vertical tracking lineage showing grievance progression from Taluk to District to State Secretariat. Completed stages link via solid `#16A34A` rules; active disputed stages link with dashed `#7C3AED` (Escalated) or `#DC2626` (SLA Exceeded) visual connecting lines.