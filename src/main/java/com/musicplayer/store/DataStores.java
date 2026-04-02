package com.musicplayer.store;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DataStores {
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String MYSQL_DRIVER_JAR = "mysql-connector-j-9.2.0.jar";
    private static final String SQLITE_DRIVER_JAR = "sqlite-jdbc-3.46.1.3.jar";
    private static final String DEFAULT_MYSQL_URL =
        "jdbc:mysql://127.0.0.1:3306/musicplayer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    private static final String DEFAULT_MYSQL_USER = "root";
    private static final String DEFAULT_MYSQL_PASSWORD = "qx418504";

    private static URLClassLoader mysqlDriverLoader;
    private static URLClassLoader sqliteDriverLoader;
    private static Driver mysqlDriverInstance;
    private static Driver sqliteDriverInstance;

    private DataStores() {
    }

    public static DataStore createDefault() {
        String mode = System.getenv().getOrDefault("MUSICPLAYER_DB_MODE", "jdbc").trim().toLowerCase();
        if ("memory".equals(mode)) {
            return new MemoryDataStore();
        }

        String url = System.getenv().getOrDefault("MUSICPLAYER_JDBC_URL", DEFAULT_MYSQL_URL);
        String user = System.getenv().getOrDefault("MUSICPLAYER_JDBC_USER", DEFAULT_MYSQL_USER);
        String password = System.getenv().getOrDefault("MUSICPLAYER_JDBC_PASSWORD", DEFAULT_MYSQL_PASSWORD);
        try {
            return createJdbcStore(url, user, password);
        } catch (Exception e) {
            System.err.println("JDBC 鏁版嵁婧愪笉鍙敤锛屽凡鍥為€€鍒板唴瀛樻ā寮? " + e.getMessage());
            return new MemoryDataStore();
        }
    }

    public static DataStore createJdbcStore(String url, String user, String password) throws SQLException {
        prepareJdbc(url, user, password);
        try (Connection ignored = openConnection(url, user, password)) {
            return new JdbcDataStore(url, user, password);
        }
    }

    public static Connection openConnection(String url, String user, String password) throws SQLException {
        prepareJdbc(url, user, password);
        if (isMySql(url) && mysqlDriverInstance != null) {
            return connectWithDriver(mysqlDriverInstance, url, user, password);
        }
        if (isSqlite(url) && sqliteDriverInstance != null) {
            return connectWithDriver(sqliteDriverInstance, url, user, password);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static Connection connectWithDriver(Driver driver, String url, String user, String password) throws SQLException {
        Properties properties = new Properties();
        if (user != null && !user.isBlank()) {
            properties.setProperty("user", user);
        }
        if (password != null && !password.isBlank()) {
            properties.setProperty("password", password);
        }
        Connection connection = driver.connect(url, properties);
        if (connection == null) {
            throw new SQLException("鏃犳硶寤虹珛鏁版嵁搴撹繛鎺? " + url);
        }
        return connection;
    }

    private static void prepareJdbc(String url, String user, String password) {
        if (isMySql(url)) {
            ensureMySqlDriver();
            createMySqlDatabaseIfNeeded(url, user, password);
            return;
        }
        if (isSqlite(url)) {
            ensureSqliteDriver();
            String dbPath = url.substring("jdbc:sqlite:".length());
            Path parent = Path.of(dbPath).toAbsolutePath().normalize().getParent();
            if (parent != null) {
                try {
                    Files.createDirectories(parent);
                } catch (Exception e) {
                    throw new IllegalStateException("鏃犳硶鍒涘缓 SQLite 鏁版嵁搴撶洰褰? " + parent, e);
                }
            }
        }
    }

    private static boolean isMySql(String url) {
        return url != null && url.startsWith("jdbc:mysql:");
    }

    private static boolean isSqlite(String url) {
        return url != null && url.startsWith("jdbc:sqlite:");
    }

    private static synchronized void ensureMySqlDriver() {
        if (mysqlDriverInstance != null) {
            return;
        }
        mysqlDriverInstance = instantiateDriver(MYSQL_DRIVER, MYSQL_DRIVER_JAR, true);
    }

    private static synchronized void ensureSqliteDriver() {
        if (sqliteDriverInstance != null) {
            return;
        }
        sqliteDriverInstance = instantiateDriver(SQLITE_DRIVER, SQLITE_DRIVER_JAR, false);
    }

    private static Driver instantiateDriver(String className, String jarName, boolean mysql) {
        try {
            try {
                Class<?> driverClass = Class.forName(className);
                return (Driver) driverClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException ignored) {
                // Fall through to local lib loading.
            }

            Path jarPath = Path.of("lib", jarName).toAbsolutePath().normalize();
            if (!Files.exists(jarPath)) {
                throw new IllegalStateException("鏈壘鍒?" + (mysql ? "MySQL" : "SQLite") + " JDBC 椹卞姩锛岃纭 " + jarPath + " 宸插姞鍏?classpath");
            }

            URLClassLoader loader = new URLClassLoader(
                new URL[] { jarPath.toUri().toURL() },
                DataStores.class.getClassLoader()
            );
            if (mysql) {
                mysqlDriverLoader = loader;
            } else {
                sqliteDriverLoader = loader;
            }
            Class<?> driverClass = Class.forName(className, true, loader);
            return (Driver) driverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("鍔犺浇 " + (mysql ? "MySQL" : "SQLite") + " JDBC 椹卞姩澶辫触锛岃纭 lib/" + jarName + " 鍙敤", e);
        }
    }

    private static void createMySqlDatabaseIfNeeded(String url, String user, String password) {
        try {
            String databaseName = extractDatabaseName(url);
            if (databaseName == null || databaseName.isBlank()) {
                throw new IllegalStateException("MySQL JDBC URL 鏈寘鍚暟鎹簱鍚? " + url);
            }
            String serverUrl = extractMySqlServerUrl(url);
            try (Connection connection = connectWithDriver(mysqlDriverInstance, serverUrl, user, password);
                 java.sql.Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("鍒濆鍖?MySQL 鏁版嵁搴撳け璐? " + e.getMessage(), e);
        }
    }

    private static String extractMySqlServerUrl(String url) {
        int schemeEnd = url.indexOf("//");
        int dbStart = url.indexOf('/', schemeEnd + 2);
        if (dbStart < 0) {
            return url;
        }
        int queryStart = url.indexOf('?', dbStart);
        String hostPart = url.substring(0, dbStart);
        String queryPart = queryStart >= 0 ? url.substring(queryStart) : "";
        return hostPart + "/" + queryPart;
    }

    private static String extractDatabaseName(String url) {
        int schemeEnd = url.indexOf("//");
        int dbStart = url.indexOf('/', schemeEnd + 2);
        if (dbStart < 0 || dbStart == url.length() - 1) {
            return null;
        }
        int queryStart = url.indexOf('?', dbStart);
        return queryStart >= 0 ? url.substring(dbStart + 1, queryStart) : url.substring(dbStart + 1);
    }
}
