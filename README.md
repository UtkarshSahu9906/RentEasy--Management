# RentEasy 🏪

A rental management Android app for small businesses — track customers, rentals, missing items, damage records, and generate professional bills.

---

## Screenshots

| Dashboard | Customers | Customer Detail | Generate Bill |
|-----------|-----------|-----------------|---------------|
| ![Dashboard](screenshots/ss_1.jpeg) | ![Customers](screenshots/ss_2.jpeg) | ![Detail](screenshots/ss_3.jpeg) | ![Bill](screenshots/ss_4.jpeg) |

---

## Features

- **Dashboard** — live stats: customers, active rentals, revenue, security held, unpaid missing items
- **Currency selector** — supports ₹ INR, $ USD, € EUR, ₱ PHP and 14 more
- **Customer management** — add, search, and view all customers
- **Rental tracking** — monthly / daily / hourly billing with configurable overage policy
- **Advance payment & security deposit** — auto-deducted from final bill
- **Damage records** — link to rental item, deduct from deposit or charge separately
- **Missing items** — track unreturned/lost items with paid/unpaid status
- **Bill generation** — dedicated billing screen with active-item policy options
- **PDF export** — print or save professional invoice as PDF
- **Share bill** — send via WhatsApp, SMS, Gmail or any installed app
- **Bill history** — every generated bill saved per customer with totals and status

---

## Tech Stack

- **Language** — Java
- **UI** — Android Views + Material Design 3
- **Auth** — Firebase Authentication (Google Sign-In)
- **Database** — Firebase Firestore (real-time)
- **Build** — Gradle with Version Catalog

---

## Project Structure

```
app/src/main/java/com/renteasy/
├── activities/
│   ├── LoginActivity.java
│   ├── MainActivity.java
│   ├── CustomerListActivity.java
│   ├── CustomerDetailActivity.java
│   ├── AddCustomerActivity.java
│   ├── AddRentalActivity.java
│   ├── AddMissingItemActivity.java
│   ├── AddDamageActivity.java
│   ├── BillActivity.java
│   ├── BillHistoryActivity.java
│   └── BillingPolicyActivity.java
├── adapters/
│   ├── CustomerAdapter.java
│   ├── RentalAdapter.java
│   ├── MissingItemAdapter.java
│   ├── DamageAdapter.java
│   └── BillHistoryAdapter.java
├── models/
│   ├── Customer.java
│   ├── Rental.java
│   ├── MissingItem.java
│   ├── DamageItem.java
│   └── Bill.java
└── utils/
    ├── CurrencyManager.java
    ├── BillingPolicy.java
    └── BillGenerator.java
```

---

## Setup

1. Clone the repository
2. Add `google-services.json` to the `app/` folder (from Firebase Console)
3. In `build.gradle` (project root), add:
   ```gradle
   id 'com.google.gms.google-services' version '4.4.1' apply false
   ```
4. In `app/build.gradle`, add:
   ```gradle
   id 'com.google.gms.google-services' version '4.4.1'
   ```
5. Sync Gradle and run

---

## Billing Policy

Configurable from **Settings → Billing Policy**:

| Scenario | Options |
|----------|---------|
| Daily rental with leftover hours | Charge exact hours / Round up to full day / Grace period |
| Monthly rental with leftover hours | Charge exact hours / Round up to full day |
| Active rental at bill time | Calculate up to now / Mark as pending |

---

## Firestore Structure

```
customers/{customerId}
  ├── rentals/{rentalId}
  ├── missingItems/{itemId}
  ├── damages/{damageId}
  └── bills/{billId}
```

---

## License
© 2026 Utkarsh. All rights reserved.
This project is private and not open for redistribution.
