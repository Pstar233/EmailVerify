package EmailVerify.emailVerify;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import EmailVerify.emailVerify.Command.CommandAdmin;
import EmailVerify.emailVerify.Command.CommandMail;
import EmailVerify.emailVerify.Command.CommandUnbind;
import EmailVerify.emailVerify.Email.EmailUnbinder;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Email.VerificationManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmailVerify extends JavaPlugin {
    private static VerificationManager verificationManager;//数据库配置文件
    private static EmailVerify emailVerify;//插件主类
    public static Map<UUID, Long> requestTimestamps = new HashMap();
    private static EmailUnbinder emailUnbinder;

    public EmailVerify() {
    }

    public void onEnable() {
        this.getLogger().info("插件加载中 by P_star");
        this.saveDefaultConfig();
        emailVerify = this;
        LanguageHelper.loadLang();

        //创建数据库对象
        verificationManager = new VerificationManager(this);
        emailUnbinder = new EmailUnbinder(this);

        //注册插件命令
        this.getServer().getPluginCommand("bindmail").setExecutor(new CommandMail(this , verificationManager));
        this.getServer().getPluginCommand("mailadmin").setExecutor(new CommandAdmin());
        this.getServer().getPluginCommand("unbindmail").setExecutor(new CommandUnbind(this, emailUnbinder));
        //((PluginCommand) Objects.requireNonNull(this.getCommand("bindmail"))).setExecutor(new CommandMail(this, verificationManager));
        //((PluginCommand) Objects.requireNonNull(this.getCommand("mailadmin"))).setExecutor(new CommandAdmin());
        //((PluginCommand) Objects.requireNonNull(this.getCommand("unbindmail"))).setExecutor(new CommandUnbind(this, emailUnbinder));
        //this.getLogger().info("指令注册成功");

        //注册时事件监听器
        this.getServer().getPluginManager().registerEvents(verificationManager, this);
        this.getServer().getPluginManager().registerEvents(emailUnbinder, this);
        this.getLogger().info(" ____             __                                      ");
        this.getLogger().info("/\\  _`\\          /\\ \\__                               ");
        this.getLogger().info("\\ \\ \\L\\ \\  ____  \\ \\ ,_\\     __      _ __         ");
        this.getLogger().info(" \\ \\ ,__/ /',__\\  \\ \\ \\/   /'__`\\   /\\`'__\\      ");
        this.getLogger().info("  \\ \\ \\/ /\\__, `\\  \\ \\ \\_ /\\ \\L\\.\\_ \\ \\ \\/ ");
        this.getLogger().info("   \\ \\_\\ \\/\\____/   \\ \\__\\\\ \\__/.\\_\\ \\ \\_\\ ");
        this.getLogger().info("    \\/_/  \\/___/     \\/__/ \\/__/\\/_/  \\/_/          ");
    }

    public void onDisable() {
    }

    public static VerificationManager getVerificationManager() {
        return verificationManager;
    }

    public static EmailVerify getPlugin() {
        return emailVerify;
    }

    public static EmailUnbinder getEmailUnbinder() {
        return emailUnbinder;
    }
}
