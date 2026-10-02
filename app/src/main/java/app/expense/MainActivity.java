package app.expense;

import android.app.*; import android.content.*; import android.content.res.ColorStateList;
import android.graphics.Typeface; import android.graphics.drawable.GradientDrawable;
import android.net.Uri; import android.os.Bundle; import android.provider.DocumentsContract;
import android.view.*; import android.widget.*;
import java.io.*; import java.text.SimpleDateFormat; import java.util.*;

public class MainActivity extends Activity {
  static final String[] NAME = {"Self", "Study"};
  static final int[] COL = {0xFF2563EB, 0xFF7C3AED};
  static final String[][] CAT = {{"Food","Travel","Bills","Shopping","Health","Other"},{"Fees","Books","Courses","Stationery","Travel","Other"}};
  static final String F = "expenses.csv", OLD = "expenses-data.json";
  SharedPreferences sp; String month; int flt = 0; // 0 = Self, 1 = Study
  TextView mtitle, st, addBtn; LinearLayout tabs, body; GestureDetector gd;
  ArrayList<String[]> E = new ArrayList<>(); double[] B = new double[2];
  SimpleDateFormat MF = new SimpleDateFormat("yyyy-MM", Locale.US);

  int dp(int x){ return (int)(x * getResources().getDisplayMetrics().density); }
  void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
  String f(double x){ return String.format(Locale.US, "\u20B9%.2f", x); }
  String pad(int n){ return n < 10 ? "0" + n : "" + n; }
  GradientDrawable shape(int c, int r){ GradientDrawable g = new GradientDrawable(); g.setColor(c); g.setCornerRadius(dp(r)); return g; }
  Button btn(String t, View.OnClickListener c){ Button b = new Button(this); b.setText(t); b.setOnClickListener(c); return b; }
  TextView tv(String t, int sz, int col){ TextView v = new TextView(this); v.setText(t); v.setTextSize(sz); v.setTextColor(col); return v; }
  TextView chip(String t, int sz, int tc, int bg, View.OnClickListener c){
    TextView v = tv(t, sz, tc); v.setGravity(Gravity.CENTER); v.setPadding(dp(14), dp(12), dp(14), dp(12));
    v.setBackground(shape(bg, 12)); v.setTypeface(null, Typeface.BOLD); v.setOnClickListener(c); return v;
  }
  LinearLayout.LayoutParams w1(){ LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(dp(4), dp(4), dp(4), dp(4)); return p; }

