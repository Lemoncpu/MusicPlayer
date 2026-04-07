import com.musicplayer.util.PasswordUtil;
public class PrintDefaultHash {
    public static void main(String[] args) {
        System.out.print(PasswordUtil.hashPassword("123456"));
    }
}
