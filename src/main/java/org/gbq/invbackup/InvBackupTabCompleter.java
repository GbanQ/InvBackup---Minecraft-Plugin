package org.gbq.invbackup;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvBackupTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("save", "restore", "toggleworldcheck", "setworld", "setinterval","reload","setmaxversions","saveall"));
        } else if (args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("save")) {

            for (Player player : ((Player) sender).getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args[0].equalsIgnoreCase("toggleworldcheck")) {
            completions.addAll(Arrays.asList("true", "false"));
        } else if (args[0].equalsIgnoreCase("setworld")) {
            Bukkit.getWorlds().forEach(world -> completions.add(world.getName()));
        } else if (args[0].equalsIgnoreCase("setinterval")) {
            completions.addAll(Arrays.asList("10", "30", "60", "120"));
        }else if (args[0].equalsIgnoreCase("setmaxversions")) {
            completions.addAll(Arrays.asList("10", "25", "50", "100"));
        }

        return filterCompletions(completions, args.length > 0 ? args[args.length - 1] : "");
    }

    private List<String> filterCompletions(List<String> completions, String current) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(current.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}
