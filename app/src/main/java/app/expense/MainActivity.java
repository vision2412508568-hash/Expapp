package app.expense;

import android.app.*; import android.content.*; import android.net.Uri; import android.os.Bundle;
import android.provider.DocumentsContract; import android.text.TextUtils; import android.view.*; import android.widget.*;
import java.io.*; import java.text.SimpleDateFormat; import java.util.*;

public class MainActivity extends Activity {
  static final String[] NAME = {"Self", "Study"};
  static final String[][] CAT = {{"Food","Travel","Bills","Shopping","Health","Other"},{"Fees","Books","Courses","Stationery","Travel","Other"}};
  SharedPreferences sp; String month; TextView title; LinearLayout body, filt;
  int flt = 2; // 0 = Self, 1 = Study, 2 = All together
  ArrayList<String[]> E = new ArrayList<>(); double[] B = new double[2];
  SimpleDateFormat MF = new SimpleDateFormat("yyyy-MM", Locale.US);

  int dp(int x){ return (int)(x * getResources().getDisplayMetrics().density); }
  void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
  String f(double x){ return String.format(Locale.US, "\u20B9%.2f", x); }
  Button btn(String t, View.OnClickListener c){ Button b = new Button(this); b.setText(t); b.setOnClickListener(c); return b; }
  TextView tv(String t, int sz, int col){ TextView v = new TextView(this); v.setText(t); v.setTextSize(sz); v.setTextColor(col); return v; }

