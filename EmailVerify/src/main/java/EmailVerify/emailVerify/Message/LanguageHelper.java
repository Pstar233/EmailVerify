package EmailVerify.emailVerify.Message;

import org.bukkit.entity.Player;

/**
 * 配置文件消息
 **/
public class LanguageHelper {
    public static LanguageHelper instance;
    private FileHelper langFile;

    public LanguageHelper(FileHelper langFile) {
        this.langFile = langFile;
    }

    public String getMessage(String path, Player player) {
        return MessageManager.MessageFormat(this.langFile.getString(path), player);
    }

    public String getMessage(String path) {
        return MessageManager.MessageFormat(this.langFile.getString(path));
    }

    public static void loadLang() {
        instance = new LanguageHelper(new FileHelper("lang.yml", false));
    }
}
