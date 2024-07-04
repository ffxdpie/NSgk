package com.fx.nsgk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "ns.db";
    private static final int DATABASE_VERSION = 1;


    // 数据库文件名
    @SuppressLint("SdCardPath")
    private static final String DB_PATH = "/data/data/com.fx.nsgk/databases/";

    public void copyDatabaseFromAssets(Context context) {
        // 获取数据库文件的目标路径
        String dbPath = DB_PATH + DATABASE_NAME;

        // 检查目标路径是否存在，如果不存在则创建它
        File dbFile = new File(dbPath);
//        if (!dbFile.exists()) {
        try {
            // 创建目标文件夹
            File directory = new File(DB_PATH);
            directory.mkdirs();
            // 从 assets 文件夹中复制数据库文件
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(DATABASE_NAME);
            OutputStream outputStream = Files.newOutputStream(dbFile.toPath());
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            //Toast.makeText(context, "创建数据成功!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "复制数据库出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
//    }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 在这里创建数据库表，如果需要的话
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果需要升级数据库，可以在这里实现
    }

    // 查询数据库中与给定值对应的 ID
    @SuppressLint("Range")
    public List<Integer> queryIdsForWorkingDistance(double distance, String angle, double weight) {
        List<Integer> ids = new ArrayList<>();
        // 将 angle 转换为最近的有效值
        int[] validAngles = {0, 2, 5, 10, 15, 20, 25, 30, 45, 60, 90};
        int angleValue = Integer.parseInt(angle);
        int roundedAngle = getClosestAngle(angleValue, validAngles);
        double distancemath;
        if (distance <= 24.5 & distance > 24) {
            distancemath = 24.5;
        } else {
            distancemath = Math.ceil(distance);
        }

        // 使用 try-with-resources 确保资源自动关闭
        try (SQLiteDatabase db = this.getReadableDatabase();
             @SuppressLint("DefaultLocale")
             Cursor cursor = db.rawQuery("SELECT id FROM NSgk WHERE distance = ? AND angle" + roundedAngle + " >= ?",
                     new String[]{String.valueOf(distancemath), String.valueOf(weight)})
        ) {

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    ids.add(id);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "数据查询错误: " + e.getMessage());
        }
        return ids;
    }


    // 获取最接近的有效 angle 值
    private int getClosestAngle(int angle, int[] validAngles) {
        for (int validAngle : validAngles) {
            if (angle <= validAngle) {
                return validAngle;
            }
        }
        return validAngles[validAngles.length - 1]; // 返回最大值
    }


    // 根据 id 获取 Working, distance 和 angle0 的值
    @SuppressLint("Range")
    public WorkingData getWorkingDistanceAngle0ById(int id) {
        SQLiteDatabase db = null;
        WorkingData result = null;  // 返回 null 表示未找到数据
        try {
            db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM NSgk WHERE id = ?", new String[]{String.valueOf(id)});
            if (cursor.moveToFirst()) {
                int working = cursor.getInt(cursor.getColumnIndex("Working"));
                double distance = cursor.getDouble(cursor.getColumnIndex("distance"));
                double angle0 = cursor.getDouble(cursor.getColumnIndex("angle0"));
                result = new WorkingData(working, distance, angle0);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "数据查询错误: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }


    // 查询数据库中根据给的条件查找适应的工况
    // rcdistance 配重伸出距离
    // Otype 支腿类型
    // Cwtype 配重类型
    // Rtype  回转类型
    @SuppressLint("Range")
    public List<Integer> queryWork(int rcdistance, int otype, int cwtype, int rtype) {
        SQLiteDatabase db = null;
        List<Integer> gkValues = new ArrayList<>();
        try {
            db = this.getReadableDatabase();
            StringBuilder queryBuilder = new StringBuilder("SELECT gk FROM gk WHERE ");
            List<String> whereArgs = new ArrayList<>();

            // 构建查询语句和参数列表
            if (rcdistance != -1) {
                queryBuilder.append("rcdistance = ? AND ");
                whereArgs.add(String.valueOf(rcdistance));
            }
            if (otype != -1) {
                queryBuilder.append("Otype = ? AND ");
                whereArgs.add(String.valueOf(otype));
            }
            if (cwtype != -1) {
                queryBuilder.append("Cwtype = ? AND ");
                whereArgs.add(String.valueOf(cwtype));
            }
            if (rtype != -1) {
                queryBuilder.append("Rtype = ? AND ");
                whereArgs.add(String.valueOf(rtype));
            }

            // 删除末尾的 "AND"
            if (queryBuilder.toString().endsWith("AND ")) {
                queryBuilder.setLength(queryBuilder.length() - 5);
            }

            Cursor cursor = db.rawQuery(queryBuilder.toString(), whereArgs.toArray(new String[0]));
            if (cursor.moveToFirst()) {
                do {
                    int gkValue = cursor.getInt(cursor.getColumnIndex("gk"));
                    gkValues.add(gkValue);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error querying database: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return gkValues;
    }
   //获取工况的信息
   @SuppressLint("Range")
   public Workingconditiontype WorkingType(String gk) {
       SQLiteDatabase db = null;
       Workingconditiontype result = null;  // 返回 null 表示未找到数据
       try {
           db = this.getReadableDatabase();
           Cursor cursor = db.rawQuery("SELECT * FROM gk WHERE gk = ?", new String[]{gk});
           if (cursor.moveToFirst()) {
               int rcdistance = cursor.getInt(cursor.getColumnIndex("rcdistance"));
               int otype = cursor.getInt(cursor.getColumnIndex("Otype"));
               int cwtype = cursor.getInt(cursor.getColumnIndex("Cwtype"));
               int rtype = cursor.getInt(cursor.getColumnIndex("Rtype"));

               result = new Workingconditiontype(rcdistance, otype, cwtype,rtype);
           }
           cursor.close();
       } catch (Exception e) {
           Log.e(TAG, "数据查询错误: " + e.getMessage());
       } finally {
           if (db != null) {
               db.close();
           }
       }
       return result;
   }




    // 根据角度和吊臂距离得到每个角度起重量
    @SuppressLint("Range")
    public ArrayList<Double> getAngleByWorking(int angle, int working) {
        SQLiteDatabase db = null;
        ArrayList<Double> angleValues = new ArrayList<>();
        try {
            db = this.getReadableDatabase();
            String angleColumn = "angle" + angle;
            String query = "SELECT " + angleColumn + " FROM NSgkmax WHERE Working = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(working)});

            while (cursor.moveToNext()) {
                if (!cursor.isNull(cursor.getColumnIndex(angleColumn))) {
                    double angleValue = cursor.getDouble(cursor.getColumnIndex(angleColumn));
                    angleValues.add(angleValue);
                } else {
                    // 提供一个默认值，如 0.0 或其他合适的值
                    angleValues.add(0.0);
                }
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error querying database: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return angleValues;
    }

}