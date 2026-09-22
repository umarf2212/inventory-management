package com.frostkeep.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class Ledger {
    public static final String[] CATEGORIES = {"Frozen", "Masale", "Sauces"};
    public static final class Item {
        public String id = UUID.randomUUID().toString(), name, category, unit = "pack";
        // price is the buying cost in paise.
        public long price; public boolean costConfirmed; public int quantity, low = 5;
        public Item(String name, String category) { this.name = name; this.category = category; }
        public long total() { return price * quantity; }
    }
    public static final class Sale {
        public String id = UUID.randomUUID().toString(), itemId, name, category, unit;
        public String customerId = "", customerName = "", orderId = "";
        public long price, time, cost = -1; public int quantity; public boolean voided;
        public long profit() { return cost < 0 ? 0 : (price - cost) * quantity; }
        public long total() { return price * quantity; }
    }
    public static final class Customer {
        public String id = UUID.randomUUID().toString(), name = "", phone = "";
        public final Map<String, Long> quotes = new LinkedHashMap<>();
    }
    public static final class OrderLine {
        public final String itemId;
        public final int quantity;
        public final long price;
        public OrderLine(String itemId, int quantity, long price) {
            this.itemId = itemId; this.quantity = quantity; this.price = price;
        }
    }
    public static final class Order {
        public String id = UUID.randomUUID().toString(), number, customerId, customerName, phone;
        public long time; public boolean paid, voided;
        public final List<Sale> lines = new ArrayList<>();
        public long total() { long n = 0; for(Sale s:lines) n += s.total(); return n; }
        public long profit() { long n = 0; for(Sale s:lines) n += s.profit(); return n; }
        public boolean costKnown() { for(Sale s:lines) if(s.cost < 0) return false; return true; }
    }
    public final List<Customer> customers = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    public int nextOrderNumber = 1;
    public final List<Item> items = new ArrayList<>();
    public final List<Sale> sales = new ArrayList<>();
    public static long parsePrice(String value) {
        try {
            BigDecimal n = new BigDecimal(value.trim());
            if (n.signum() < 0 || n.compareTo(new BigDecimal("10000000")) > 0) throw new IllegalArgumentException();
            return n.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (Exception e) { throw new IllegalArgumentException("Enter a price from 0 to 10,000,000 with at most 2 decimal places."); }
    }
    public static int parseQuantity(String value) {
        try { int n = Integer.parseInt(value.trim()); if(n < 0 || n > 1000000) throw new Exception(); return n; }
        catch(Exception e) { throw new IllegalArgumentException("Enter a whole quantity from 0 to 1,000,000."); }
    }
    public Item find(String id) { for(Item i:items) if(i.id.equals(id)) return i; return null; }
    public Customer customer(String id) { for(Customer c:customers) if(c.id.equals(id)) return c; return null; }
    public static String phone(String value) {
        String p = value.trim().replaceAll("[ ()-]", "");
        if(!p.isEmpty() && !p.matches("\\+[1-9][0-9]{7,14}"))
            throw new IllegalArgumentException("Include the country code, for example +919876543210, or leave the phone blank.");
        return p;
    }
    public void adjust(Item item, int delta) {
        if(!items.contains(item) || (long)item.quantity + delta < 0 || (long)item.quantity + delta > 1000000)
            throw new IllegalArgumentException("Stock must stay between 0 and 1,000,000.");
        item.quantity += delta;
    }
    private void validateSale(Item item, int quantity, long price, long time) {
        if(time < 0 || time > System.currentTimeMillis() + 1000)
            throw new IllegalArgumentException("Choose today or an earlier sale date.");
        if (!items.contains(item) || quantity <= 0 || quantity > item.quantity || price <= 0 || price > 1000000000L)
            throw new IllegalArgumentException("Enter a positive price and a quantity within available stock.");
    }
    public Sale sell(Item item, int quantity, long price, long time) {
        return sell(item, quantity, price, time, null);
    }
    public Sale sell(Item item, int quantity, long price, long time, Customer customer) {
        validateSale(item, quantity, price, time);
        if(customer != null && !customers.contains(customer)) throw new IllegalArgumentException("Choose an existing customer.");
        Sale s = new Sale(); s.itemId = item.id; s.name = item.name; s.category = item.category; s.unit = item.unit;
        s.price = price; s.quantity = quantity; s.time = time; s.cost = item.costConfirmed ? item.price : -1;
        if(customer != null) { s.customerId = customer.id; s.customerName = customer.name; }
        item.quantity -= quantity; sales.add(0, s); return s;
    }
    public void voidSale(Sale sale) {
        if (!sales.contains(sale) || sale.voided) throw new IllegalArgumentException("This sale has already been voided.");
        if(!sale.orderId.isEmpty()) throw new IllegalArgumentException("Void the whole batch order from Batch orders.");
        Item i = find(sale.itemId);
        if(i == null) throw new IllegalArgumentException("The item was deleted, so its stock cannot be restored.");
        if(i.quantity + sale.quantity > 1000000) throw new IllegalArgumentException("Restoring this sale exceeds the stock limit.");
        i.quantity += sale.quantity; sale.voided = true;
    }
    public Order createOrder(Customer customer, List<OrderLine> requested, boolean paid, long time) {
        if(!customers.contains(customer)) throw new IllegalArgumentException("Choose a customer.");
        if(requested.isEmpty()) throw new IllegalArgumentException("Add at least one product.");
        Set<String> seen = new HashSet<>();
        // Validate the complete order before changing any stock.
        for(OrderLine line:requested) {
            if(!seen.add(line.itemId)) throw new IllegalArgumentException("Combine duplicate products into one line.");
            validateSale(find(line.itemId), line.quantity, line.price, time);
        }
        Order order = new Order();
        order.number = String.format(Locale.US, "FK-%05d", nextOrderNumber++);
        order.customerId = customer.id; order.customerName = customer.name; order.phone = customer.phone;
        order.time = time; order.paid = paid;
        for(OrderLine line:requested) {
            Sale sale = sell(find(line.itemId), line.quantity, line.price, time, customer);
            sale.orderId = order.id; order.lines.add(sale);
        }
        orders.add(0, order); return order;
    }
    public void setPaid(Order order, boolean paid) {
        if(!orders.contains(order) || order.voided) throw new IllegalArgumentException("A voided order cannot change payment status.");
        order.paid = paid;
    }
    public void voidOrder(Order order) {
        if(!orders.contains(order) || order.voided) throw new IllegalArgumentException("This order has already been voided.");
        for(Sale sale:order.lines) {
            Item i = find(sale.itemId);
            if(i == null || i.quantity + sale.quantity > 1000000)
                throw new IllegalArgumentException("Cannot restore stock: an item is deleted or would exceed the stock limit.");
        }
        for(Sale sale:order.lines) { find(sale.itemId).quantity += sale.quantity; sale.voided = true; }
        order.voided = true;
    }
    public long outstanding() { long n = 0; for(Order o:orders) if(!o.paid && !o.voided) n += o.total(); return n; }
    public long profit(long since) { long n = 0; for(Sale s:sales) if(!s.voided && s.time >= since) n += s.profit(); return n; }
    public boolean costsKnown(long since) { for(Sale s:sales) if(!s.voided && s.time >= since && s.cost < 0) return false; return true; }
    public long revenue(long since) { long sum = 0; for(Sale s:sales) if(!s.voided && s.time >= since) sum += s.total(); return sum; }
    public static Ledger seeded() {
        Ledger l = new Ledger();
        String[][] names = {
            {"Just Crave French Fries (9mm)","Yumway French Fries (9mm)","Yumway French Fries (6mm)","Sweet Corn","Galacia Mozarella","Amul Mozzarella","Milkymist Mozzarella","Cheese Slice","Just Crave Aloo Tikki","Just Crave Herb n Chilli Patty","Momos","Cheese Cake"},
            {"Chefart Marinade","Chefart Cajun Breading Mix","Chefys Peri Peri","Chefys Oregano (500gm)","Veeba Oregano Sachet","Veeba Chilli Flakes Sachet","Tasty Pixel Chocolava Cake Premix","Tortillas (8.5\")"},
            {"Veeba Mayonnaise","Purple Cheese Blend","Green White Cheese Dressing","Red White Cheese Dressing","Veeba Pizza Topping Creamy Tomato","Veeba Schezwan Sauce","Veeba Makhni Sauce","Veeba Tandoori Mayonnaise","Foodrite Tomato Ketchup Sachet","Mealtime Ketchup 5kg Can","Foodrite Pizza n Pasta Sauce","Foodrite Schezwan Chutney"}
        };
        for(int c=0;c<names.length;c++) for(String name:names[c]) l.items.add(new Item(name,CATEGORIES[c]));
        return l;
    }
}
