package com.frostkeep.app;

import org.json.*;
import java.util.*;

/** Versioned storage and full-fidelity JSON backup, including historical cost/price snapshots. */
public final class LedgerStore {
    public static final int VERSION = 2;
    private LedgerStore() {}
    public static String encode(Ledger ledger) {
        try {
            JSONObject root = new JSONObject();
            JSONArray items = new JSONArray(), sales = new JSONArray(), customers = new JSONArray(), orders = new JSONArray();
            for(Ledger.Item i:ledger.items) {
                JSONObject o = new JSONObject();
                o.put("id", i.id); o.put("name", i.name); o.put("category", i.category); o.put("unit", i.unit);
                o.put("price", i.price); o.put("costConfirmed", i.costConfirmed); o.put("quantity", i.quantity); o.put("low", i.low);
                items.put(o);
            }
            for(Ledger.Sale s:ledger.sales) {
                JSONObject o = new JSONObject();
                o.put("id", s.id); o.put("itemId", s.itemId); o.put("name", s.name); o.put("category", s.category); o.put("unit", s.unit);
                o.put("price", s.price); o.put("cost", s.cost); o.put("quantity", s.quantity); o.put("time", s.time); o.put("voided", s.voided);
                o.put("customerId", s.customerId); o.put("customerName", s.customerName); o.put("orderId", s.orderId); sales.put(o);
            }
            for(Ledger.Customer c:ledger.customers) {
                JSONObject o = new JSONObject(); o.put("id", c.id); o.put("name", c.name); o.put("phone", c.phone);
                o.put("quotes", new JSONObject(c.quotes)); customers.put(o);
            }
            for(Ledger.Order order:ledger.orders) {
                JSONObject o = new JSONObject();
                o.put("id", order.id); o.put("number", order.number); o.put("customerId", order.customerId);
                o.put("customerName", order.customerName); o.put("phone", order.phone); o.put("time", order.time);
                o.put("paid", order.paid); o.put("voided", order.voided);
                JSONArray lines = new JSONArray(); for(Ledger.Sale s:order.lines) lines.put(s.id);
                o.put("saleIds", lines); orders.put(o);
            }
            root.put("version", VERSION); root.put("app", "Frostkeep"); root.put("currency", "INR");
            root.put("nextOrderNumber", ledger.nextOrderNumber);
            root.put("items", items); root.put("sales", sales); root.put("customers", customers); root.put("orders", orders);
            return root.toString(2);
        } catch(JSONException e) { throw new IllegalStateException("Could not encode saved data", e); }
    }
    public static Ledger decode(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        int version = root.getInt("version");
        if(version < 1 || version > VERSION) throw new JSONException("Unsupported data version");
        Ledger l = new Ledger();
        JSONArray items = root.getJSONArray("items"), sales = root.getJSONArray("sales");
        for(int n = 0; n < items.length(); n++) {
            JSONObject o = items.getJSONObject(n);
            Ledger.Item i = new Ledger.Item(o.getString("name"), o.getString("category"));
            i.id = o.getString("id"); i.unit = o.getString("unit"); i.price = o.getLong("price");
            // Version 1 stored selling prices: preserve the value but do not treat it as a verified cost.
            i.costConfirmed = version >= 2 && o.getBoolean("costConfirmed");
            i.quantity = o.getInt("quantity"); i.low = o.getInt("low"); l.items.add(i);
        }
        Map<String, Ledger.Sale> saleById = new HashMap<>();
        for(int n = 0; n < sales.length(); n++) {
            JSONObject o = sales.getJSONObject(n); Ledger.Sale s = new Ledger.Sale();
            s.id = o.getString("id"); s.itemId = o.getString("itemId"); s.name = o.getString("name");
            s.category = o.getString("category"); s.unit = o.getString("unit"); s.price = o.getLong("price");
            s.quantity = o.getInt("quantity"); s.time = o.getLong("time"); s.voided = o.getBoolean("voided");
            if(version >= 2) {
                s.cost = o.getLong("cost"); s.customerId = o.getString("customerId");
                s.customerName = o.getString("customerName"); s.orderId = o.getString("orderId");
            }
            l.sales.add(s); saleById.put(s.id, s);
        }
        if(version >= 2) {
            JSONArray customers = root.getJSONArray("customers"), orders = root.getJSONArray("orders");
            for(int n = 0; n < customers.length(); n++) {
                JSONObject o = customers.getJSONObject(n); Ledger.Customer c = new Ledger.Customer();
                c.id = o.getString("id"); c.name = o.getString("name"); c.phone = o.getString("phone");
                JSONObject quotes = o.getJSONObject("quotes");
                Iterator<String> keys = quotes.keys(); while(keys.hasNext()) { String id = keys.next(); c.quotes.put(id, quotes.getLong(id)); }
                l.customers.add(c);
            }
            for(int n = 0; n < orders.length(); n++) {
                JSONObject o = orders.getJSONObject(n); Ledger.Order order = new Ledger.Order();
                order.id = o.getString("id"); order.number = o.getString("number"); order.customerId = o.getString("customerId");
                order.customerName = o.getString("customerName"); order.phone = o.getString("phone"); order.time = o.getLong("time");
                order.paid = o.getBoolean("paid"); order.voided = o.getBoolean("voided");
                JSONArray lines = o.getJSONArray("saleIds");
                for(int x = 0; x < lines.length(); x++) {
                    Ledger.Sale s = saleById.get(lines.getString(x));
                    if(s == null || !s.orderId.equals(order.id)) throw new JSONException("Missing order line");
                    order.lines.add(s);
                }
                l.orders.add(order);
            }
            l.nextOrderNumber = root.getInt("nextOrderNumber");
        }
        return l;
    }
}
