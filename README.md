# Frostkeep 1.1

Native Android inventory, customer pricing, sales and batch billing for a small frozen-food business. Works offline with INR prices and whole-unit stock. Requires Android 8.0 or later.

## Install or update

Install `releases/Frostkeep-1.1-debug.apk` on your phone. Install it over the previous Frostkeep APK to retain your data; do not uninstall the old app first. Android may ask you to allow installation from the app opening the file. This APK is development-signed for testing and personal use, not a Play Store release.

## Inventory

All 32 starter products are included in Frozen, Masale and Sauces. Add, edit, delete, search and filter products. Each product has a **buying price**, whole-unit stock, packaging unit and low-stock threshold. Its total is buying price × quantity. Use the **− / +** controls around the quantity for quick stock corrections, or **Add stock** for larger deliveries. These adjustments are inventory corrections, not sales.

### Existing data from 1.0

Old prices were selling prices. The upgrade preserves those values for review, but marks them as unconfirmed buying costs. Open **Edit item**, enter or review the buying cost, and save to confirm it. The inventory valuation only includes confirmed costs. A local copy of the original version-1 data is retained during migration.

Historical sales keep their original revenue. Since they did not store buying costs, the app does not invent historical profit for them.

## Customers and quoted prices

1. Open **Customers → Add customer**.
2. Enter a name and optional phone number. Include the international prefix, for example `+919876543210`.
3. Open **Details & quoted prices → Quote a product**.
4. Pick an inventory product and save this customer's selling price.

The same product can have different prices for each customer. Quotes can be changed or removed without changing old orders. Customer details can be edited or deleted; historical customer names remain on existing sales and bills, and pending orders remain in Orders.

## Individual sales

Choose a product through Inventory or **Sales → Record a sale**. Select a customer optionally; their quoted price fills automatically. If there is no quote, enter a selling price. The selling price can always be overridden for that sale without changing the saved quote.

The date defaults to today. **Change date** opens a picker for recording earlier sales. Enter a quantity within current available stock and record the sale. Revenue charts use the selected date. Backdated sales capture the buying cost currently recorded when the sale is entered; the app does not reconstruct past purchase costs or inventory levels.

## Batch orders

1. Open **Orders → New batch order** and select a customer.
2. Choose the date and **Pending payment** or **Paid**.
3. Add products and quantities. Saved customer quotes fill in automatically; enter a price for products without a quote or override an existing quote for this order.
4. Review the total and gross profit, then tap **Generate bill**.

All order lines are validated before any stock changes. Generating an order deducts stock and records revenue whether the order is paid or pending. Prices, item names, customer name and confirmed buying costs are saved as historical snapshots. Subsequent price changes do not change old bills or margins.

Open an order to view its bill, copy the text, or mark payment as received. Payment changes update outstanding balances without adding another sale or deducting more stock. An order can be moved back to pending to correct a mistake. **Void batch order** restores every line to stock and removes its revenue, profit and pending balance. Individual batch lines cannot be voided separately. Deleted products prevent stock restoration; the app reports this rather than partially voiding an order.

### Bills and WhatsApp

Bills show the customer, bill number, date, payment status, each item's quantity × selling price, line totals, grand total and amount due. Buying costs and profit never appear in customer-facing bill text.

**Copy bill text** copies a ready-to-paste message. When the customer has a phone number, **Open WhatsApp with bill** opens a prefilled chat and **Copy WhatsApp link** copies its link. You still review and send the message yourself. If the customer is still in the directory, their current phone number is used; otherwise the order's saved phone is used.

Links follow [WhatsApp's documented click-to-chat format](https://faq.whatsapp.com/5913398998672934). Opening the chat requires an appropriate installed app or browser and connectivity; core stock and billing features work offline. WhatsApp delivery is not automatic and is not tracked.

## Insights

The separate Insights page includes 7/30-day revenue graphs, category revenue, units sold, transaction counts and top products. A batch counts as one transaction, with its individual products included in category and product totals.

Gross profit is selling revenue minus the buying costs captured when recording sales. It can be negative. Sales lacking confirmed costs are excluded from the profit figure with an explicit notice. Pending-payment orders count toward revenue and gross profit; outstanding balances are shown separately in Orders. Expenses, taxes, partial payments and purchase-batch/FIFO costing are not included.

## Settings and data backups

Open **Settings → Export full backup**. Android's file picker lets you save a timestamped JSON file in **Downloads** or another location. The export includes all inventory, customer details and quotes, sales, batch lines, historical prices and costs, payment states, void history, and the bill numbering sequence. Bills can be reconstructed from these records.

The app reports success only after the file is written and closed. Cancelling the picker leaves data unchanged. Backups are manual and include customer contact details. This version provides export only; an in-app restore/import flow is not yet included. Clearing app storage or uninstalling removes the local working data.

## Build and verify

Use JDK 17 and Android SDK platform/build tools 34. Set `sdk.dir` in `local.properties` for your SDK installation, then:

```sh
./gradlew assembleDebug lintDebug
./scripts/test-ledger.sh
./gradlew assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.frostkeep.app.test/com.frostkeep.app.SmokeInstrumentation
```

The Gradle wrapper is included. First builds require dependencies to be downloaded unless already cached. No third-party runtime libraries are used.

The standalone suite covers 68 business-rule checks: money, stock limits, quotes, atomic batches, profit and history snapshots, payment transitions, voiding, backdating and exact WhatsApp text encoding. Device instrumentation adds 14 serialization, legacy migration, bill reconstruction and backup roundtrip checks using Android's actual JSON implementation. Instrumentation without arguments does not alter the app's saved ledger. The optional `-e seed true` argument replaces the ledger with a fixture and is only for disposable test emulators.

## Source layout

- `MainActivity.java`: UI, dialogs, date picker, chart, clipboard/WhatsApp actions and Android backup file picker.
- `Ledger.java`: domain model, validation, stock, customers, quotes, orders, payments and profit arithmetic.
- `LedgerStore.java`: versioned JSON persistence, backup format and migration from version 1.
- `Billing.java`: plain-text bills and encoded WhatsApp links.
- `LedgerTest.java`: standalone business-rule tests.
- `SmokeInstrumentation.java`: device storage and migration tests.

Before store publication, configure a private release signing key, modernize the target SDK and test across supported Android versions.
