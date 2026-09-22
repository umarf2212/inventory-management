package com.frostkeep.app;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** Customer-visible bills deliberately contain no buying costs or margins. */
public final class Billing {
    private Billing() {}
    public static String amount(long paise) { return "₹" + BigDecimal.valueOf(paise, 2).toPlainString(); }
    public static String text(Ledger.Order order) {
        StringBuilder b = new StringBuilder("FROSTKEEP\nBill ").append(order.number)
            .append("\nCustomer: ").append(order.customerName)
            .append("\nDate: ").append(Instant.ofEpochMilli(order.time).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
            .append("\nStatus: ").append(order.voided ? "VOIDED" : order.paid ? "PAID" : "PENDING PAYMENT").append("\n\n");
        int n = 1;
        for(Ledger.Sale s:order.lines) b.append(n++).append(". ").append(s.name).append("\n   ")
            .append(s.quantity).append(" ").append(s.unit).append("(s) × ").append(amount(s.price))
            .append(" = ").append(amount(s.total())).append("\n");
        b.append("\nGrand total: ").append(amount(order.total()));
        if(!order.voided) b.append("\nAmount due: ").append(amount(order.paid ? 0 : order.total()));
        return b.append("\n\nThank you!").toString();
    }
    public static String whatsapp(Ledger.Order order) {
        return whatsapp(order.phone, text(order));
    }
    public static String whatsapp(String number, String bill) {
        String phone = Ledger.phone(number);
        if(phone.isEmpty()) throw new IllegalArgumentException("Add a phone number with a country code to open WhatsApp.");
        try { return "https://wa.me/" + phone.substring(1) + "?text=" + URLEncoder.encode(bill, StandardCharsets.UTF_8.name()).replace("+", "%20"); }
        catch(java.io.UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }
}
