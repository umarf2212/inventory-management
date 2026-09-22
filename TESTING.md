# Frostkeep 1.1 verification

## Automated checks

- JDK 17 / Gradle 8.2: `assembleDebug` and `lintDebug` succeeded. No lint errors; warnings cover target SDK age, backup policy attributes, and English-only UI strings.
- `scripts/test-ledger.sh`: 68 passing business-rule checks.
- Android 13 instrumentation: 14 passing storage, schema-migration, historical bill reconstruction, and JSON-file roundtrip checks.
- The APK signature was verified with `apksigner`.

## Android 13 emulator checks

Testing used an isolated temporary emulator with sample data, not a user's phone or the existing emulator's data.

- Created a customer with an international phone number and a product quote.
- Confirmed the quote filled into a batch order and an individual sale.
- Created a two-product pending order for ₹380.00. Quantities were deducted once; buying-cost snapshots produced ₹130.00 gross profit.
- Verified the bill showed quantity multipliers, per-unit selling prices, line totals, grand total and amount due. Internal profit was displayed separately from copied bill text.
- Exercised Copy bill and Copy WhatsApp link. Actual WhatsApp account delivery was not tested or attempted.
- Marked the order paid and verified the paid state persisted without duplicate sales.
- Installed the final APK over the test installation and verified customers, quotes, stock, sales and orders survived the update.
- Verified the inventory plus/minus controls each changed and saved the quantity correctly.
- Selected an earlier sale date in the date picker, recorded the customer sale, and verified its date persisted while today’s revenue stayed unchanged.
- Exported a full backup through Android's file picker to Downloads and compared its parsed contents with the entire live ledger; they matched exactly.

The production APK contains no emulator sample customers, stock or sales. Screenshots under `screenshots/v1.1` illustrate the test fixture.
