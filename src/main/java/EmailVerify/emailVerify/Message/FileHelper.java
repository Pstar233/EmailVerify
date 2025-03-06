package EmailVerify.emailVerify.Message;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import java.io.File;
import java.io.IOException;
import java.util.List;

import EmailVerify.emailVerify.EmailVerify;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class FileHelper {
    private File file;
    private FileConfiguration config;

    public FileHelper(String filePath, boolean isAbsolute) {
        if (!isAbsolute) {
            this.file = new File(EmailVerify.getPlugin().getDataFolder(), filePath);
            if (!this.file.exists()) {
                EmailVerify.getPlugin().saveResource(filePath, false);
            }
        } else {
            this.file = new File(filePath);
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public FileHelper(File file) {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public Object read(String path) {
        return this.config.get(path);
    }

    public List<String> getStringList(String path) {
        return this.config.getStringList(path);
    }

    public String getString(String path) {
        return this.config.getString(path);
    }

    public void write(String path, Object value) {
        this.config.set(path, value);
        this.save();
    }

    public void saveItemStack(String path, ItemStack item) {
        this.config.set(path, item.serialize());
        this.save();
    }

    public ItemStack loadItemStack(String path) {
        return this.config.contains(path) ? this.config.getItemStack(path) : null;
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException var2) {
            EmailVerify.getPlugin().getLogger().warning("文件 " + this.file.getName() + " 保存失败");
        }

    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(this.file);
    }
}
