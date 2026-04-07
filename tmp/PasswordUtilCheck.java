import com.musicplayer.util.PasswordUtil;
public class PasswordUtilCheck {
    public static void main(String[] args) {
        String hash = PasswordUtil.hashPassword("123456");
        System.out.println(hash.startsWith("pbkdf2$") ? "HASH_OK" : "HASH_BAD");
        System.out.println(PasswordUtil.verifyPassword("123456", hash) ? "VERIFY_OK" : "VERIFY_BAD");
        System.out.println(!PasswordUtil.verifyPassword("wrong", hash) ? "REJECT_OK" : "REJECT_BAD");
    }
}
