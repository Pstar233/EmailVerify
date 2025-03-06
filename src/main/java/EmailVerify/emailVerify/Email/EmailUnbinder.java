package EmailVerify.emailVerify.Email;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import EmailVerify.emailVerify.EmailVerify;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Message.MessageManager;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EmailUnbinder implements Listener {
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, String> unbindCodes = new HashMap<>();
    // 修改1：将任务存储类型保持为 ScheduledTask，用于保存 Folia 调度器返回的任务对象
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();

    // 玩家是否验证或取消验证标志，true表示已取消（用于避免重复提示）
    private boolean overdue = false;

    public EmailUnbinder(EmailVerify plugin) {
        this.loadDataFile();
    }

    // 数据文件加载
    private void loadDataFile() {
        this.dataFile = new File(EmailVerify.getPlugin().getDataFolder(), "data.yml");
        if (!this.dataFile.exists()) {
            EmailVerify.getPlugin().saveResource("data.yml", false);
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
    }

    public void initiateUnbind(final Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (EmailVerify.requestTimestamps.containsKey(playerUUID)) {
            long lastRequestTime = EmailVerify.requestTimestamps.get(playerUUID);
            if (currentTime - lastRequestTime < 60000L) {
                // 每分钟只能请求一次验证码
                player.sendMessage(LanguageHelper.instance.getMessage("messages.code_cooldown"));
                return;
            }
        }
        // 重新加载数据文件
        this.loadDataFile();
        String playerKey = playerUUID.toString();
        if (this.dataConfig.contains(playerKey) && this.dataConfig.getBoolean(playerKey + ".isVerified")) {
            final String email = this.dataConfig.getString(playerKey + ".email");
            if (email != null && !email.isEmpty()) {
                // 生成解绑验证码
                final String code = this.generateCode();
                this.unbindCodes.put(playerUUID, code);
                // 通知玩家验证码正在发送
                player.sendMessage(LanguageHelper.instance.getMessage("messages.code_wait"));
                EmailVerify.requestTimestamps.put(playerUUID, currentTime);
                AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();
                asyncScheduler.runNow(EmailVerify.getPlugin(), t -> {
                    String playername = player.getName();
                    // 获取邮件标题及内容，并替换验证码与玩家名称
                    String senderMessBject = EmailVerify.getPlugin().getConfig().getString("sendermess.unbindmess.title");
                    String senderMessText = EmailVerify.getPlugin().getConfig().getString("sendermess.unbindmess.path")
                            .replace("{Captcha}", code)
                            .replace("{player}", playername);
                    try {
                        (new EmailSender(EmailVerify.getPlugin())).sendEmail(email, senderMessBject, senderMessText);
                    } catch (Exception var2) {
                        var2.printStackTrace();
                        EmailVerify.getPlugin().getLogger().warning("在连接验证服务器时出错！");
                        player.sendMessage(LanguageHelper.instance.getMessage("messages.error_email_send"));
                        return;
                    }
                    // 发送验证码成功后通知玩家
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.code_unbind_send"));
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.code_unbind_send2"));
                });

                // 修改2：获取验证码失效时间配置（单位秒），使用 asyncScheduler.runDelayed 调度一次性任务
                long lapse = EmailVerify.getPlugin().getConfig().getLong("mail.lapse");
                ScheduledTask task = asyncScheduler.runDelayed(EmailVerify.getPlugin(), t -> {
                    // 失效后移除验证码，并发送过期提示（仅发送一次）
                    EmailUnbinder.this.unbindCodes.remove(player.getUniqueId());
                    if (!overdue) {
                        player.sendMessage(LanguageHelper.instance.getMessage("messages.code_outdate"));
                    } else {
                        overdue = false;
                    }
                }, lapse, TimeUnit.SECONDS);
                // 保存任务引用，便于后续取消任务
                tasks.put(player.getUniqueId(), task);
            } else {
                player.sendMessage(LanguageHelper.instance.getMessage("messages.bind_not_found"));
            }
        } else {
            player.sendMessage(LanguageHelper.instance.getMessage("messages.not_verified"));
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();

        if (this.unbindCodes.containsKey(playerId) && !this.unbindCodes.get(playerId).equals(message)) {
            // 玩家输入"取消"则取消解绑操作
            if (message.equals("取消")) {
                overdue = true;
                // 修改3：从 tasks Map 中取出任务引用，进行空值判断后取消任务
                ScheduledTask task = this.tasks.get(playerId);
                if (task != null) {
                    task.cancel();
                    this.tasks.remove(playerId);
                }
                this.unbindCodes.remove(playerId);
                event.setCancelled(true);
                player.sendMessage(LanguageHelper.instance.getMessage("messages.cancel"));
            } else {
                event.setCancelled(true);
                player.sendMessage(LanguageHelper.instance.getMessage("messages.error_code"));
            }
        }
        // 如果玩家输入的验证码正确，则执行解绑操作
        if (this.unbindCodes.containsKey(playerId) && this.unbindCodes.get(playerId).equals(message)) {
            player.sendMessage(LanguageHelper.instance.getMessage("messages.bind_success"));
            overdue = true;
            event.setCancelled(true);
            // 取消绑定，将邮箱信息清空，并更新数据库
            this.dataConfig.set(playerId + ".email", null);
            this.dataConfig.set(playerId + ".isVerified", false);
            this.saveDataFile();
            EmailVerify.requestTimestamps.remove(playerId);
            this.unbindCodes.remove(playerId);
            // 执行解绑成功命令
            List<String> commands = EmailVerify.getPlugin().getConfig().getStringList("commands.unbind");
            for (String command : commands) {
                String formattedCommand = MessageManager.MessageFormat(command, player);
                EmailVerify.getPlugin().getServer().getGlobalRegionScheduler().run(EmailVerify.getPlugin(),task -> {
                    EmailVerify.getPlugin().getServer().dispatchCommand(EmailVerify.getPlugin().getServer().getConsoleSender(), formattedCommand);
                });
            }
        }
    }

    private void saveDataFile() {
        try {
            this.dataConfig.save(this.dataFile);
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    /**
     * 生成6位随机验证码
     **/
    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(code);
    }
}
