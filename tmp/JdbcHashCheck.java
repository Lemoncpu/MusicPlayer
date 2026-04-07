import com.musicplayer.model.UserSession;
import com.musicplayer.store.JdbcDataStore;
import com.musicplayer.store.DataStores;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class JdbcHashCheck {
    public static void main(String[] args) throws Exception {
        JdbcDataStore store = (JdbcDataStore) DataStores.createJdbcStore(
            "jdbc:mysql://127.0.0.1:3306/musicplayer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8",
            "root",
            "qx418504"
        );
        String username = "hashcheckuser";
        try (Connection connection = DataStores.openConnection(
            "jdbc:mysql://127.0.0.1:3306/musicplayer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8",
            "root",
            "qx418504"
        )) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM app_user WHERE username = ?")) {
                delete.setString(1, username);
                delete.executeUpdate();
            }
        }
        UserSession session = store.register(username, "secret123");
        UserSession login = store.login(username, "secret123");
        try (Connection connection = DataStores.openConnection(
            "jdbc:mysql://127.0.0.1:3306/musicplayer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8",
            "root",
            "qx418504"
        ); PreparedStatement query = connection.prepareStatement("SELECT password FROM app_user WHERE username = ?")) {
            query.setString(1, username);
            try (ResultSet rs = query.executeQuery()) {
                rs.next();
                String stored = rs.getString(1);
                System.out.println(session.username().equals(login.username()) ? "LOGIN_OK" : "LOGIN_BAD");
                System.out.println(stored.startsWith("pbkdf2$") ? "DB_HASH_OK" : stored);
                System.out.println(!stored.equals("secret123") ? "NOT_PLAINTEXT" : "PLAINTEXT_BAD");
            }
        }
        store.close();
    }
}
