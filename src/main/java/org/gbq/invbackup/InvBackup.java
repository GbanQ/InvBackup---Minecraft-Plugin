package org.gbq.invbackup;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class InvBackup extends JavaPlugin {

    private String worldName;
    private boolean worldCheckEnabled;
    private int saveInterval;
    private int maxVersions;
    @Override
    public void onEnable() {
        createFoldersAndFiles();
        saveDefaultConfig();

        saveInterval = getConfig().getInt("save-interval", 600) * 20;
        worldCheckEnabled = getConfig().getBoolean("check-world", true);
        worldName = getConfig().getString("world-name", "world");
        maxVersions = getConfig().getInt("max-versions", 100);

        this.getCommand("invbackup").setTabCompleter(new InvBackupTabCompleter());

        getLogger().info("Сохранение инвентаря каждые " + (saveInterval / 20) / 60 + " минут(ы)");
        getLogger().info("Проверка миров: " + worldCheckEnabled);
        getLogger().info("Мир по умолчанию: " + worldName);
        getLogger().info("Максимальное количество версий: " + maxVersions);
        startInventorySaveTask();
    }

    private void createFoldersAndFiles() {

        File inventoryFolder = new File(getDataFolder(), "inventories");
        if (!inventoryFolder.exists()) {
            inventoryFolder.mkdirs();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();

                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("save-interval", 600);
                config.set("check-world", true);
                config.set("world-name", "world");
                config.set("max-versions", 100);
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startInventorySaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getName().equals(worldName)) {
                        savePlayerInventory(player);
                    }
                }
            }
        }.runTaskTimer(this, 0, saveInterval);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("invbackup.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: ");
            sender.sendMessage(ChatColor.AQUA + "/invbackup save <игрок> - " + ChatColor.WHITE + "Сохранить инвентарь указанного игрока.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup saveall - " + ChatColor.WHITE + "Сохранить инвентари всех онлайн-игроков.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup restore <игрок> [версия] - " + ChatColor.WHITE + "Восстановить инвентарь указанного игрока. Если версия не указана, будет восстановлено последнее сохранение.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup toggoworldcheck <true|false> - " + ChatColor.WHITE + "Вкл/Выкл проверку мира при восстановлении инвентаря.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup setworld <название_мира> - " + ChatColor.WHITE + "Установить мир, в котором будет проверяться сохранение инвентаря.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup setinterval <время_в_минутaх> - " + ChatColor.WHITE + "Установить интервал сохранения инвентаря в минутах.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup setmaxversions <количество> - " + ChatColor.WHITE + "Установить максимальное количество версий сохранений.");
            sender.sendMessage(ChatColor.AQUA + "/invbackup reload - " + ChatColor.WHITE + "Перезагрузить конфигурацию плагина.");
            return true;
        }
        // Обработка команды saveall
        if (args[0].equalsIgnoreCase("saveall")) {
            saveAllPlayerInventories(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("toggleworldcheck")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /invbackup toggleworldcheck <true|false>");
                return true;
            }
            worldCheckEnabled = Boolean.parseBoolean(args[1]);
            getConfig().set("check-world", worldCheckEnabled);
            saveConfig();
            sender.sendMessage(ChatColor.YELLOW + "Проверка мира " + (worldCheckEnabled ? "включена" : "отключена") + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("setworld")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /invbackup setworld <название_мира>");
                return true;
            }
            worldName = args[1];
            getConfig().set("world-name", worldName);
            saveConfig();
            sender.sendMessage(ChatColor.YELLOW + "Мир для сохранения инвентаря установлен на: " + worldName);
            return true;
        }

        if (args[0].equalsIgnoreCase("setinterval")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /invbackup setinterval <время_в_минутaх>");
                return true;
            }
            try {
                int intervalInMinutes = Integer.parseInt(args[1]);
                saveInterval = intervalInMinutes * 60 * 20; // Преобразование минут в тики
                getConfig().set("save-interval", intervalInMinutes * 60); // Сохраняем интервал в минутах
                saveConfig();
                startInventorySaveTask(); // Перезапуск задачи с новым интервалом
                sender.sendMessage(ChatColor.YELLOW + "Интервал сохранения инвентаря установлен на: " + intervalInMinutes + " минут.");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Время должно быть числом.");
            }
            return true;
        }

        // Установка максимального количества версий
        if (args[0].equalsIgnoreCase("setmaxversions")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /invbackup setmaxversions <количество>");
                return true;
            }
            try {
                maxVersions = Integer.parseInt(args[1]);
                getConfig().set("max-versions", maxVersions);
                saveConfig();
                sender.sendMessage(ChatColor.YELLOW + "Максимальное количество версий установлено на: " + maxVersions);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть числом.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadPlugin(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /invbackup <save|restore> <игрок> [версия]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            savePlayerInventory(target);
            sender.sendMessage(ChatColor.GREEN + "Инвентарь игрока " + target.getName() + " сохранён.");
        } else if (args[0].equalsIgnoreCase("restore")) {

            if (args.length >= 3) {
                try {
                    int version = Integer.parseInt(args[2]);
                    restorePlayerInventory(target, version);
                    sender.sendMessage(ChatColor.GREEN + "Инвентарь игрока " + target.getName() + " восстановлен с версии " + version + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Версия должна быть числом.");
                }
            } else {
                restorePlayerInventory(target, -1);
                sender.sendMessage(ChatColor.GREEN + "Инвентарь игрока " + target.getName() + " восстановлен с последнего сохранения.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Неверная команда!");
        }
        return true;
    }
    private void saveAllPlayerInventories(CommandSender sender) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerInventory(player);
        }
        sender.sendMessage(ChatColor.GREEN + "Инвентари всех онлайн-игроков сохранены.");
    }
    private void savePlayerInventory(Player player) {
        File file = new File(getDataFolder() + "/inventories", player.getName() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<ItemStack> items = Arrays.asList(player.getInventory().getContents());
        List<ItemStack> armor = Arrays.asList(player.getInventory().getArmorContents());
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        String worldName = player.getWorld().getName();

        int versionCount = config.getConfigurationSection("versions") == null ? 0 : config.getConfigurationSection("versions").getKeys(false).size();

        if (versionCount >= maxVersions) {
            config.set("versions.1", null);

            // Сдвигаем все версии вниз
            for (int i = 1; i < maxVersions; i++) {
                config.set("versions." + i, config.get("versions." + (i + 1)));
            }
            config.set("versions." + maxVersions, null);
        }

        int newVersion = Math.min(versionCount + 1, maxVersions);
        config.set("versions." + newVersion + ".items", items);
        config.set("versions." + newVersion + ".armor", armor);
        config.set("versions." + newVersion + ".offHand", offHand);
        config.set("versions." + newVersion + ".timestamp", timeStamp);
        config.set("versions." + newVersion + ".world", worldName);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void reloadPlugin(CommandSender sender) {
        reloadConfig();
        createFoldersAndFiles();
        saveInterval = getConfig().getInt("save-interval", 200) * 20;
        worldCheckEnabled = getConfig().getBoolean("check-world", true);
        worldName = getConfig().getString("world-name", "world");
        maxVersions = getConfig().getInt("max-versions", 100);

        // Логирование загруженных параметров
        sender.sendMessage(ChatColor.GREEN + "Плагин успешно перезагружен!");
        getLogger().info("Плагин InvBackup перезагружен.");
        getLogger().info("Сохранение инвентаря каждые " + saveInterval + " тиков");
        getLogger().info("Проверка миров: " + worldCheckEnabled);
        getLogger().info("Мир по умолчанию: " + worldName);
        getLogger().info("Максимальное количество версий: " + maxVersions);
    }

    private void restorePlayerInventory(Player player, int version) {
        File file = new File(getDataFolder() + "/inventories", player.getName() + ".yml");
        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + "У вас нет сохранённых инвентарей.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = (version == -1) ? "versions." + config.getConfigurationSection("versions").getKeys(false).size() : "versions." + version;

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "Указанная версия не найдена.");
            return;
        }

        if (worldCheckEnabled) {
            String savedWorld = config.getString(path + ".world");
            if (!player.getWorld().getName().equals(savedWorld)) {
                player.sendMessage(ChatColor.RED + "Этот инвентарь был сохранён в другом мире: " + savedWorld);
                return;
            }
        }

        ItemStack[] items = config.getList(path + ".items").toArray(new ItemStack[0]);
        ItemStack[] armor = config.getList(path + ".armor").toArray(new ItemStack[0]);
        ItemStack offHand = config.getItemStack(path + ".offHand");

        player.getInventory().setContents(items);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offHand);

        player.sendMessage(ChatColor.GREEN + "Инвентарь успешно восстановлен.");
    }
}