  protected void onCreate(Bundle st){
    super.onCreate(st);
    sp = getSharedPreferences("x", 0);
    load(sp.getString("db", ""));
    month = MF.format(new Date());
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true);
    root.setBackgroundColor(0xFFFFFFFF);
    LinearLayout top = new LinearLayout(this);
    title = tv("", 18, 0xFF000000); title.setGravity(Gravity.CENTER);
    top.addView(btn("\u25C0", v -> shift(-1)));
    top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
    top.addView(btn("\u25B6", v -> shift(1)));
    filt = new LinearLayout(this);
    body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(12), dp(8), dp(12), dp(8));
    ScrollView sv = new ScrollView(this); sv.addView(body);
    LinearLayout bot = new LinearLayout(this);
    bot.addView(btn("Backup folder", v -> pick()), new LinearLayout.LayoutParams(0, -2, 1f));
    bot.addView(btn("Restore", v -> restore()), new LinearLayout.LayoutParams(0, -2, 1f));
    root.addView(top); root.addView(filt); root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(bot);
    setContentView(root); render();
  }

  void shift(int d){
    try { Calendar c = Calendar.getInstance(); c.setTime(MF.parse(month)); c.add(Calendar.MONTH, d); month = MF.format(c.getTime()); } catch (Exception e) {}
    render();
  }

  void render(){
    title.setText(month); filt.removeAllViews(); body.removeAllViews();
    String[] fn = {"Self", "Study", "Both"};
    for (int k = 0; k < 3; k++) {
      final int kk = k;
      Button b = btn((flt == k ? "\u25CF " : "") + fn[k], x -> { flt = kk; render(); });
      filt.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
    }
    double out = 0, inc = 0; double[] outS = new double[2];
    TreeMap<String, Double> cat = new TreeMap<>();
    ArrayList<String[]> rows = new ArrayList<>();
    for (String[] e : E) {
      int s = Integer.parseInt(e[0]);
      if ((flt == 2 || s == flt) && e[5].startsWith(month)) rows.add(e);
    }
    Collections.sort(rows, (p, q) -> q[5].compareTo(p[5]));
    for (String[] e : rows) {
      int s = Integer.parseInt(e[0]); double a = Double.parseDouble(e[2]);
      if (e[1].equals("o")) {
        out += a; outS[s] += a;
        String key = (flt == 2 ? NAME[s] + " \u00B7 " : "") + e[3];
        Double o = cat.get(key); cat.put(key, (o == null ? 0 : o) + a);
      } else inc += a;
    }
    String head = flt == 2 ? "Self + Study" : NAME[flt];
    TextView h = tv(head, 20, 0xFF000000); h.setGravity(Gravity.CENTER); body.addView(h);
    body.addView(tv("Spent   " + f(out) + "\nIncome   " + f(inc) + "\nBalance   " + f(inc - out), 17, 0xFF000000));
    if (flt == 2) body.addView(tv("Self spent " + f(outS[0]) + "   |   Study spent " + f(outS[1]), 13, 0xFF555555));
    LinearLayout hb = new LinearLayout(this);
    for (int s = 0; s < 2; s++) {
      if (flt != 2 && flt != s) continue;
      final int ss = s;
      String bt = B[s] > 0 ? NAME[s] + " budget " + f(B[s]) + "\n" + (B[s] >= outS[s] ? f(B[s] - outS[s]) + " left" : f(outS[s] - B[s]) + " OVER") : "Set " + NAME[s] + " budget";
      hb.addView(btn(bt, x -> budgetDlg(ss)), new LinearLayout.LayoutParams(0, -2, 1f));
    }
    body.addView(hb);
    body.addView(btn("+ Add entry", x -> addDlg()));
    if (!cat.isEmpty()) body.addView(tv("\nSpending by category", 14, 0xFF000000));
    for (String k : cat.keySet()) body.addView(tv(k + "   " + f(cat.get(k)), 14, 0xFF555555));
    body.addView(tv("\nEntries (tap to delete)", 14, 0xFF000000));
    for (final String[] e : rows) {
      boolean o = e[1].equals("o");
      String label = e[5].substring(8) + "  " + (flt == 2 ? "[" + NAME[Integer.parseInt(e[0])] + "] " : "") + (e[4].isEmpty() ? e[3] : e[4]);
      TextView r = tv(label + "   " + (o ? "-" : "+") + f(Double.parseDouble(e[2])), 15, o ? 0xFFC62828 : 0xFF2E7D32);
      r.setPadding(0, dp(8), 0, dp(8));
      r.setOnClickListener(x -> new AlertDialog.Builder(this).setMessage("Delete this entry?")
        .setPositiveButton("Delete", (d2, w) -> { E.remove(e); save(); render(); }).setNegativeButton("Cancel", null).show());
      body.addView(r);
    }
  }

  void budgetDlg(int s){
    final EditText a = new EditText(this); a.setInputType(8194); a.setHint("Monthly budget");
    new AlertDialog.Builder(this).setTitle(NAME[s] + " budget").setView(a)
      .setPositiveButton("Save", (d, w) -> { try { B[s] = Double.parseDouble(a.getText().toString()); } catch (Exception e) { B[s] = 0; } save(); render(); })
      .setNegativeButton("Cancel", null).show();
  }

  void addDlg(){
    LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20), dp(10), dp(20), 0);
    final Spinner who = new Spinner(this); who.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, NAME));
    who.setSelection(flt == 1 ? 1 : 0);
    final Spinner c = new Spinner(this);
    c.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CAT[who.getSelectedItemPosition()]));
    who.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> p, View v, int pos, long id){
        c.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, CAT[pos]));
      }
      public void onNothingSelected(AdapterView<?> p){}
    });
    final EditText a = new EditText(this); a.setInputType(8194); a.setHint("Amount");
    final EditText n = new EditText(this); n.setHint("Note");
    l.addView(who); l.addView(a); l.addView(n); l.addView(c);
    new AlertDialog.Builder(this).setTitle("Add entry").setView(l)
      .setPositiveButton("Spent", (d, w) -> put(who.getSelectedItemPosition(), "o", a.getText().toString(), n.getText().toString(), (String) c.getSelectedItem()))
      .setNegativeButton("Income", (d, w) -> put(who.getSelectedItemPosition(), "i", a.getText().toString(), n.getText().toString(), (String) c.getSelectedItem()))
      .setNeutralButton("Cancel", null).show();
  }

  void put(int s, String t, String amt, String note, String cat){
    try {
      double a = Double.parseDouble(amt); if (a <= 0) return;
      String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
      String d = today.startsWith(month) ? today : month + "-01";
      E.add(new String[]{"" + s, t, "" + a, cat, note.replace('|', ' ').replace('\n', ' '), d});
      save(); render();
    } catch (Exception e) { toast("Enter an amount"); }
  }

  String text(){
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2; i++) sb.append("B|" + i + "|" + B[i] + "\n");
    for (String[] e : E) sb.append(TextUtils.join("|", e)).append("\n");
    return sb.toString();
  }

  void load(String t){
    E.clear(); B = new double[2];
    for (String l : t.split("\n")) {
      String[] p = l.split("\\|", -1);
      try { if (p[0].equals("B")) B[Integer.parseInt(p[1])] = Double.parseDouble(p[2]); else if (p.length == 6) E.add(p); } catch (Exception e) {}
    }
  }

  void save(){ sp.edit().putString("db", text()).apply(); backup(); }

  Uri fileUri(Uri t){ return DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t) + "/expenses-data.json"); }

  void backup(){
    String d = sp.getString("dir", null); if (d == null) return;
    try {
      Uri t = Uri.parse(d); OutputStream os = null;
      try { os = getContentResolver().openOutputStream(fileUri(t), "wt"); } catch (Exception e) {}
      if (os == null) {
        Uri dir = DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t));
        Uri nf = DocumentsContract.createDocument(getContentResolver(), dir, "application/json", "expenses-data.json");
        os = getContentResolver().openOutputStream(nf, "wt");
      }
      os.write(text().getBytes("UTF-8")); os.close();
    } catch (Exception e) { toast("Backup failed - choose the folder again"); }
  }

  void pick(){ startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1); }

  protected void onActivityResult(int r, int c, Intent d){
    if (r == 1 && c == RESULT_OK && d != null) {
      Uri u = d.getData();
      getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      sp.edit().putString("dir", u.toString()).apply(); backup(); toast("Backup folder set");
    }
  }

  void restore(){
    final String d = sp.getString("dir", null);
    if (d == null) { toast("Choose a backup folder first"); return; }
    new AlertDialog.Builder(this).setMessage("Replace data on this phone with the folder backup?")
      .setPositiveButton("Restore", (x, w) -> {
        try {
          InputStream is = getContentResolver().openInputStream(fileUri(Uri.parse(d)));
          String t = new Scanner(is, "UTF-8").useDelimiter("\\A").next(); is.close();
          load(t); save(); render();
        } catch (Exception e) { toast("No backup found"); }
      }).setNegativeButton("Cancel", null).show();
  }
}
