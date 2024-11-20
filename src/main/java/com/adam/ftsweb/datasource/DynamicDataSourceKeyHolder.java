package com.adam.ftsweb.datasource;

public class DynamicDataSourceKeyHolder {
    public static final String MASTER = "master";
    public static final String SLAVE = "slave";
    private static final ThreadLocal<String> holder = new ThreadLocal<>();

    public static String getDataSourceKey() {
        return holder.get();
    }

    public static void useMaster() {
        holder.set(MASTER);
    }

    public static void useSlave() {
        holder.set(SLAVE);
    }
}