  protected void onCreate(Bundle b0){
    super.onCreate(b0);
    sp = getSharedPreferences("x", 0);
    load(sp.getString("db", ""));
    month = MF.format(new Date());
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true);
    root.setBackgroundColor(0xFFFFFFFF); root.setPadding(dp(8), dp(8), dp(8), dp(8));
    tabs = new LinearLayout(this);
    LinearLayout mrow = new LinearLayout(this); mrow.setGravity(Gravity.CENTER_VERTICAL);
    TextView prev = chip("\u25C0", 16, 0xFF374151, 0xFFF3F4F6, x -> shift(-1));
    mtitle = tv("", 18, 0xFF111827); mtitle.setGravity(Gravity.CENTER); mtitle.setTypeface(null, Typeface.BOLD);
    mtitle.setOnClickListener(x -> { month = MF.format(new Date()); render(); });
    TextView next = chip("\u25B6", 16, 0xFF374151, 0xFFF3F4F6, x -> shift(1));
    mrow.addView(prev); mrow.addView(mtitle, new LinearLayout.LayoutParams(0, -2, 1f)); mrow.addView(next);
    body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(4), dp(8), dp(4), dp(8));
    ScrollView sv = new ScrollView(this); sv.addView(body);
    st = tv("", 11, 0xFF9CA3AF); st.setGravity(Gravity.CENTER); st.setPadding(0, dp(4), 0, dp(4));
    LinearLayout bot = new LinearLayout(this);
    addBtn = chip("+ Add", 18, 0xFFFFFFFF, COL[0], x -> addDlg(null));
    TextView gear = chip("\u2699", 18, 0xFF374151, 0xFFF3F4F6, x -> menu());
    bot.addView(addBtn, w1()); bot.addView(gear, new LinearLayout.LayoutParams(-2, -2));
    gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
      public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy){
        if (Math.abs(vx) > 1500 && Math.abs(vx) > 2 * Math.abs(vy)) { int n = vx < 0 ? 1 : 0; if (n != flt) { flt = n; render(); } }
        return false;
      }
    });
    root.addView(tabs); root.addView(mrow); root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(st); root.addView(bot);
    setContentView(root); render();
  }

  public boolean dispatchTouchEvent(MotionEvent ev){ gd.onTouchEvent(ev); return super.dispatchTouchEvent(ev); }

  void shift(int d){
    try { Calendar c = Calendar.getInstance(); c.setTime(MF.parse(month)); c.add(Calendar.MONTH, d); month = MF.format(c.getTime()); } catch (Exception e) {}
    render();
  }

  void render(){
    tabs.removeAllViews();
    for (int k = 0; k < 2; k++) {
      final int kk = k;
      tabs.addView(chip(NAME[k], 16, flt == k ? 0xFFFFFFFF : 0xFF374151, flt == k ? COL[k] : 0xFFE5E7EB, x -> { flt = kk; render(); }), w1());
    }
    addBtn.setBackground(shape(COL[flt], 12));
    try { mtitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(MF.parse(month))); } catch (Exception e) { mtitle.setText(month); }
    double out = 0, inc = 0; TreeMap<String, Double> cat = new TreeMap<>(); ArrayList<String[]> rows = new ArrayList<>();
    for (String[] e : E) if (e[0].equals("" + flt) && e[5].startsWith(month)) rows.add(e);
    Collections.sort(rows, (p, q) -> q[5].compareTo(p[5]));
    for (String[] e : rows) {
      double a = Double.parseDouble(e[2]);
      if (e[1].equals("o")) { out += a; Double o = cat.get(e[3]); cat.put(e[3], (o == null ? 0 : o) + a); } else inc += a;
    }
    body.removeAllViews();
    LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(16), dp(14), dp(16), dp(14)); card.setBackground(shape(0xFFF3F4F6, 16));
    card.addView(tv("Balance", 12, 0xFF6B7280));
    TextView bal = tv(f(inc - out), 30, inc - out < 0 ? 0xFFC62828 : 0xFF111827); bal.setTypeface(null, Typeface.BOLD); card.addView(bal);
    LinearLayout io = new LinearLayout(this);
    io.addView(tv("Spent " + f(out), 14, 0xFFC62828), new LinearLayout.LayoutParams(0, -2, 1f));
    io.addView(tv("Income " + f(inc), 14, 0xFF2E7D32), new LinearLayout.LayoutParams(0, -2, 1f));
    card.addView(io);
    LinearLayout bb = new LinearLayout(this); bb.setOrientation(LinearLayout.VERTICAL); bb.setPadding(0, dp(10), 0, 0);
    bb.setOnClickListener(x -> budgetDlg());
    if (B[flt] > 0) {
      ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      pb.setMax(100); pb.setProgress((int) Math.min(100, out / B[flt] * 100));
      pb.setProgressTintList(ColorStateList.valueOf(out > B[flt] ? 0xFFC62828 : COL[flt]));
      bb.addView(pb, new LinearLayout.LayoutParams(-1, dp(10)));
      bb.addView(tv(out <= B[flt] ? f(B[flt] - out) + " left of " + f(B[flt]) + " budget" : f(out - B[flt]) + " over the " + f(B[flt]) + " budget", 12, out > B[flt] ? 0xFFC62828 : 0xFF6B7280));
    } else bb.addView(tv("Tap here to set a monthly budget", 12, 0xFF6B7280));
    card.addView(bb); body.addView(card);
    if (!cat.isEmpty()) {
      TextView ch = tv("Where it went", 13, 0xFF6B7280); ch.setPadding(0, dp(16), 0, dp(4)); body.addView(ch);
      for (String k : cat.keySet()) {
        double v = cat.get(k); int pct = out > 0 ? (int) Math.round(v / out * 100) : 0;
        LinearLayout r = new LinearLayout(this);
        r.addView(tv(k, 14, 0xFF111827), new LinearLayout.LayoutParams(0, -2, 1f)); r.addView(tv(f(v), 14, 0xFF6B7280));
        body.addView(r);
        LinearLayout bar = new LinearLayout(this); bar.setPadding(0, dp(2), 0, dp(6));
        View fill = new View(this); fill.setBackground(shape(COL[flt], 2));
        bar.addView(fill, new LinearLayout.LayoutParams(0, dp(4), Math.max(1, pct)));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, dp(4), Math.max(1, 100 - pct)));
        body.addView(bar);
      }
    }
    TextView eh = tv("Entries (tap one to edit or delete)", 13, 0xFF6B7280); eh.setPadding(0, dp(16), 0, dp(4)); body.addView(eh);
    if (rows.isEmpty()) body.addView(tv("Nothing yet. Tap + Add below.", 14, 0xFF9CA3AF));
    for (final String[] e : rows) {
      boolean o = e[1].equals("o");
      LinearLayout row = new LinearLayout(this); row.setPadding(0, dp(8), 0, dp(8)); row.setGravity(Gravity.CENTER_VERTICAL);
      LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
      left.addView(tv(e[4].isEmpty() ? e[3] : e[4], 15, 0xFF111827));
      left.addView(tv(e[5].substring(8) + "/" + e[5].substring(5, 7) + " \u00B7 " + e[3], 11, 0xFF9CA3AF));
      row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
      row.addView(tv((o ? "-" : "+") + f(Double.parseDouble(e[2])), 15, o ? 0xFFC62828 : 0xFF2E7D32));
      row.setOnClickListener(x -> entryMenu(e));
      body.addView(row);
    }
    if (sp.getString("dir", null) == null) st.setText("Backup is off \u2013 tap \u2699 to choose a folder");
    else if (st.getText().length() == 0 || st.getText().toString().startsWith("Backup is off")) st.setText("Backup folder is set");
  }

  void menu(){
    new AlertDialog.Builder(this).setItems(new String[]{"Choose backup folder", "Restore from backup"}, (d, w) -> { if (w == 0) pick(); else restore(false); }).show();
  }

  void entryMenu(final String[] e){
    new AlertDialog.Builder(this).setItems(new String[]{"Edit", "Delete"}, (d, w) -> {
      if (w == 0) addDlg(e); else { E.remove(e); save(); render(); toast("Deleted"); }
    }).show();
  }

  void budgetDlg(){
    final EditText a = new EditText(this); a.setInputType(8194); a.setHint("Monthly budget");
    if (B[flt] > 0) a.setText("" + B[flt]);
    new AlertDialog.Builder(this).setTitle(NAME[flt] + " budget").setView(a)
      .setPositiveButton("Save", (d, w) -> { try { B[flt] = Double.parseDouble(a.getText().toString()); } catch (Exception e) { B[flt] = 0; } save(); render(); })
      .setNegativeButton("Cancel", null).show();
  }

  void addDlg(final String[] old){
    LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20), dp(10), dp(20), 0);
    final EditText a = new EditText(this); a.setInputType(8194); a.setHint("Amount");
    final EditText n = new EditText(this); n.setHint("Note (optional)");
    final Spinner c = new Spinner(this); c.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CAT[flt]));
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    final String[] dt = {old != null ? old[5] : (today.startsWith(month) ? today : month + "-01")};
    if (old != null) { a.setText(old[2]); n.setText(old[4]); for (int i = 0; i < CAT[flt].length; i++) if (CAT[flt][i].equals(old[3])) c.setSelection(i); }
    final Button db = btn(dt[0], null);
    db.setOnClickListener(x -> {
      String[] q = dt[0].split("-");
      new DatePickerDialog(this, (vw, yy, mm, dd) -> { dt[0] = yy + "-" + pad(mm + 1) + "-" + pad(dd); db.setText(dt[0]); },
        Integer.parseInt(q[0]), Integer.parseInt(q[1]) - 1, Integer.parseInt(q[2])).show();
    });
    l.addView(a); l.addView(n); l.addView(c); l.addView(db);
    new AlertDialog.Builder(this).setTitle(old == null ? "Add to " + NAME[flt] : "Edit entry").setView(l)
      .setPositiveButton("Spent", (d, w) -> put(old, "o", a.getText().toString(), n.getText().toString(), (String) c.getSelectedItem(), dt[0]))
      .setNegativeButton("Income", (d, w) -> put(old, "i", a.getText().toString(), n.getText().toString(), (String) c.getSelectedItem(), dt[0]))
      .setNeutralButton("Cancel", null).show();
  }

  void put(String[] old, String t, String amt, String note, String cat, String date){
    try {
      double a = Double.parseDouble(amt.trim()); if (a <= 0) throw new Exception();
      if (old != null) E.remove(old);
      E.add(new String[]{"" + flt, t, "" + a, cat, note.replace(',', ';').replace('|', ' ').replace('\n', ' ').replace('"', ' ').trim(), date});
      month = date.substring(0, 7); save(); render();
    } catch (Exception e) { toast("Enter an amount"); }
  }

  // Readable file: opens in Excel / Google Sheets / Notepad
  String text(){
    StringBuilder sb = new StringBuilder("Section,Type,Date,Category,Note,Amount\n");
    for (int i = 0; i < 2; i++) if (B[i] > 0) sb.append(NAME[i]).append(",Budget,,,,").append(String.format(Locale.US, "%.2f", B[i])).append("\n");
    for (String[] e : E)
      sb.append(NAME[Integer.parseInt(e[0])]).append(",").append(e[1].equals("o") ? "Spent" : "Income").append(",").append(e[5]).append(",")
        .append(e[3]).append(",").append(e[4]).append(",").append(String.format(Locale.US, "%.2f", Double.parseDouble(e[2]))).append("\n");
    return sb.toString();
  }

  void load(String t){
    E.clear(); B = new double[2];
    for (String l : t.split("\n")) {
      l = l.trim(); if (l.isEmpty()) continue;
      try {
        if (l.contains("|")) { // older backup format
          String[] p = l.split("\\|", -1);
          if (p[0].equals("B")) B[Integer.parseInt(p[1])] = Double.parseDouble(p[2]); else if (p.length == 6) E.add(p);
        } else {
          String[] p = l.split(",", -1);
          if (p.length != 6 || p[0].equals("Section")) continue;
          int s = p[0].equals("Study") ? 1 : 0;
          if (p[1].equals("Budget")) B[s] = Double.parseDouble(p[5]);
          else E.add(new String[]{"" + s, p[1].equals("Income") ? "i" : "o", p[5], p[3], p[4], p[2]});
        }
      } catch (Exception e) {}
    }
  }

  void save(){ sp.edit().putString("db", text()).apply(); backup(); }

  Uri docUri(Uri t, String name){ return DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t) + "/" + name); }

  String read(Uri t, String name) throws Exception {
    InputStream is = getContentResolver().openInputStream(docUri(t, name));
    String s = new Scanner(is, "UTF-8").useDelimiter("\\A").next(); is.close(); return s;
  }

  void backup(){
    String d = sp.getString("dir", null); if (d == null) return;
    if (E.isEmpty() && B[0] == 0 && B[1] == 0) return; // never overwrite a backup with empty data
    try {
      Uri t = Uri.parse(d); OutputStream os = null;
      try { os = getContentResolver().openOutputStream(docUri(t, F), "wt"); } catch (Exception e) {}
      if (os == null) {
        Uri dir = DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t));
        Uri nf = DocumentsContract.createDocument(getContentResolver(), dir, "text/csv", F);
        os = getContentResolver().openOutputStream(nf, "wt");
      }
      os.write(text().getBytes("UTF-8")); os.close();
      st.setText("Backed up " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date()) + " \u2713");
    } catch (Exception e) { st.setText("Backup failed \u2013 choose the folder again (\u2699)"); }
  }

  void pick(){ startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1); }

  protected void onActivityResult(int r, int c, Intent d){
    if (r == 1 && c == RESULT_OK && d != null) {
      Uri u = d.getData();
      getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      sp.edit().putString("dir", u.toString()).apply(); st.setText("Backup folder is set");
      if (E.isEmpty() && B[0] == 0 && B[1] == 0) restore(true); else backup();
    }
  }

  void restore(final boolean quiet){
    final String d = sp.getString("dir", null);
    if (d == null) { toast("Choose a backup folder first"); return; }
    String t = null;
    try { t = read(Uri.parse(d), F); } catch (Exception e) { try { t = read(Uri.parse(d), OLD); } catch (Exception e2) {} }
    if (t == null) { if (!quiet) toast("No backup file found in that folder"); return; }
    final String data = t;
    new AlertDialog.Builder(this).setMessage("Backup found. Replace the data on this phone with it?")
      .setPositiveButton("Restore", (x, w) -> { load(data); save(); render(); toast("Restored"); })
      .setNegativeButton("Cancel", null).show();
  }
}
