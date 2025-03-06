package EmailVerify.emailVerify.Message;


import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class MessageManager {
    public MessageManager() {
    }

    public static String MessageFormat(String message) {
        return message.replace("&", "§");
    }

    public static String MessageFormat(String message, Player player) {
        message = PlaceholderAPI.setPlaceholders(player, message);
        return MessageFormat(message);
    }
}
