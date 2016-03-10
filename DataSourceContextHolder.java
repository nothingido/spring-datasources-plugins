package com.plugin.datasources;

/**
 * 
 * @author 1635
 *
 */
public class DataSourceContextHolder {
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();
    public static void setDataSourceID(String id) {
        contextHolder.set(id);
    }
    public static String getDataSourceID() {
        return contextHolder.get();
    }
    public static void clearDataSourceID() {
        contextHolder.remove();
    }
}