#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p app/build/ledger-tests
javac -d app/build/ledger-tests app/src/main/java/com/frostkeep/app/Ledger.java app/src/main/java/com/frostkeep/app/Billing.java app/src/test/java/com/frostkeep/app/LedgerTest.java
java -cp app/build/ledger-tests com.frostkeep.app.LedgerTest
