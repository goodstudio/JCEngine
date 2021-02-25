package pers.jc.sql;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.reflections.Reflections;
import pers.jc.engine.JCEngine;
import pers.jc.network.HttpComponent;
import pers.jc.network.HttpGet;
import pers.jc.util.JCLogger;
import java.util.*;

@HttpComponent("/dataView")
public class DataView {
    private static HashMap<String, Class<?>> tableMap = new HashMap<>();
    private static CURD curd;
    private static String username;
    private static String password;
    private static boolean loginVerify;
    public static boolean enabled;

    public static void scanPackage(String targetPackage) {
        Reflections reflections = new Reflections(targetPackage);
        Set<Class<?>> tables = reflections.getTypesAnnotatedWith(Table.class);
        for (Class<?> table : tables) {
            tableMap.put(table.getName(), table);
        }
    }

    public static void setCURD(CURD curd) {
        DataView.curd = curd;
    }

    public static void setLoginVerify(String username, String password) {
        DataView.username = username;
        DataView.password = password;
        loginVerify = true;
    }

    public static void enable() {
        boolean error = false;
        if (DataView.curd instanceof CURD == false) {
            error = true;
            JCLogger.error("Please Execute Code \"DataView.setCURD(CURD curd)\"");
        }
        if (tableMap.size() == 0) {
            error = true;
            JCLogger.error("Please Execute Code \"DataView.scanPackage(String targetPackage)\"");
        }
        if (error) {
            JCLogger.error("DataView enable failed");
            return;
        }
        JCEngine.addComponent(DataView.class);
        enabled = true;
    }

    @HttpGet("/getTableList")
    public HashMap<String, String> getTableList() throws Exception {
        HashMap<String, String> tableList = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : tableMap.entrySet()) {
            Class<?> tableClass = entry.getValue();
            TableInfo tableInfo = Handle.getTableInfo(tableClass);
            tableList.put(entry.getKey(), tableInfo.tableName);
        }
        return tableList;
    }

    @HttpGet("/getTableCols")
    public ArrayList<JSONObject> getTableCols(String tableKey) throws Exception {
        Class<?> tableClass = tableMap.get(tableKey);
        TableInfo tableInfo = Handle.getTableInfo(tableClass);
        ArrayList<JSONObject> tableCols = new ArrayList<>();
        for (FieldInfo fieldInfo : tableInfo.fieldInfos) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("field", fieldInfo.columnLabel);
            jsonObject.put("title", fieldInfo.columnLabel);
            jsonObject.put("isKey", fieldInfo.isIdColumn);
            jsonObject.put("autoIncrement", fieldInfo.autoIncrement);
            jsonObject.put("align", "center");
            jsonObject.put("edit", "text");
            tableCols.add(jsonObject);
        }
        return tableCols;
    }

    @HttpGet("/showTable")
    public JSONObject showTable(String tableKey, int page, int limit) {
        Class<?> tableClass = tableMap.get(tableKey);
        int count = curd.getRowCount(tableClass);
        ArrayList<?> data = curd.select(tableClass, new SQL(){{
            LIMIT((page - 1) * limit, limit);
        }});
        return responseTableInfo(0, data, count, "success");
    }

    @HttpGet("/deleteRows")
    public int deleteRows(String tableKey, String rowKVsList) throws Exception {
        Class<?> tableClass = tableMap.get(tableKey);
        JSONArray arr = (JSONArray) JSONArray.parse(rowKVsList);
        int delCount = 0;
        for (Object kvsObj : arr) {
            JSONArray kvs = (JSONArray) kvsObj;
            delCount += curd.delete(new SQL(){{
                DELETE_FROM(tableClass);
                for (int i = 0; i < kvs.size(); i += 2) {
                    String key = kvs.getString(i);
                    String value = kvs.getString(i + 1);
                    WHERE(key + " = " + PARAM(value));
                }
            }});
        }
        return delCount;
    }


    private JSONObject responseTableInfo(int code, Object data, int count, String msg) {
        JSONObject response = new JSONObject();
        response.put("code", code);
        response.put("data", data);
        response.put("count", count);
        response.put("msg", msg);
        return response;
    }
}