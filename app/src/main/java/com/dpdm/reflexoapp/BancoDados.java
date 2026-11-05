package com.dpdm.reflexoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BancoDados {

    private final SQLiteDatabase database;

    public BancoDados(Context context) {
        database = context.openOrCreateDatabase(
                "bd",
                Context.MODE_PRIVATE,
                null
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS reflexo (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "value1 REAL, " +
                        "value2 REAL, " +
                        "value3 REAL, " +
                        "tempo REAL, " +
                        "latitude REAL, " +
                        "longitude REAL, " +
                        "data TEXT)"
        );

        try { database.execSQL("ALTER TABLE reflexo ADD COLUMN latitude REAL"); } catch (Exception ignored) {}
        try { database.execSQL("ALTER TABLE reflexo ADD COLUMN longitude REAL"); } catch (Exception ignored) {}
        try { database.execSQL("ALTER TABLE reflexo ADD COLUMN data TEXT"); } catch (Exception ignored) {}
    }

    public void salvarEvento(float x, float y, float z, float tempo, double lat, double lng, String dataStr) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("value1", x);
        contentValues.put("value2", y);
        contentValues.put("value3", z);
        contentValues.put("tempo", tempo);
        contentValues.put("latitude", lat);
        contentValues.put("longitude", lng);
        contentValues.put("data", dataStr);

        database.insert("reflexo", null, contentValues);
    }

    public long getMelhorTempo() {
        try (Cursor cursor = database.rawQuery("SELECT MIN(tempo) FROM reflexo WHERE tempo > 0", null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return (long) cursor.getDouble(0);
            }
        }
        return -1;
    }

    public List<Map<String, String>> obterHistorico() {
        List<Map<String, String>> list = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("SELECT tempo, latitude, longitude, data FROM reflexo ORDER BY id DESC", null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    double tempo = cursor.getDouble(0);
                    double lat = cursor.getDouble(1);
                    double lng = cursor.getDouble(2);
                    String data = cursor.getString(3);

                    Map<String, String> map = new HashMap<>();
                    map.put("tempo", String.format(Locale.getDefault(), "Tempo: %.0f ms", tempo));
                    map.put("data", "Data: " + (data != null ? data : "N/A"));
                    if (lat != 0.0 || lng != 0.0) {
                        map.put("localizacao", String.format(Locale.getDefault(), "Local: Lat %.4f, Lng %.4f", lat, lng));
                    } else {
                        map.put("localizacao", "Local: Não informado");
                    }
                    list.add(map);
                }
            }
        }
        return list;
    }

    public void limparHistorico() {
        database.delete("reflexo", null, null);
    }
}
