package com.freedom.filemanager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import rikka.shizuku.Shizuku;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends Activity {
    private LinearLayout root, list;
    private TextView status;
    private File current = Environment.getExternalStorageDirectory();
    private final ArrayList<DocumentFile> safStack = new ArrayList<>();
    private static final int PICK_TREE = 41;
    private static final int SHIZUKU_REQUEST = 42;
    private boolean safMode = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        TextView title = new TextView(this); title.setText("Freedom File Manager 0.2.0"); title.setTextSize(23); root.addView(title);
        status = new TextView(this); status.setTextSize(13); root.addView(status);
        Button permission = new Button(this); permission.setText("إعداد الوصول إلى الملفات");
        permission.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
        }); root.addView(permission);
        Button folder = new Button(this); folder.setText("اختيار مجلد وتصفحه عبر SAF");
        folder.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, PICK_TREE);
        }); root.addView(folder);
        Button shizuku = new Button(this); shizuku.setText("فحص / طلب صلاحية Shizuku");
        shizuku.setOnClickListener(v -> requestShizuku()); root.addView(shizuku);
        Button up = new Button(this); up.setText("المجلد السابق");
        up.setOnClickListener(v -> {
            if (safMode) { if (safStack.size() > 1) { safStack.remove(safStack.size()-1); showSaf(); } }
            else if (current != null && current.getParentFile()!=null) { current=current.getParentFile(); showFiles(); }
        }); root.addView(up);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); showFiles();
    }

    private void requestShizuku() {
        try {
            if (!Shizuku.pingBinder()) { message("Shizuku غير متصل. افتح تطبيق Shizuku وابدأ الخدمة أولاً."); return; }
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                message("صلاحية Shizuku ممنوحة. UID الخدمة: " + runShizuku("id")); refreshStatus();
            } else Shizuku.requestPermission(SHIZUKU_REQUEST);
        } catch (Throwable e) { message("تعذر الاتصال بـ Shizuku: " + e.getMessage()); }
    }

    private String runShizuku(String command) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) return "غير مصرح";
            java.lang.Process p = Shizuku.newProcess(new String[]{"sh","-c",command}, null, null);
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder b = new StringBuilder(); String line;
            while ((line=r.readLine())!=null) b.append(line).append("\n");
            p.waitFor(); return b.toString().trim();
        } catch (Throwable e) { return "خطأ: " + e.getMessage(); }
    }

    private void refreshStatus() {
        boolean broad = Environment.isExternalStorageManager();
        String sz;
        try { sz = Shizuku.pingBinder() ? (Shizuku.checkSelfPermission()==android.content.pm.PackageManager.PERMISSION_GRANTED ? "متصل ومصرّح" : "متصل دون تصريح") : "غير متصل"; }
        catch (Throwable e) { sz="غير متاح"; }
        status.setText("الإصدار: 0.2.0\nوصول جميع الملفات: " + (broad?"مفعّل":"غير مفعّل") +
                "\nShizuku: " + sz + "\nالوضع: " + (safMode?"SAF":"التخزين المشترك"));
    }

    private void showFiles() {
        safMode=false; list.removeAllViews(); refreshStatus();
        if (!Environment.isExternalStorageManager()) {
            addLine("امنح إذن إدارة جميع الملفات لتصفح التخزين المشترك. أو اختر مجلداً عبر SAF.");
            return;
        }
        if (current==null || !current.exists() || !current.isDirectory()) current=Environment.getExternalStorageDirectory();
        File[] files=current.listFiles();
        if(files==null){ addLine("لا يمكن قراءة هذا المجلد."); return; }
        Arrays.sort(files, Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName,String.CASE_INSENSITIVE_ORDER));
        for(File f:files) {
            TextView row=row((f.isDirectory()?"📁 ":"📄 ")+f.getName());
            row.setOnClickListener(v->{ if(f.isDirectory()){current=f;showFiles();} else FileInspector.inspect(this, Uri.fromFile(f), f.getName()); });
            list.addView(row); separator();
        }
    }

    private void showSaf() {
        safMode=true; list.removeAllViews(); refreshStatus();
        DocumentFile dir=safStack.get(safStack.size()-1);
        addLine("SAF: "+dir.getName());
        DocumentFile[] files=dir.listFiles();
        Arrays.sort(files, Comparator.comparing(DocumentFile::isDirectory).reversed().thenComparing(DocumentFile::getName,String.CASE_INSENSITIVE_ORDER));
        for(DocumentFile f:files) {
            TextView row=row((f.isDirectory()?"📁 ":"📄 ")+f.getName());
            row.setOnClickListener(v->{ if(f.isDirectory()){safStack.add(f);showSaf();} else FileInspector.inspect(this,f.getUri(),f.getName()); });
            list.addView(row); separator();
        }
    }

    private TextView row(String text) {
        TextView t=new TextView(this); t.setText(text); t.setTextSize(16); t.setPadding(8,14,8,14);
        t.setBackgroundResource(android.R.drawable.list_selector_background); return t;
    }
    private void addLine(String s){ TextView t=row(s); list.addView(t); }
    private void separator(){ View v=new View(this); v.setBackgroundColor(0x22000000); list.addView(v,new LinearLayout.LayoutParams(-1,1)); }
    private void message(String s){ new android.app.AlertDialog.Builder(this).setMessage(s).setPositiveButton("حسناً",null).show(); }

    @Override protected void onResume(){ super.onResume(); if(list!=null){ if(safMode) showSaf(); else showFiles(); } }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_TREE && resultCode==RESULT_OK && data!=null) {
            Uri uri=data.getData();
            int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri,flags);
                DocumentFile tree=DocumentFile.fromTreeUri(this,uri);
                if(tree!=null){ safStack.clear(); safStack.add(tree); showSaf(); }
            } catch(Exception e){ message("تعذر حفظ إذن المجلد: "+e.getMessage()); }
        }
    }
}
