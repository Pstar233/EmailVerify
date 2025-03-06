package EmailVerify.emailVerify.Command;


import EmailVerify.emailVerify.Email.EmailUnbinder;
import EmailVerify.emailVerify.EmailVerify;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandUnbind implements CommandExecutor {
    private final EmailVerify plugin;
    private final EmailUnbinder emailUnbinder;

    public CommandUnbind(EmailVerify plugin, EmailUnbinder emailUnbinder) {
        this.plugin = plugin;
        this.emailUnbinder = emailUnbinder;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length != 1) {
                player.sendMessage(LanguageHelper.instance.getMessage("messages.unbind_help"));
                return true;
            } else {
                if (args[0].equalsIgnoreCase("confirm")) {
                    this.emailUnbinder.initiateUnbind(player);
                } else {
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.unbind_help"));
                }

                return true;
            }
        } else {
            sender.sendMessage(MessageManager.MessageFormat("&c此命令只能由玩家执行。"));
            return true;
        }
    }
}
