package com.freedom.filemanager;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.Locale;

public final class FileInspector {
    private FileInspector() {}
    public static void inspect(Context context, Uri uri, String name) {
        String lower=name==null?"":name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")) inspectSqlite(context,uri,name);
        else inspectHex(context,uri,name);
    }
    private static File copyToCache(Context c, Uri uri) throws IOException {
        File out=File.createTempFile("ffm_inspect_",".bin",c.getCacheDir());
        try(InputStream in=c.getContentResolver().openInputStream(uri); OutputStream os=new FileOutputStream(out)) {
            if(in==null) throw new IOException("تعذر فتح الملف");
            byte[] buf=new byte[8192]; int n; while((n=in.read(buf))!=-1) os.write(buf,0,n);
        }
        return out;
    }
    private static void inspectSqlite(Context c,Uri uri,String name) {
        File temp=null;
        try {
            temp=copyToCache(c,uri);
            SQLiteDatabase db=SQLiteDatabase.openDatabase(temp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            StringBuilder out=new StringBuilder("قاعدة البيانات: ").append(name).append("\n\nالجداول والفهارس:\n");
            try(Cursor cur=db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE type IN ('table','index','view','trigger') ORDER BY type,name",null)) {
                while(cur.moveToNext()) {
                    out.append("\n[").append(cur.getString(0)).append("] ").append(cur.getString(1))
                       .append("\nالجدول: ").append(cur.getString(2));
                    String sql=cur.getString(3); if(sql!=null) out.append("\n").append(sql);
                    out.append("\n");
                }
            }
            db.close(); show(c,"فحص SQLite (قراءة فقط)",out.toString());
        } catch(Throwable e) { show(c,"تعذر قراءة SQLite","قد لا يكون الملف قاعدة SQLite سليمة أو قد يكون مشفراً.\n"+e); }
        finally { if(temp!=null) temp.delete(); }
    }
    private static void inspectHex(Context c,Uri uri,String name) {
        try(InputStream in=c.getContentResolver().openInputStream(uri)) {
            if(in==null) throw new IOException("تعذر فتح الملف");
            byte[] data=new byte[512]; int n=in.read(data);
            if(n<0)n=0;
            StringBuilder out=new StringBuilder("الملف: ").append(name).append("\nأول ").append(n).append(" بايت (حد أقصى 512):\n\n");
            for(int i=0;i<n;i+=16) {
                out.append(String.format(Locale.ROOT,"%08X  ",i));
                for(int j=0;j<16;j++) out.append(i+j<n?String.format(Locale.ROOT,"%02X ",data[i+j]&255):"   ");
                out.append(" |");
                for(int j=0;j<16 && i+j<n;j++){ int b=data[i+j]&255; out.append(b>=32&&b<127?(char)b:'.'); }
                out.append("|\n");
            }
            show(c,"عارض HEX للقراءة فقط",out.toString());
        } catch(Throwable e) { show(c,"تعذر فتح الملف",e.toString()); }
    }
    private static void show(Context c,String title,String body) {
        new android.app.AlertDialog.Builder(c).setTitle(title).setMessage(body)
          .setPositiveButton("إغلاق",null).show();
    }
}
