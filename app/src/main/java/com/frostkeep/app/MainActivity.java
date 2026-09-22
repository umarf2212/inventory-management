package com.frostkeep.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.time.*;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(246,247,242), INK=Color.rgb(27,49,40), GREEN=Color.rgb(39,94,72), MUTED=Color.rgb(117,129,121), LIME=Color.rgb(220,241,179), LINE=Color.rgb(227,232,223);
    Ledger ledger; LinearLayout root, content, list; String screen="Inventory", category="All", query=""; boolean lowOnly=false; int days=7;
    String pendingBackup;
    boolean pendingOrdersOnly = false;
    final NumberFormat money=NumberFormat.getCurrencyInstance(new Locale("en","IN"));
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); money.setCurrency(Currency.getInstance("INR"));
        if(b!=null) { screen=b.getString("screen","Inventory"); category=b.getString("category","All"); query=b.getString("query",""); lowOnly=b.getBoolean("lowOnly"); days=b.getInt("days",7); }
        String saved=getPreferences(0).getString("ledger",null);
        try { ledger=saved==null?Ledger.seeded():decode(saved); }
        catch(Exception e) { new AlertDialog.Builder(this).setTitle("Couldn’t open saved inventory").setMessage("Your saved data has been preserved. Close and reopen the app to try again.").setPositiveButton("Close",(d,w)->finish()).setCancelable(false).show(); return; }
        if(saved==null && !getPreferences(0).edit().putString("ledger",encode()).commit()) { toast("Couldn’t save initial inventory."); finish(); return; }
        if(saved != null) try {
            if(new JSONObject(saved).getInt("version") == 1 && !getPreferences(0).contains("ledger-v1-before-costs")) {
                if(!getPreferences(0).edit().putString("ledger-v1-before-costs", saved).commit()) {
                    toast("Couldn’t preserve your original data. Free device storage and reopen."); finish(); return;
                }
            }
        } catch(JSONException ignored) {}
        render();
    }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putString("screen",screen); b.putString("category",category); b.putString("query",query); b.putBoolean("lowOnly",lowOnly); b.putInt("days",days); }
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    String cash(long n){return money.format(n/100.0);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    void stretch(LinearLayout r,View v){r.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    TextView button(String label,boolean primary,Runnable action){TextView t=text(label,14,primary?Color.WHITE:GREEN,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(13),dp(16),dp(13));t.setMinHeight(dp(48));t.setBackground(shape(primary?GREEN:LINE,16));t.setOnClickListener(v->action.run());return t;}
    LinearLayout card(){LinearLayout l=col();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(Color.WHITE,22));return l;}
    void addCard(LinearLayout parent,View card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);parent.addView(card,p);}
    void label(LinearLayout l,String s){gap(l,14);l.addView(text(s,12,MUTED,true));gap(l,7);}
    void render(){
        root=col();root.setBackgroundColor(BG);setContentView(root);
        LinearLayout header=row();header.setPadding(dp(24),dp(18),dp(24),dp(10));
        TextView mark=text("✳",30,GREEN,true);header.addView(mark);TextView brand=text("  frostkeep",22,INK,true);stretch(header,brand);
        header.addView(button("Settings",false,()->{screen="Settings";render();}));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=col();content.setPadding(dp(22),dp(12),dp(22),dp(22));scroll.addView(content);
        switch(screen) { case "Sales": sales(); break; case "Customers": customers(); break; case "Orders": orders(); break; case "Insights": insights(); break; case "Settings": settings(); break; default: inventory(); }
        LinearLayout nav=row();nav.setPadding(dp(14),dp(10),dp(14),dp(10));nav.setBackgroundColor(Color.WHITE);
        String[] titles={"Inventory","Customers","Orders","Sales","Insights"};String[] icons={"▦","♙","▤","↗","▥"};
        for(int i=0;i<titles.length;i++){final String title=titles[i];TextView n=text(icons[i]+"\n"+title,11,screen.equals(title)?GREEN:MUTED,true);n.setGravity(Gravity.CENTER);n.setPadding(0,dp(10),0,dp(10));if(screen.equals(title))n.setBackground(shape(LINE,16));n.setOnClickListener(v->{screen=title;render();});nav.addView(n,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);
    }
    void title(String heading,String sub){content.addView(text(heading,26,INK,true));gap(content,6);content.addView(text(sub,14,MUTED,false));gap(content,22);}
    void inventory(){
        title("Your stock, sorted.","Your stock, all in one cool place.");
        long value=0;int low=0;for(Ledger.Item i:ledger.items){if(i.costConfirmed)value+=i.total();if(i.quantity<=i.low)low++;}
        LinearLayout hero=card();hero.setBackground(shape(INK,24));hero.addView(text("INVENTORY BUYING VALUE",11,LIME,true));gap(hero,8);hero.addView(text(cash(value),34,Color.WHITE,true));gap(hero,16);
        LinearLayout stats=row();stretch(stats,text(ledger.items.size()+" items  ·  Confirmed costs",12,Color.rgb(193,211,200),false));TextView alert=text(low+" to restock ↗",12,LIME,true);alert.setMinHeight(dp(48));alert.setGravity(Gravity.CENTER);alert.setOnClickListener(v->{lowOnly=!lowOnly;render();});stats.addView(alert);hero.addView(stats);addCard(content,hero);
        if(ledger.items.stream().anyMatch(i -> !i.costConfirmed && i.price > 0)) {
            content.addView(text("Review buying costs: old selling prices have been retained for reference. Edit and save each item to confirm its buying cost.",12,GREEN,false)); gap(content,12);
        }
        EditText search=new EditText(this);search.setSingleLine();search.setTextSize(14);search.setHint("Search your inventory");search.setPadding(dp(16),dp(10),dp(16),dp(10));search.setBackground(shape(Color.WHITE,16));search.setText(query);content.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));gap(content,14);
        HorizontalScrollView h=new HorizontalScrollView(this);h.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();for(String c:new String[]{"All","Frozen","Masale","Sauces"}){TextView t=button(c,c.equals(category),()->{category=c;render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(7);chips.addView(t,p);}h.addView(chips);content.addView(h);gap(content,18);
        LinearLayout top=row();stretch(top,text(lowOnly?"Needs restocking":"Your inventory",19,INK,true));top.addView(button("＋ Add item",true,()->edit(null)));content.addView(top);gap(content,8);
        if(lowOnly){content.addView(button("Low stock filter  ×  Clear",false,()->{lowOnly=false;render();}));gap(content,10);}
        content.addView(text("Tap an item to edit, restock or record a sale.",12,MUTED,false));gap(content,14);list=col();content.addView(list);populate();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();populate();}public void afterTextChanged(Editable e){}});
    }
    void populate(){list.removeAllViews();int count=0;for(Ledger.Item i:ledger.items){if(!category.equals("All")&&!category.equals(i.category))continue;if(!i.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;if(lowOnly&&i.quantity>i.low)continue;count++;
        LinearLayout c=card();LinearLayout first=row();TextView icon=text(i.category.equals("Frozen")?"❄":i.category.equals("Masale")?"✳":"◒",24,GREEN,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(i.category.equals("Frozen")?Color.rgb(233,243,238):Color.rgb(249,240,218),14));first.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout names=col();names.setPadding(dp(12),0,dp(8),0);names.addView(text(i.name,16,INK,true));gap(names,5);names.addView(text(i.category+"  /  "+i.unit,11,MUTED,false));stretch(first,names);first.addView(text("›",24,MUTED,false));c.addView(first);gap(c,17);
        LinearLayout metrics=row();metric(metrics,"BUYING PRICE",i.costConfirmed?cash(i.price):"Not set");metric(metrics,"TOTAL COST",i.costConfirmed?cash(i.total()):"Not set");c.addView(metrics);
        gap(c,12);LinearLayout stock=row();stretch(stock,text("QUANTITY · "+i.unit+"(s)",11,MUTED,true));
        TextView decrease=button("−",false,()->quickStock(i,-1));decrease.setContentDescription("Decrease stock: "+i.name);stock.addView(decrease);
        TextView quantity=text("  "+i.quantity+"  ",17,INK,true);quantity.setGravity(Gravity.CENTER);stock.addView(quantity);
        TextView increase=button("+",false,()->quickStock(i,1));increase.setContentDescription("Increase stock: "+i.name);stock.addView(increase);c.addView(stock);
        if(i.quantity<=i.low){gap(c,12);c.addView(text(i.quantity==0?"●  Out of stock":"●  Low stock · restock soon",11,Color.rgb(160,105,44),true));}
        c.setOnClickListener(v->detail(i));addCard(list,c);
    }if(count==0){LinearLayout empty=card();empty.addView(text("No items here yet",18,INK,true));gap(empty,8);empty.addView(text("Try another search or add an item.",14,MUTED,false));addCard(list,empty);}}
    void metric(LinearLayout r,String label,String value){LinearLayout l=col();l.addView(text(label,10,MUTED,true));gap(l,5);l.addView(text(value,13,INK,true));stretch(r,l);}
    LinearLayout form(){LinearLayout l=col();l.setPadding(dp(24),dp(8),dp(24),dp(16));return l;}
    EditText field(LinearLayout l,String title,String value,boolean decimal,boolean numeric){label(l,title);EditText e=new EditText(this);e.setText(value);e.setTextSize(16);e.setSingleLine();e.setInputType(numeric?android.text.InputType.TYPE_CLASS_NUMBER|(decimal?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0):android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);l.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;}
    Spinner select(LinearLayout l,String title,String[] options,String selected){label(l,title);Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);s.setAdapter(a);s.setSelection(Math.max(0,Arrays.asList(options).indexOf(selected)));l.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;}
    AlertDialog dialog(String title,LinearLayout f,String positive){ScrollView s=new ScrollView(this);s.addView(f);return new AlertDialog.Builder(this).setTitle(title).setView(s).setNegativeButton("Cancel",null).setPositiveButton(positive,null).create();}
    void edit(Ledger.Item old){
        LinearLayout f=form();EditText name=field(f,"Item name",old==null?"":old.name,false,false);Spinner cat=select(f,"Category",Ledger.CATEGORIES,old==null?"Frozen":old.category);Spinner unit=select(f,"Stock unit",new String[]{"pack","bag","box","bottle","can","piece","sachet"},old==null?"pack":old.unit);
        EditText price=field(f,"Buying price per unit (₹)",old==null?"0":String.format(Locale.US,"%.2f",old.price/100.0),true,true),quantity=field(f,"Quantity in stock",old==null?"0":""+old.quantity,false,true),low=field(f,"Low-stock alert at",old==null?"5":""+old.low,false,true);
        label(f,"Count whole packs or units. Total = buying price × quantity.");AlertDialog d=dialog(old==null?"Add an item":"Edit item",f,"Save item");d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            String n=name.getText().toString().trim();if(n.isEmpty()||n.length()>120)throw new IllegalArgumentException("Enter an item name of 1–120 characters.");
            for(Ledger.Item other:ledger.items)if(other!=old&&other.name.equalsIgnoreCase(n))throw new IllegalArgumentException("An item with that name already exists.");
            long p=Ledger.parsePrice(price.getText().toString());int q=Ledger.parseQuantity(quantity.getText().toString()),a=Ledger.parseQuantity(low.getText().toString());
            if(commit(()->{Ledger.Item i=old==null?new Ledger.Item(n,cat.getSelectedItem().toString()):old;i.name=n;i.category=cat.getSelectedItem().toString();i.unit=unit.getSelectedItem().toString();i.price=p;i.costConfirmed=true;i.quantity=q;i.low=a;if(old==null)ledger.items.add(i);})){d.dismiss();render();toast("Item saved");}
        }catch(IllegalArgumentException e){toast(e.getMessage());}}));d.show();
    }
    void detail(Ledger.Item i){new AlertDialog.Builder(this).setTitle(i.name).setItems(new String[]{"Record a sale","Add stock","Edit item","Delete item"},(d,w)->{if(w==0)sale(i);if(w==1)restock(i);if(w==2)edit(i);if(w==3)new AlertDialog.Builder(this).setTitle("Delete this item?").setMessage("This removes "+i.name+" and its remaining stock. Past sales stay in your history. Sales for a deleted item cannot be voided.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(a,b)->{if(commit(()->ledger.items.remove(i))){render();toast("Item deleted");}}).show();}).show();}
    void restock(Ledger.Item i){LinearLayout f=form();f.addView(text(i.name+"\nCurrently "+i.quantity+" "+i.unit+"(s)",16,INK,true));EditText q=field(f,"Units to add","",false,true);AlertDialog d=dialog("Add stock",f,"Add stock");d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{int n=Ledger.parseQuantity(q.getText().toString());if(n<=0||i.quantity+n>1000000)throw new IllegalArgumentException("Add at least one unit, up to 1,000,000 total.");if(commit(()->i.quantity+=n)){d.dismiss();render();toast("Stock updated");}}catch(IllegalArgumentException e){toast(e.getMessage());}}));d.show();}
    void chooseSale(){ArrayList<Ledger.Item> available=new ArrayList<>();for(Ledger.Item i:ledger.items)if(i.quantity>0)available.add(i);if(available.isEmpty()){new AlertDialog.Builder(this).setTitle("Add stock first").setMessage("Open an inventory item and choose Add stock. Then you’re ready to record your first sale.").setPositiveButton("Go to inventory",(d,w)->{screen="Inventory";render();}).setNegativeButton("Close",null).show();return;}String[] names=new String[available.size()];for(int n=0;n<names.length;n++)names[n]=available.get(n).name+" · "+available.get(n).quantity+" available";new AlertDialog.Builder(this).setTitle("Choose an item").setItems(names,(d,w)->sale(available.get(w))).setNegativeButton("Cancel",null).show();}
    void sale(Ledger.Item i) {
        if(i.quantity == 0) { toast("This item is out of stock. Add stock first."); return; }
        LinearLayout f=form(); f.addView(text(i.name,18,INK,true)); gap(f,8);
        f.addView(text(i.quantity+" "+i.unit+"(s) available",14,MUTED,false));
        String[] names=new String[ledger.customers.size()+1];names[0]="No customer (optional)";
        for(int n=0;n<ledger.customers.size();n++)names[n+1]=ledger.customers.get(n).name;
        Spinner customer=select(f,"Customer (optional)",names,names[0]);
        DateInput date=new DateInput(f,"Sale date");
        EditText q=field(f,"Quantity sold","1",false,true), p=field(f,"Selling price per unit (₹)","",true,true);
        p.setHint("Enter selling price");
        TextView total=text("Enter a selling price",20,GREEN,true);gap(f,12);f.addView(total);
        TextWatcher watch=watcher(()->{try{total.setText("Total  "+cash(Ledger.parsePrice(p.getText().toString())*Ledger.parseQuantity(q.getText().toString())));}catch(Exception e){total.setText("Enter a valid price and quantity");}});
        q.addTextChangedListener(watch);p.addTextChangedListener(watch);
        customer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View v,int pos,long id){
                Ledger.Customer c=pos==0?null:ledger.customers.get(pos-1);
                Long quote=c==null?null:c.quotes.get(i.id);p.setText(quote==null?"":decimal(quote));
            }
        });
        label(f,"Stock decreases when saved. Buying costs are captured for profit reporting.");
        AlertDialog d=dialog("Record a sale",f,"Record sale");
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            int n=Ledger.parseQuantity(q.getText().toString());long price=Ledger.parsePrice(p.getText().toString());
            Ledger.Customer c=customer.getSelectedItemPosition()==0?null:ledger.customers.get(customer.getSelectedItemPosition()-1);
            if(commit(()->ledger.sell(i,n,price,date.time(),c))){d.dismiss();render();toast("Sale recorded · stock updated");}
        }catch(IllegalArgumentException e){toast(e.getMessage());}}));d.show();
    }
    long start(int daysBack){return LocalDate.now().minusDays(daysBack).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();}
    void sales(){title("Every sale counts.","A simple record of what’s moving.");LinearLayout c=card();c.addView(text("TODAY’S SALES",11,MUTED,true));gap(c,8);c.addView(text(cash(ledger.revenue(start(0))),34,INK,true));gap(c,18);c.addView(button("＋ Record a sale",true,this::chooseSale));addCard(content,c);gap(content,10);content.addView(text("Sales history",20,INK,true));gap(content,14);
        if(ledger.sales.isEmpty()){empty("Your first sale starts here.","Record a sale to see your history and bring your insights to life.");return;}
        ArrayList<Ledger.Sale> history=new ArrayList<>(ledger.sales);history.sort((a,b)->Long.compare(b.time,a.time));
        for(Ledger.Sale s:history){LinearLayout entry=card();entry.addView(text(s.name,16,INK,true));gap(entry,6);entry.addView(text(s.customerName.isEmpty()?"Walk-in customer":s.customerName,12,GREEN,true));entry.addView(text(new SimpleDateFormat("dd MMM yyyy · h:mm a",Locale.getDefault()).format(new Date(s.time)),12,MUTED,false));gap(entry,12);LinearLayout r=row();stretch(r,text(s.quantity+" "+s.unit+"(s) × "+cash(s.price),13,MUTED,false));r.addView(text(cash(s.total()),18,INK,true));entry.addView(r);gap(entry,8);entry.addView(text(s.voided?"VOIDED · excluded from analytics":s.orderId.isEmpty()?"Recorded · tap to void":"Batch order · tap to view bill",11,s.voided?MUTED:GREEN,true));if(!s.orderId.isEmpty())entry.setOnClickListener(v->{for(Ledger.Order order:ledger.orders)if(order.id.equals(s.orderId))showBill(order);});else if(!s.voided)entry.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Void this sale?").setMessage("Restore "+s.quantity+" unit(s) to stock and exclude "+cash(s.total())+" from analytics. The record stays in your history.").setNegativeButton("Cancel",null).setPositiveButton("Void sale",(d,w)->{try{if(commit(()->ledger.voidSale(s)))render();}catch(IllegalArgumentException e){toast(e.getMessage());}}).show());addCard(content,entry);}
    }
    void empty(String title,String subtitle){LinearLayout c=card();c.addView(text(title,20,INK,true));gap(c,10);c.addView(text(subtitle,14,MUTED,false));addCard(content,c);}
    void insights(){title("See the bigger picture.","Small insights. Smarter restocking.");LinearLayout tabs=row();stretch(tabs,button("7 days",days==7,()->{days=7;render();}));View space=new View(this);tabs.addView(space,new LinearLayout.LayoutParams(dp(8),1));stretch(tabs,button("30 days",days==30,()->{days=30;render();}));content.addView(tabs);gap(content,20);
        long since=start(days-1),revenue=ledger.revenue(since);long units=0;int transactions=0;Set<String> transactionIds=new HashSet<>();Map<String,Long> categories=new LinkedHashMap<>(),products=new HashMap<>();for(String c:Ledger.CATEGORIES)categories.put(c,0L);
        for(Ledger.Sale s:ledger.sales)if(!s.voided&&s.time>=since){units+=s.quantity;transactionIds.add(s.orderId.isEmpty()?s.id:s.orderId);transactions=transactionIds.size();categories.put(s.category,categories.getOrDefault(s.category,0L)+s.total());products.put(s.name,products.getOrDefault(s.name,0L)+s.total());}
        LinearLayout summary=card();summary.setBackground(shape(INK,24));summary.addView(text("REVENUE · LAST "+days+" DAYS",11,LIME,true));gap(summary,8);summary.addView(text(cash(revenue),34,Color.WHITE,true));gap(summary,12);summary.addView(text(units+" units sold    ·    "+transactions+" sales",14,Color.rgb(203,219,205),false));addCard(content,summary);
        LinearLayout profit=card();profit.addView(text(ledger.costsKnown(since)?"GROSS PROFIT":"GROSS PROFIT · KNOWN COSTS ONLY",11,MUTED,true));gap(profit,8);
        profit.addView(text(cash(ledger.profit(since)),25,GREEN,true));gap(profit,6);
        profit.addView(text("Selling revenue minus buying costs saved with each sale. Pending orders are included; expenses are excluded.",12,MUTED,false));
        if(!ledger.costsKnown(since)){gap(profit,6);profit.addView(text("Some sales have no confirmed buying cost and are excluded from profit.",12,MUTED,false));}addCard(content,profit);
        LinearLayout chart=card();chart.addView(text("Sales over time",19,INK,true));gap(chart,5);chart.addView(text("Daily revenue (₹)",12,MUTED,false));gap(chart,12);double[] values=new double[days];for(Ledger.Sale s:ledger.sales)if(!s.voided&&s.time>=since){long offset=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now().minusDays(days-1),Instant.ofEpochMilli(s.time).atZone(ZoneId.systemDefault()).toLocalDate());if(offset>=0&&offset<days)values[(int)offset]+=s.total()/100.0;}Chart graph=new Chart(values);graph.setContentDescription("Daily sales revenue chart. Total "+cash(revenue)+" over "+days+" days.");chart.addView(graph,new LinearLayout.LayoutParams(-1,dp(180)));if(transactions==0)chart.addView(text("Record a sale to start your graph.",12,MUTED,false));addCard(content,chart);
        LinearLayout cats=card();cats.addView(text("By category",19,INK,true));gap(cats,16);for(Map.Entry<String,Long> e:categories.entrySet()){LinearLayout r=row();stretch(r,text(e.getKey(),14,INK,false));r.addView(text(cash(e.getValue()),14,INK,true));cats.addView(r);gap(cats,8);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(revenue==0?0:(int)(e.getValue()*1000/revenue));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LINE));cats.addView(bar,new LinearLayout.LayoutParams(-1,dp(6)));gap(cats,16);}addCard(content,cats);
        LinearLayout best=card();best.addView(text("Top items",19,INK,true));gap(best,6);best.addView(text("Ranked by sales revenue",12,MUTED,false));ArrayList<Map.Entry<String,Long>> sorted=new ArrayList<>(products.entrySet());sorted.sort((a,b)->Long.compare(b.getValue(),a.getValue()));if(sorted.isEmpty()){gap(best,14);best.addView(text("Your bestsellers will appear here.",14,MUTED,false));}for(int n=0;n<Math.min(5,sorted.size());n++){gap(best,18);LinearLayout r=row();stretch(r,text((n+1)+".  "+sorted.get(n).getKey(),14,INK,false));r.addView(text(cash(sorted.get(n).getValue()),14,GREEN,true));best.addView(r);}addCard(content,best);content.addView(text("Revenue uses recorded selling prices. Voided sales are excluded. All dates use your device’s time zone.",12,MUTED,false));
    }
    class Chart extends View {double[] data;Paint paint=new Paint(3);Chart(double[] d){super(MainActivity.this);data=d;}void line(Canvas c,float x,float y,float xx,float yy,int color){paint.setColor(color);paint.setStrokeWidth(dp(1));c.drawLine(x,y,xx,yy,paint);}void words(Canvas c,String s,float x,float y,int color){paint.setColor(color);paint.setTextSize(dp(10));c.drawText(s,x,y,paint);}
        protected void onDraw(Canvas c){super.onDraw(c);float left=dp(47),right=getWidth()-dp(4),top=dp(14),bottom=getHeight()-dp(28);double max=0;for(double d:data)max=Math.max(max,d);max=max==0?100:Math.ceil(max/10)*10;for(int n=0;n<=2;n++){float y=top+(bottom-top)*n/2;line(c,left,y,right,y,LINE);double v=max*(2-n)/2;words(c,v>=1000000?String.format(Locale.US,"%.1fM",v/1000000):v>=1000?String.format(Locale.US,"%.1fk",v/1000):String.format(Locale.US,"%.0f",v),0,y+dp(4),MUTED);}float step=(right-left)/data.length;for(int i=0;i<data.length;i++){paint.setColor(i==data.length-1?GREEN:Color.rgb(174,203,141));float h=(float)(data[i]/max)*(bottom-top);if(h>0)c.drawRoundRect(left+i*step+dp(2),bottom-h,left+(i+1)*step-dp(2),bottom,dp(4),dp(4),paint);}words(c,LocalDate.now().minusDays(data.length-1).format(java.time.format.DateTimeFormatter.ofPattern("d MMM")),left,bottom+dp(21),MUTED);words(c,"Today",right-dp(30),bottom+dp(21),MUTED);}
    }
    String decimal(long price) { return java.math.BigDecimal.valueOf(price,2).toPlainString(); }
    TextWatcher watcher(Runnable action) {
        return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int n){}public void afterTextChanged(Editable e){action.run();}public void onTextChanged(CharSequence s,int a,int b,int c){}};
    }
    class DateInput {
        LocalDate date=LocalDate.now(); final TextView view;
        DateInput(LinearLayout parent,String labelText) {
            label(parent,labelText); view=button("",false,this::pick); parent.addView(view); update();
        }
        void update(){view.setText(date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))+"  ·  Change date");}
        void pick(){
            DatePickerDialog picker=new DatePickerDialog(MainActivity.this,(v,y,m,d)->{date=LocalDate.of(y,m+1,d);update();},date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();
        }
        long time(){return date.equals(LocalDate.now())?System.currentTimeMillis():date.atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}
    }
    void quickStock(Ledger.Item item,int delta) {
        try {
            if(commit(()->ledger.adjust(item,delta))) {
                // Rebuild totals without losing the user's position in the inventory list.
                ScrollView scroll=(ScrollView)content.getParent();int position=scroll.getScrollY();
                render();ScrollView updated=(ScrollView)content.getParent();updated.post(()->updated.scrollTo(0,position));
            }
        } catch(IllegalArgumentException e){toast(e.getMessage());}
    }
    void customers() {
        title("Know your customers.","Their details. Their prices. All in one place.");
        content.addView(button("＋ Add customer",true,()->editCustomer(null)));gap(content,18);
        if(ledger.customers.isEmpty()){empty("Your customer list starts here.","Add a name, an optional WhatsApp number, and product prices quoted specifically for them.");return;}
        for(Ledger.Customer customer:ledger.customers) {
            LinearLayout c=card();c.addView(text(customer.name,21,INK,true));gap(c,7);
            c.addView(text(customer.phone.isEmpty()?"No phone added":customer.phone,14,MUTED,false));gap(c,12);
            long due=0;for(Ledger.Order order:ledger.orders)if(order.customerId.equals(customer.id)&&!order.paid&&!order.voided)due+=order.total();
            c.addView(text(customer.quotes.size()+" quoted products  ·  "+cash(due)+" pending",13,GREEN,true));gap(c,14);
            c.addView(button("Details & quoted prices  ›",false,()->customerDetails(customer)));addCard(content,c);
        }
    }
    void editCustomer(Ledger.Customer old) {
        LinearLayout f=form();EditText name=field(f,"Customer name",old==null?"":old.name,false,false);
        EditText phone=field(f,"Phone / WhatsApp (optional)",old==null?"":old.phone,false,false);
        phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);phone.setHint("+91 98765 43210");
        label(f,"Include + and the country code. Quoted product prices can be added after saving.");
        AlertDialog d=dialog(old==null?"Add customer":"Edit customer",f,"Save customer");
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            String n=name.getText().toString().trim();if(n.isEmpty()||n.length()>120)throw new IllegalArgumentException("Enter a customer name of 1–120 characters.");
            String p=Ledger.phone(phone.getText().toString());
            for(Ledger.Customer other:ledger.customers)if(other!=old&&other.name.equalsIgnoreCase(n))throw new IllegalArgumentException("A customer with this name exists. Add a distinguishing name.");
            Ledger.Customer customer=old==null?new Ledger.Customer():old;
            if(commit(()->{customer.name=n;customer.phone=p;if(old==null)ledger.customers.add(customer);})){d.dismiss();render();customerDetails(customer);}
        }catch(IllegalArgumentException e){toast(e.getMessage());}}));d.show();
    }
    void customerDetails(Ledger.Customer customer) {
        LinearLayout f=form();f.addView(text(customer.phone.isEmpty()?"No phone added":customer.phone,14,MUTED,false));gap(f,12);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(customer.name).setView(scrollForm(f)).setNegativeButton("Close",null).create();
        f.addView(button("Edit name & phone",false,()->{d.dismiss();editCustomer(customer);}));gap(f,10);
        f.addView(button("＋ Quote a product",true,()->{
            if(ledger.items.isEmpty()){toast("Add inventory products first.");return;}
            String[] names=new String[ledger.items.size()];for(int n=0;n<names.length;n++)names[n]=ledger.items.get(n).name;
            new AlertDialog.Builder(this).setTitle("Choose a product").setItems(names,(a,n)->{d.dismiss();quote(customer,ledger.items.get(n));}).setNegativeButton("Cancel",null).show();
        }));label(f,"QUOTED SELLING PRICES");
        if(customer.quotes.isEmpty())f.addView(text("No quotes yet. Add a product to set this customer’s selling price.",14,MUTED,false));
        for(Map.Entry<String,Long> entry:customer.quotes.entrySet()) {
            Ledger.Item item=ledger.find(entry.getKey());LinearLayout c=card();
            c.addView(text(item==null?"Deleted product":item.name,16,INK,true));gap(c,6);c.addView(text(cash(entry.getValue())+" per "+(item==null?"unit":item.unit),16,GREEN,true));gap(c,10);
            if(item!=null)c.addView(button("Edit quote",false,()->{d.dismiss();quote(customer,item);}));
            gap(c,8);c.addView(button("Remove quote",false,()->new AlertDialog.Builder(this).setTitle("Remove quoted price?")
                .setMessage("Existing sales and bills keep their recorded prices.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(a,b)->{
                    if(commit(()->customer.quotes.remove(entry.getKey()))){d.dismiss();render();customerDetails(customer);}
                }).show()));addCard(f,c);
        }
        gap(f,20);f.addView(button("Delete customer",false,()->new AlertDialog.Builder(this).setTitle("Delete customer?")
            .setMessage("Remove "+customer.name+" and their quotes? Existing sales, orders and pending payments remain in history.")
            .setNegativeButton("Cancel",null).setPositiveButton("Delete",(a,b)->{if(commit(()->ledger.customers.remove(customer))){d.dismiss();render();}}).show()));d.show();
    }
    ScrollView scrollForm(LinearLayout f){ScrollView s=new ScrollView(this);s.addView(f);return s;}
    void quote(Ledger.Customer customer,Ledger.Item item) {
        LinearLayout f=form();f.addView(text(item.name,18,INK,true));gap(f,8);
        f.addView(text(item.costConfirmed?"Buying cost: "+cash(item.price)+" per "+item.unit:"Buying cost not confirmed",13,MUTED,false));
        Long existing=customer.quotes.get(item.id);EditText price=field(f,"Quoted selling price (₹)",existing==null?"":decimal(existing),true,true);
        AlertDialog d=dialog("Price for "+customer.name,f,"Save quote");
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            long p=Ledger.parsePrice(price.getText().toString());if(p<=0)throw new IllegalArgumentException("Enter a quoted price greater than zero.");
            if(commit(()->customer.quotes.put(item.id,p))){d.dismiss();render();customerDetails(customer);}
        }catch(IllegalArgumentException e){toast(e.getMessage());}}));d.show();
    }
    void orders() {
        title("Batch orders.","One customer. One order. One simple bill.");
        LinearLayout summary=card();summary.setBackground(shape(INK,24));summary.addView(text("PENDING PAYMENTS",11,LIME,true));gap(summary,8);
        summary.addView(text(cash(ledger.outstanding()),30,Color.WHITE,true));gap(summary,8);
        long count=ledger.orders.stream().filter(o->!o.paid&&!o.voided).count();summary.addView(text(count+" unpaid orders",13,LIME,false));addCard(content,summary);
        content.addView(button("＋ New batch order",true,this::chooseOrderCustomer));gap(content,12);
        content.addView(button(pendingOrdersOnly?"Pending only  ×  Show all":"Show pending payments only",false,()->{pendingOrdersOnly=!pendingOrdersOnly;render();}));gap(content,18);
        ArrayList<Ledger.Order> orders=new ArrayList<>(ledger.orders);orders.sort((a,b)->Long.compare(b.time,a.time));int shown=0;
        for(Ledger.Order order:orders) {
            if(pendingOrdersOnly&&(order.paid||order.voided))continue;shown++;
            LinearLayout c=card();LinearLayout r=row();stretch(r,text(order.customerName,20,INK,true));r.addView(text(cash(order.total()),18,GREEN,true));c.addView(r);gap(c,8);
            c.addView(text(order.number+" · "+dateText(order.time)+" · "+order.lines.size()+" products",12,MUTED,false));gap(c,10);
            c.addView(text(order.voided?"VOIDED":order.paid?"PAID":"PENDING PAYMENT",12,order.paid?GREEN:Color.rgb(158,103,43),true));gap(c,10);
            c.addView(button("View bill & payment  ›",false,()->showBill(order)));addCard(content,c);
        }
        if(shown==0)empty(pendingOrdersOnly?"All caught up.":"Build your first batch.",pendingOrdersOnly?"No pending payments to show.":"Choose a customer, add their products and quantities, then generate a bill.");
    }
    String dateText(long time){return new SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(new Date(time));}
    void chooseOrderCustomer() {
        if(ledger.customers.isEmpty()){new AlertDialog.Builder(this).setTitle("Add a customer first").setMessage("Each batch order belongs to a customer.").setNegativeButton("Cancel",null).setPositiveButton("Add customer",(d,w)->{screen="Customers";render();editCustomer(null);}).show();return;}
        String[] names=new String[ledger.customers.size()];for(int n=0;n<names.length;n++)names[n]=ledger.customers.get(n).name;
        new AlertDialog.Builder(this).setTitle("Order for which customer?").setItems(names,(d,n)->new BatchComposer(ledger.customers.get(n)).show()).setNegativeButton("Cancel",null).show();
    }
    class BatchComposer {
        final Ledger.Customer customer;
        final LinearLayout form=form(), lines=col();
        final ArrayList<BatchRow> rows=new ArrayList<>();
        final DateInput date;
        final Spinner payment;
        final TextView summary=text("Add a product to begin",18,GREEN,true);
        final AlertDialog dialog;
        BatchComposer(Ledger.Customer customer) {
            this.customer=customer;form.addView(text(customer.name,22,INK,true));gap(form,6);
            form.addView(text("Customer quotes fill automatically. You can override the price for this order.",13,MUTED,false));
            date=new DateInput(form,"Order date");payment=select(form,"Payment status",new String[]{"Pending payment","Paid"},"Pending payment");
            gap(form,12);form.addView(lines);form.addView(button("＋ Add product",false,this::addProduct));gap(form,18);form.addView(summary);
            label(form,"Generating the bill records sales and deducts stock, even if payment is pending.");
            dialog=dialog("New batch order",form,"Generate bill");
            dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{try{
                ArrayList<Ledger.OrderLine> requested=new ArrayList<>();for(BatchRow r:rows)requested.add(r.line());
                Ledger.Order[] created=new Ledger.Order[1];
                if(commit(()->created[0]=ledger.createOrder(customer,requested,payment.getSelectedItemPosition()==1,date.time()))) {
                    dialog.dismiss();screen="Orders";render();showBill(created[0]);
                }
            }catch(IllegalArgumentException e){toast(e.getMessage());}}));
        }
        void show(){dialog.show();}
        void addProduct(){
            ArrayList<Ledger.Item> available=new ArrayList<>();for(Ledger.Item i:ledger.items)if(i.quantity>0&&rows.stream().noneMatch(r->r.item.id.equals(i.id)))available.add(i);
            if(available.isEmpty()){toast("No more products with available stock. Restock items in Inventory first.");return;}
            String[] names=new String[available.size()];for(int n=0;n<names.length;n++)names[n]=available.get(n).name+" · "+available.get(n).quantity+" available";
            new AlertDialog.Builder(MainActivity.this).setTitle("Add product").setItems(names,(d,n)->{
                BatchRow row=new BatchRow(available.get(n));rows.add(row);lines.addView(row.view);update();
            }).setNegativeButton("Cancel",null).show();
        }
        void update(){
            long total=0,profit=0;boolean known=true;
            try{for(BatchRow r:rows){Ledger.OrderLine l=r.line();total+=l.price*l.quantity;if(r.item.costConfirmed)profit+=(l.price-r.item.price)*l.quantity;else known=false;}
                summary.setText("Grand total  "+cash(total)+"\n"+(known?"Gross profit  "+cash(profit):"Profit needs confirmed buying costs"));
            }catch(IllegalArgumentException e){summary.setText("Complete each quantity and selling price to see the total.");}
        }
        class BatchRow {
            final Ledger.Item item;final LinearLayout view=card();final EditText quantity,price;
            BatchRow(Ledger.Item item){
                this.item=item;view.addView(text(item.name,17,INK,true));gap(view,5);view.addView(text(item.quantity+" "+item.unit+"(s) available",12,MUTED,false));
                quantity=field(view,"Quantity","1",false,true);Long quote=customer.quotes.get(item.id);
                price=field(view,quote==null?"Selling price (₹) · no quote saved":"Selling price (₹) · customer quote",quote==null?"":decimal(quote),true,true);
                quantity.addTextChangedListener(watcher(BatchComposer.this::update));price.addTextChangedListener(watcher(BatchComposer.this::update));gap(view,8);
                view.addView(button("Remove product",false,()->{rows.remove(this);lines.removeView(view);update();}));
            }
            Ledger.OrderLine line(){int q=Ledger.parseQuantity(quantity.getText().toString());long p=Ledger.parsePrice(price.getText().toString());if(q<=0||q>item.quantity||p<=0)throw new IllegalArgumentException("Check "+item.name+": use a positive price and quantity within available stock.");return new Ledger.OrderLine(item.id,q,p);}
        }
    }
    void showBill(Ledger.Order order) {
        LinearLayout f=form();String bill=Billing.text(order);TextView message=text(bill,15,INK,false);message.setTextIsSelectable(true);f.addView(message);
        if(!order.voided){label(f,"FOR YOUR RECORDS");f.addView(text(order.costKnown()?"Gross profit: "+cash(order.profit())+(order.total()>0?String.format(Locale.US," · %.1f%% margin",100.0*order.profit()/order.total()):""):"Gross profit unavailable: buying costs were not confirmed for all products.",14,GREEN,true));}
        gap(f,18);AlertDialog d=new AlertDialog.Builder(this).setTitle(order.number+" · Bill").setView(scrollForm(f)).setNegativeButton("Close",null).create();
        f.addView(button("Copy bill text",true,()->copy("Bill "+order.number,bill)));gap(f,10);
        Ledger.Customer customer=ledger.customer(order.customerId);String phone=customer==null?order.phone:customer.phone;
        if(!phone.isEmpty()) {
            f.addView(button("Open WhatsApp with bill",false,()->{
                try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(Billing.whatsapp(phone,bill))));}
                catch(ActivityNotFoundException e){toast("No app can open this link. Copy the bill or WhatsApp link instead.");}
                catch(IllegalArgumentException e){toast(e.getMessage());}
            }));gap(f,8);
            f.addView(button("Copy WhatsApp link",false,()->{try{copy("WhatsApp bill link",Billing.whatsapp(phone,bill));}catch(IllegalArgumentException e){toast(e.getMessage());}}));
        } else {f.addView(text("Add a phone number in Customers to open this bill in WhatsApp.",12,MUTED,false));}
        if(!order.voided){gap(f,18);f.addView(button(order.paid?"Change to pending payment":"Mark as paid",false,()->{
            String next=order.paid?"pending payment":"paid";
            new AlertDialog.Builder(this).setTitle("Mark this order as "+next+"?").setMessage(order.number+" · "+cash(order.total()))
                .setNegativeButton("Cancel",null).setPositiveButton("Confirm",(a,b)->{if(commit(()->ledger.setPaid(order,!order.paid))){d.dismiss();render();showBill(order);}}).show();
        }));gap(f,10);f.addView(button("Void batch order",false,()->new AlertDialog.Builder(this).setTitle("Void the entire batch?")
            .setMessage("Restore all products to stock and exclude the order from revenue, profit and pending payments. If payment was received, arrange any refund separately.")
            .setNegativeButton("Cancel",null).setPositiveButton("Void order",(a,b)->{try{if(commit(()->ledger.voidOrder(order))){d.dismiss();render();}}catch(IllegalArgumentException e){toast(e.getMessage());}}).show()));}
        d.show();
    }
    void copy(String label,String value){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label,value));toast("Copied to clipboard");}
    void settings() {
        title("Settings.","A safe copy of your business, on your terms.");
        LinearLayout c=card();c.addView(text("Data backup",22,INK,true));gap(c,10);
        c.addView(text("Export all inventory, customers, quoted prices, sales, batch orders, payment statuses and bill details as one JSON file.",14,MUTED,false));gap(c,18);
        c.addView(button("Export full backup",true,this::exportBackup));gap(c,12);
        c.addView(text("Choose Downloads or another folder in the Android file picker. Keep this file for safekeeping; it includes customer contact details.",12,MUTED,false));
        String last=getPreferences(0).getString("lastBackup","");if(!last.isEmpty()){gap(c,14);c.addView(text("Last export: "+last,12,GREEN,true));}addCard(content,c);
        LinearLayout about=card();about.addView(text("Frostkeep 1.1",18,INK,true));gap(about,8);about.addView(text("Offline storage · INR (₹)\nBackups are manual. This version exports backups; importing them in the app is not yet available.",13,MUTED,false));addCard(content,about);
    }
    void exportBackup() {
        pendingBackup=encode();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"Frostkeep-backup-"+new SimpleDateFormat("yyyy-MM-dd-HHmmss",Locale.US).format(new Date())+".json");
        try{startActivityForResult(intent,701);}catch(ActivityNotFoundException e){pendingBackup=null;toast("No file picker is available on this device.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);if(request!=701)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){pendingBackup=null;return;}
        try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
            if(out==null)throw new java.io.IOException("Unable to open file");
            out.write((pendingBackup==null?encode():pendingBackup).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.flush();
        }catch(Exception e){pendingBackup=null;toast("Backup failed. Please choose another location and try again.");return;}
        pendingBackup=null;getPreferences(0).edit().putString("lastBackup",new SimpleDateFormat("dd MMM yyyy · h:mm a",Locale.getDefault()).format(new Date())).apply();
        if(screen.equals("Settings"))render();toast("Full backup saved to your chosen location");
    }
    boolean commit(Runnable mutation){
        String before=encode();
        try { mutation.run(); }
        catch(RuntimeException e){
            if(!before.equals(encode()))try{ledger=decode(before);}catch(Exception ignored){}
            throw e;
        }
        if(getPreferences(0).edit().putString("ledger",encode()).commit())return true;
        try{ledger=decode(before);}catch(Exception ignored){}
        new AlertDialog.Builder(this).setTitle("Couldn’t save changes")
            .setMessage("Your change could not be saved. Close the app and free some device storage before trying again.")
            .setPositiveButton("Close",(d,w)->finish()).setCancelable(false).show();
        return false;
    }
    String encode(){ return LedgerStore.encode(ledger); }
    Ledger decode(String raw)throws JSONException{ return LedgerStore.decode(raw); }
}
