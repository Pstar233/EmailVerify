package EmailVerify.emailVerify.Email;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import EmailVerify.emailVerify.EmailVerify;
import EmailVerify.emailVerify.Message.LanguageHelper;
import EmailVerify.emailVerify.Message.MessageManager;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
// 修改1：导入 ScheduledTask 类型，用于保存任务引用
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class VerificationManager implements Listener {
    private final Map<UUID, String> playerCodes = new HashMap<>();
    private final Map<UUID, String> playerEmails = new HashMap<>();
    // 修改2：将 tasks 的类型由 BukkitRunnable 改为 ScheduledTask，用于存放 Folia 调度器返回的任务对象
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private File dataFile;

    // 玩家是否验证，或者取消验证。如果为 true，则代表取消了验证码过期通知
    private boolean overdue = false;

    private FileConfiguration dataConfig;

    // 构造方法，初始化数据文件
    public VerificationManager(EmailVerify plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!this.dataFile.exists()) {
            try {
                this.dataFile.createNewFile();
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
    }

    public void reload() {
        this.dataFile = new File(EmailVerify.getPlugin().getDataFolder(), "data.yml");
        if (!this.dataFile.exists()) {
            try {
                this.dataFile.createNewFile();
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
    }

    public void startVerification(final Player player, final String email) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (EmailVerify.requestTimestamps.containsKey(playerUUID)) {
            long lastRequestTime = EmailVerify.requestTimestamps.get(playerUUID);
            if (currentTime - lastRequestTime < 60000L) {
                player.sendMessage(LanguageHelper.instance.getMessage("messages.code_cooldown"));
                return;
            }
        }

        // 判断邮箱是否已被绑定
        if (this.isEmailVerified(email)) {
            player.sendMessage(LanguageHelper.instance.getMessage("messages.email_already_bind"));
        } else if (this.isPlayerVerified(player)) {
            player.sendMessage(LanguageHelper.instance.getMessage("messages.player_already_bind"));
        } else {
            this.playerEmails.put(player.getUniqueId(), email);
            final String code = this.generateCode();
            this.playerCodes.put(player.getUniqueId(), code);
            player.sendMessage(LanguageHelper.instance.getMessage("messages.code_wait"));
            EmailVerify.requestTimestamps.put(playerUUID, currentTime);

            // 使用异步任务计划程序发送邮件
            AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();
            asyncScheduler.runNow(EmailVerify.getPlugin(), t -> {
                String playername = player.getName();
                String senderMessBject = EmailVerify.getPlugin().getConfig().getString("sendermess.bindmess.title");
                String sendMessPath = EmailVerify.getPlugin().getConfig().getString("sendermess.bindmess.path")
                        .replace("{Captcha}", code)
                        .replace("{player}", playername);
                try {
                    (new EmailSender(EmailVerify.getPlugin())).sendEmail(email, senderMessBject, sendMessPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.error_email_send"));
                    return;
                }
                player.sendMessage(LanguageHelper.instance.getMessage("messages.code_send"));
                player.sendMessage(LanguageHelper.instance.getMessage("messages.code_send2"));
            });

            // 获取验证码失效时间配置
            long lapse = EmailVerify.getPlugin().getConfig().getLong("mail.lapse");
            // 修改3：使用 asyncScheduler.runAtFixedRate 调度验证码过期任务，
            // 并将返回的 ScheduledTask 对象存入 tasks Map 中，便于后续取消任务
            ScheduledTask task = asyncScheduler.runDelayed(EmailVerify.getPlugin(), t -> {
                VerificationManager.this.playerCodes.remove(player.getUniqueId());
                if (!overdue) {
                    player.sendMessage(LanguageHelper.instance.getMessage("messages.code_outdate"));
                } else {
                    overdue = false;
                }
            }, lapse, TimeUnit.SECONDS);
            tasks.put(player.getUniqueId(), task);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();

        if (this.playerCodes.containsKey(playerId) && !this.playerCodes.get(playerId).equals(message)) {
            // 当玩家输入“取消”时取消验证码流程
            if (message.equals("取消")) {
                overdue = true;
                // 修改4：从 tasks Map 中取出任务引用，并检查是否为 null，再取消任务
                ScheduledTask task = this.tasks.get(playerId);
                if (task != null) {
                    task.cancel();
                    this.tasks.remove(playerId);
                }
                this.playerCodes.remove(playerId);
                event.setCancelled(true);
                player.sendMessage(LanguageHelper.instance.getMessage("messages.cancel"));
            } else {
                event.setCancelled(true);
                player.sendMessage(LanguageHelper.instance.getMessage("messages.error_code"));
            }
        } else if (this.playerCodes.containsKey(playerId) && this.playerCodes.get(playerId).equals(message)) {
            // 正确输入验证码，完成绑定
            overdue = true;
            player.sendMessage(LanguageHelper.instance.getMessage("messages.bind_success"));
            event.setCancelled(true);
            this.dataConfig.set(playerId + ".name", player.getName());
            this.dataConfig.set(playerId + ".isVerified", true);
            this.dataConfig.set(playerId + ".email", this.playerEmails.get(playerId));
            this.saveDataConfig();
            this.playerCodes.remove(playerId);
            this.playerEmails.remove(playerId);
            EmailVerify.requestTimestamps.remove(playerId);
            List<String> commands = EmailVerify.getPlugin().getConfig().getStringList("commands.reward");
            for (String command : commands) {
                String formattedCommand = MessageManager.MessageFormat(command, player);
                EmailVerify.getPlugin().getServer().getGlobalRegionScheduler().run(EmailVerify.getPlugin(),task -> {
                    EmailVerify.getPlugin().getServer().dispatchCommand(EmailVerify.getPlugin().getServer().getConsoleSender(), formattedCommand);
                });
            }
        }
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // 判断邮箱是否已验证
    private boolean isEmailVerified(String email) {
        this.reload();
        int count = 0;
        int quantity = EmailVerify.getPlugin().getConfig().getInt("mail.quantity");
        Iterator<String> iterator = this.dataConfig.getKeys(false).iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (email.equals(this.dataConfig.getString(key + ".email"))) {
                count++;
                if (count >= quantity) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlayerVerified(Player player) {
        this.reload();
        return this.dataConfig.getBoolean(player.getUniqueId() + ".isVerified", false);
    }

    private void saveDataConfig() {
        try {
            this.dataConfig.save(this.dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
