package EmailVerify.emailVerify.Command;


import EmailVerify.emailVerify.EmailVerify;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * 管理员命令
 * 重载插件
 * **/

public class CommandAdmin implements CommandExecutor {
    public CommandAdmin() {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        //管理员命令
        if (cmd.getName().equalsIgnoreCase("mailadmin")) {
            if (sender.hasPermission("emailverification.admin")) {
                if (args.length == 0) {
                    //插件命令帮助
                    sender.sendMessage("§6重载插件数据: §e/mailadmin reload");
                    sender.sendMessage("§6管理员指令帮助: §e/mailadmin help");
                    return true;
                } else if (args.length == 1) {
                    //判断并重载
                    if (args[0].equalsIgnoreCase("reload")) {
                        LanguageHelper.loadLang();
                        EmailVerify.getVerificationManager().reload();
                        //重载
                        EmailVerify.getPlugin().reloadConfig();
                        sender.sendMessage(MessageManager.MessageFormat("&a插件重载成功！"));
                        return true;
                    } else if (args[0].equalsIgnoreCase("help")) {

                        sender.sendMessage("§7重载插件数据: §e/mailadmin reload");
                        sender.sendMessage("§7管理员指令帮助: §e/mailadmin help");

                        return true;
                    } else {
                        //指令有错误，指令输入有误
                        sender.sendMessage(LanguageHelper.instance.getMessage("messages.error_command"));
                        return true;
                    }
                } else {
                    //指令有错误，指令输入有误
                    sender.sendMessage(LanguageHelper.instance.getMessage("messages.error_command"));
                    return true;
                }
            } else {
                //没有权限
                sender.sendMessage(LanguageHelper.instance.getMessage("messages.no_perm"));
                return true;
            }
        } else {
            return false;
        }
    }
}
