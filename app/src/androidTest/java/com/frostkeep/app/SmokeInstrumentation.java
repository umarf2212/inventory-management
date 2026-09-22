package com.frostkeep.app;

import android.app.*;
import android.os.Bundle;
import org.json.*;
import java.util.*;

/** Runs storage/upgrade tests using Android's real JSON implementation, without changing user data. */
public class SmokeInstrumentation extends Instrumentation {
    int checks; Bundle arguments;
    void check(boolean result,String label){checks++;if(!result)throw new AssertionError(label);}
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);this.arguments=arguments;start();}
    @Override public void onStart(){
        Bundle results=new Bundle();
        try{
            Ledger l=Ledger.seeded();Ledger.Item item=l.items.get(0);item.quantity=9;item.price=10000;item.costConfirmed=true;
            Ledger.Customer c=new Ledger.Customer();c.name="Customer & Unicode ₹";c.phone="+919876543210";c.quotes.put(item.id,15000L);l.customers.add(c);
            Ledger.Order order=l.createOrder(c,Arrays.asList(new Ledger.OrderLine(item.id,2,15000)),false,1000);
            l.sell(item,1,16000,2000,c);
            String json=LedgerStore.encode(l);Ledger restored=LedgerStore.decode(json);
            check(restored.items.size()==32&&restored.items.get(0).quantity==6,"Stock roundtrip");
            check(restored.customers.get(0).name.equals(c.name)&&restored.customers.get(0).quotes.get(item.id)==15000,"Customer quotes roundtrip");
            check(restored.orders.size()==1&&restored.orders.get(0).total()==30000&&restored.outstanding()==30000,"Order and payment roundtrip");
            check(restored.profit(0)==16000&&restored.sales.size()==2,"Costs and sales roundtrip");
            check(restored.orders.get(0).lines.get(0)==restored.sales.get(1),"Order lines share ledger sale identity");
            check(Billing.text(restored.orders.get(0)).equals(Billing.text(order)),"Bill reconstructed exactly from backup");
            check(restored.nextOrderNumber==2,"Invoice sequence preserved");
            restored.setPaid(restored.orders.get(0),true);Ledger paid=LedgerStore.decode(LedgerStore.encode(restored));check(paid.outstanding()==0,"Paid status saved");
            paid.voidOrder(paid.orders.get(0));Ledger voided=LedgerStore.decode(LedgerStore.encode(paid));
            check(voided.items.get(0).quantity==8&&voided.revenue(0)==16000&&voided.orders.get(0).voided,"Void history survives serialization");
            JSONObject legacy=new JSONObject(json);legacy.put("version",1);legacy.remove("orders");legacy.remove("customers");legacy.remove("nextOrderNumber");
            Ledger migrated=LedgerStore.decode(legacy.toString());
            check(!migrated.items.get(0).costConfirmed&&migrated.items.get(0).price==10000,"Legacy price preserved without inventing cost");
            check(migrated.sales.get(0).cost==-1&&migrated.revenue(0)==46000,"Legacy revenue retained and historical costs unknown");
            check(migrated.customers.isEmpty()&&migrated.orders.isEmpty(),"Legacy upgrade initializes new lists");
            java.io.File file=new java.io.File(getTargetContext().getCacheDir(),"frostkeep-backup-test.json");
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            String disk=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);
            check(LedgerStore.decode(disk).outstanding()==30000,"Export file reloads fully");file.delete();
            JSONObject unsupported=new JSONObject(json);unsupported.put("version",999);
            boolean rejected=false;try{LedgerStore.decode(unsupported.toString());}catch(JSONException expected){rejected=true;}check(rejected,"Future schema rejected");
            if("true".equals(arguments.getString("seed"))) {
                Ledger sample=Ledger.seeded();sample.items.get(0).quantity=20;sample.items.get(0).price=10000;sample.items.get(0).costConfirmed=true;
                sample.items.get(1).quantity=15;sample.items.get(1).price=5000;sample.items.get(1).costConfirmed=true;
                getTargetContext().getSharedPreferences("MainActivity",0).edit().putString("ledger",LedgerStore.encode(sample)).commit();
            }
            results.putString("stream","PASS: "+checks+" Android storage, migration and backup checks\n");finish(Activity.RESULT_OK,results);
        }catch(Throwable e){results.putString("stream","FAIL: "+e+"\n");finish(Activity.RESULT_CANCELED,results);}
    }
}
