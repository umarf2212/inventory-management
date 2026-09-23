package com.frostkeep.app;

import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class LedgerTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void rejects(Runnable f){try{f.run();throw new AssertionError("Expected validation failure");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        Ledger l=Ledger.seeded();check(l.items.size()==32,"32 starter items");
        check(l.items.stream().filter(i->i.category.equals("Frozen")).count()==12,"12 frozen items");
        check(l.items.stream().filter(i->i.category.equals("Masale")).count()==8,"8 masale items");
        check(l.items.stream().filter(i->i.category.equals("Sauces")).count()==12,"12 sauces");
        check(l.items.stream().allMatch(i->i.quantity==0&&i.price==0),"No invented data");
        check(Ledger.parsePrice("19.99")==1999,"Exact money");rejects(()->Ledger.parsePrice("1.001"));rejects(()->Ledger.parsePrice("-1"));rejects(()->Ledger.parsePrice("NaN"));rejects(()->Ledger.parseQuantity("1.5"));rejects(()->Ledger.parseQuantity("1000001"));
        Ledger.Item i=l.items.get(0);i.quantity=10;i.price=1999;check(i.total()==19990,"Stock value");
        Ledger.Sale s=l.sell(i,3,1999,1000);check(i.quantity==7,"Stock reduced");check(l.revenue(0)==5997,"Exact revenue");check(l.revenue(1001)==0,"Date filtering");
        i.price=2999;check(s.total()==5997,"Historical price preserved");
        rejects(()->l.sell(i,8,1999,2000));rejects(()->l.sell(i,0,1999,2000));rejects(()->l.sell(i,1,0,2000));check(i.quantity==7&&l.sales.size()==1,"Invalid sale leaves state unchanged");
        l.voidSale(s);check(i.quantity==10,"Void restores stock");check(l.revenue(0)==0,"Void excludes revenue");rejects(()->l.voidSale(s));check(i.quantity==10,"Double void cannot inflate stock");
        Ledger.Sale s2=l.sell(i,2,100,2000);l.items.remove(i);check(l.revenue(0)==200,"Deletion preserves sales");rejects(()->l.voidSale(s2));
        batchChecks();
        System.out.println("PASS: "+checks+" ledger checks");
    }
    static void batchChecks() {
        Ledger l=Ledger.seeded();Ledger.Item a=l.items.get(0),b=l.items.get(1);
        a.quantity=10;a.price=10000;a.costConfirmed=true;b.quantity=6;b.price=5000;b.costConfirmed=true;
        Ledger.Customer alice=new Ledger.Customer();alice.name="Alice & Co";alice.phone=Ledger.phone("+91 98765-43210");l.customers.add(alice);
        Ledger.Customer bob=new Ledger.Customer();bob.name="Bob";l.customers.add(bob);
        alice.quotes.put(a.id,15000L);bob.quotes.put(a.id,17500L);
        check(!alice.quotes.get(a.id).equals(bob.quotes.get(a.id)),"Customer prices independent");
        check(alice.phone.equals("+919876543210"),"Phone normalization");rejects(()->Ledger.phone("9876543210"));rejects(()->Ledger.phone("+0123456789"));
        check(Ledger.phone("").isEmpty(),"Phone optional");
        l.adjust(a,1);l.adjust(a,-1);check(a.quantity==10,"Quick adjustment");rejects(()->l.adjust(a,-11));check(a.quantity==10,"No negative stock");
        List<Ledger.OrderLine> request=Arrays.asList(new Ledger.OrderLine(a.id,2,15000),new Ledger.OrderLine(b.id,3,7500));
        rejects(()->l.createOrder(alice,Collections.emptyList(),false,1000));
        rejects(()->l.createOrder(new Ledger.Customer(),request,false,1000));
        rejects(()->l.createOrder(alice,Arrays.asList(request.get(0),new Ledger.OrderLine(b.id,7,7500)),false,1000));
        check(a.quantity==10&&b.quantity==6&&l.sales.isEmpty()&&l.orders.isEmpty()&&l.nextOrderNumber==1,"Oversell rejects entire batch without mutation");
        rejects(()->l.createOrder(alice,Arrays.asList(request.get(0),request.get(0)),false,1000));
        rejects(()->l.createOrder(alice,request,false,System.currentTimeMillis()+86400000));
        Ledger.Order order=l.createOrder(alice,request,false,1000);
        check(order.total()==52500,"Batch total");check(order.profit()==17500,"Profit uses cost");check(order.costKnown(),"Known costs");
        check(a.quantity==8&&b.quantity==3,"Batch stock deduction");check(l.sales.size()==2&&l.revenue(0)==52500,"No double counted revenue");
        check(l.profit(0)==17500&&l.outstanding()==52500,"Profit and pending payments");
        check(order.number.equals("FK-00001"),"Bill number");
        String bill=Billing.text(order);check(bill.contains("2 pack(s) × ₹150.00 = ₹300.00")&&bill.contains("Grand total: ₹525.00"),"Bill quantities and totals");
        check(!bill.contains("profit")&&!bill.contains("₹100.00"),"Bill hides buying costs");
        String link=Billing.whatsapp(order);check(link.startsWith("https://wa.me/919876543210?text="),"WhatsApp recipient");
        check(URLDecoder.decode(link.substring(link.indexOf("?text=")+6),StandardCharsets.UTF_8).equals(bill),"WhatsApp exact Unicode text encoding");
        rejects(()->Billing.whatsapp("",bill));
        alice.quotes.put(a.id,99900L);alice.name="New customer name";a.price=50000;a.name="New item name";
        check(order.total()==52500&&order.profit()==17500&&order.customerName.equals("Alice & Co")&&!Billing.text(order).contains("New item name"),"Historical order snapshots stay fixed");
        l.setPaid(order,true);check(l.outstanding()==0&&l.revenue(0)==52500&&a.quantity==8,"Payment does not resell or restock");
        check(Billing.text(order).contains("Amount due: ₹0.00"),"Paid bill has zero due");
        l.setPaid(order,false);check(l.outstanding()==52500,"Payment correction");
        rejects(()->l.voidSale(order.lines.get(0)));check(!order.lines.get(0).voided&&a.quantity==8,"Batch lines cannot be individually voided");
        l.items.remove(b);rejects(()->l.voidOrder(order));check(a.quantity==8&&!order.voided,"Void preflight is atomic");l.items.add(b);
        l.voidOrder(order);check(a.quantity==10&&b.quantity==6&&l.revenue(0)==0&&l.outstanding()==0&&l.profit(0)==0,"Void restores entire batch and balances");
        rejects(()->l.voidOrder(order));rejects(()->l.setPaid(order,true));
        Ledger.Sale single=l.sell(a,1,20000,2000,bob);check(single.customerId.equals(bob.id)&&single.customerName.equals("Bob"),"Individual sale optional customer snapshot");
        check(single.profit()==-30000,"Negative profit supported");
        b.costConfirmed=false;Ledger.Sale unknown=l.sell(b,1,9000,3000);check(unknown.cost==-1&&!l.costsKnown(0),"Unconfirmed costs excluded");
        a.quantity=1000000;rejects(()->l.adjust(a,1));
        Ledger.Order o2=l.createOrder(bob,Collections.singletonList(new Ledger.OrderLine(b.id,2,8000)),false,4000);
        check(b.quantity==3,"Stock deducted for o2");
        check(o2.total()==16000,"o2 total");
        rejects(()->l.updateOrder(o2,Collections.singletonList(new Ledger.OrderLine(b.id,10,8000)),false,4000));
        check(b.quantity==3,"Failed update left stock unchanged");
        l.updateOrder(o2,Collections.singletonList(new Ledger.OrderLine(b.id,4,9000)),true,4500);
        check(b.quantity==1,"Updated quantity deducted additional stock");
        check(o2.total()==36000,"Updated order total");
        check(o2.paid,"Updated order paid status");
        l.updateOrder(o2,Arrays.asList(new Ledger.OrderLine(b.id,1,9000),new Ledger.OrderLine(a.id,5,25000)),false,4600);
        check(b.quantity==4,"Reduced b restored stock");
        check(a.quantity==999995,"Added a deducted stock");
        check(o2.lines.size()==2,"Order now has 2 lines");
        l.voidOrder(o2);
        check(b.quantity==5&&a.quantity==1000000&&o2.voided,"Void restored both items");
        // Reopen voided order for editing
        l.updateOrder(o2,Collections.singletonList(new Ledger.OrderLine(b.id,2,9000)),false,4600);
        check(!o2.voided,"Reopened order is no longer voided");
        check(b.quantity==3,"Reopening deducted required stock");
        check(o2.total()==18000,"Reopened order total updated");
        rejects(()->l.deleteOrder(o2)); // active order cannot be deleted
        // Void again and test reinstate
        l.voidOrder(o2);
        check(b.quantity==5&&o2.voided,"Voided again");
        b.quantity=1; // not enough stock for 2 items
        rejects(()->l.reinstateOrder(o2));
        check(b.quantity==1&&o2.voided,"Failed reinstate left stock unchanged");
        b.quantity=5;
        l.reinstateOrder(o2);
        check(!o2.voided&&b.quantity==3,"Reinstated order deducted stock and cleared voided flag");
        // Void and delete
        l.voidOrder(o2);
        check(l.orders.contains(o2),"Order present before delete");
        int salesBefore=l.sales.size();
        l.deleteOrder(o2);
        check(!l.orders.contains(o2),"Order removed after delete");
        check(l.sales.size()==salesBefore-o2.lines.size(),"Sales lines removed after delete");
        rejects(()->l.deleteOrder(o2));
    }
}
