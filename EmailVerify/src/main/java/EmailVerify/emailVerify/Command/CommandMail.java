package EmailVerify.emailVerify.Command;


import java.util.List;
import java.util.regex.Pattern;

import EmailVerify.emailVerify.EmailVerify;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Message.MessageManager;
import EmailVerify.emailVerify.Email.VerificationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 绑定邮箱
 **/

public class CommandMail implements CommandExecutor {
    private final EmailVerify plugin;
    private final VerificationManager verificationManager;

    public CommandMail(EmailVerify plugin, VerificationManager verificationManager) {
        this.plugin = plugin;
        this.verificationManager = verificationManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {
        //判断输入是否是玩家
        if (sender instanceof Player player) {
            if (args.length == 0) {
                //发送帮助消息
                player.sendMessage(LanguageHelper.instance.getMessage("messages.bind_help"));
                return true;
            } else {
                String email = args[0];
                //判断邮箱地址
                if (!this.isValidEmail(email)) {
                    //无效的邮箱地址
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.error_email"));
                    return true;
                } else {
                    //执行发送邮件
                    this.verificationManager.startVerification(player, email);
                    return true;
                }
            }
        } else {
            sender.sendMessage(MessageManager.MessageFormat("&c此命令只能由玩家执行。"));
            return true;
        }
    }

    /**
     * 判断邮箱格式
     **/
    private boolean isValidEmail(String email) {
        //获取配置文件
        String designatemail = EmailVerify.getPlugin().getConfig().getString("designatemail.enable");
        //如果是 false 就允许任何邮箱
        if (designatemail.equals("false")) {
            //邮箱格式
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            Pattern pattern = Pattern.compile(emailRegex);

            return pattern.matcher(email).matches();
        } else {
            //拿到配置文件所有邮箱
            List<String> mailD = EmailVerify.getPlugin().getConfig().getStringList("designatemail.mail");

            for (int i = 0; i < mailD.size(); i++) {
                String mail = "^[A-Za-z0-9+_.-]+"+ mailD.get(i) +"+$";
                Pattern pattern = Pattern.compile(mail);
                if (pattern.matcher(email).matches()){
                    return true;
                }
            }

        }
        return false;
    }
}
