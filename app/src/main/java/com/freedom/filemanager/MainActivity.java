package com.freedom.filemanager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends Activity {
    private LinearLayout root, list;
    private TextView status;
    private File current = Environment.getExternalStorageDirectory();
    private static final int PICK_TREE = 41;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,18);
        TextView title = new TextView(this); title.setText("Freedom File Manager"); title.setTextSize(23);
        root.addView(title);
        status = new TextView(this); status.setTextSize(13); root.addView(status);
        Button permission = new Button(this); permission.setText("إعداد الوصول إلى الملفات");
        permission.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
        });
        root.addView(permission);
        Button folder = new Button(this); folder.setText("اختيار مجلد عبر منتقي Android (SAF)");
        folder.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, PICK_TREE);
        });
        root.addView(folder);
        Button up = new Button(this); up.setText("المجلد السابق");
        up.setOnClickListener(v -> { if (current != null && current.getParentFile()!=null) { current=current.getParentFile(); showFiles(); }});
        root.addView(up);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        showFiles();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) showFiles(); }

    private void showFiles() {
        list.removeAllViews();
        boolean broad = Environment.isExternalStorageManager();
        status.setText("الوصول الشامل للملفات: " + (broad ? "مفعّل" : "غير مفعّل") +
                "\nالمسار: " + (current==null?"—":current.getAbsolutePath()) +
                "\nملاحظة: Android 14 يقيّد Android/data وAndroid/obb حتى مع هذا الإذن.");
        if (!broad) {
            TextView hint = new TextView(this);
            hint.setText("فعّل إذن إدارة جميع الملفات، أو اختر مجلداً باستخدام SAF. هذا الإذن لا يفتح بيانات التطبيقات الخاصة.");
            hint.setPadding(8,16,8,16); list.addView(hint);
            return;
        }
        if (current == null || !current.exists() || !current.isDirectory()) current=Environment.getExternalStorageDirectory();
        File[] files=current.listFiles();
        if(files==null){ addLine("لا يمكن قراءة هذا المجلد."); return; }
        Arrays.sort(files, Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName,String.CASE_INSENSITIVE_ORDER));
        for(File f:files){
            TextView row=new TextView(this);
            row.setText((f.isDirectory()?"📁 ":"📄 ")+f.getName());
            row.setTextSize(16); row.setPadding(8,14,8,14);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setOnClickListener(v->{ if(f.isDirectory()){current=f;showFiles();} else openInfo(f); });
            list.addView(row);
            View line=new View(this); line.setBackgroundColor(0x22000000);
            list.addView(line,new LinearLayout.LayoutParams(-1,1));
        }
    }
    private void openInfo(File f) {
        String msg="الملف: "+f.getName()+"\nالحجم: "+f.length()+" بايت\nالمسار: "+f.getAbsolutePath()+
                "\n\nهذه النسخة الأولية للعرض فقط؛ محرر النصوص وSQLite وHEX وعمليات التعديل ستضاف في المراحل التالية.";
        new android.app.AlertDialog.Builder(this).setTitle("تفاصيل الملف").setMessage(msg).setPositiveButton("حسناً",null).show();
    }
    private void addLine(String s){ TextView t=new TextView(this);t.setText(s);list.addView(t); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_TREE && resultCode==RESULT_OK && data!=null){
            Uri uri=data.getData();
            int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri,flags);
            new android.app.AlertDialog.Builder(this).setTitle("تم منح صلاحية المجلد")
                .setMessage("تم حفظ إذن الوصول إلى:\n"+uri+"\n\nستُضاف واجهة تصفح SAF الكاملة في المرحلة التالية.")
                .setPositiveButton("حسناً",null).show();
        }
    }
}