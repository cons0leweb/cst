package org.cons0leweb.cst;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements CommandExecutor {

    private final Main plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ctitle")) {
            if (!sender.hasPermission("cst.use")) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("no_permission"));
                return true;
            }
            handleTitleCommand(sender, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("cchat")) {
            if (!sender.hasPermission("cst.use")) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("no_permission"));
                return true;
            }
            handleChatCommand(sender, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("chelp")) {
            if (!sender.hasPermission("cst.use")) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("no_permission"));
                return true;
            }
            sendHelpMessage(sender); // Вызов метода для отправки справочного сообщения
            return true;
        } else if (command.getName().equalsIgnoreCase("cactionbar")) {
            if (!sender.hasPermission("cst.use")) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("no_permission"));
                return true;
            }
            handleActionBarCommand(sender, args);
            return true;
        }
        return false;
    }

    private String[] parseArguments(String input) {
        return input.split(";");
    }

    private void handleTitleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("ctitle_usage"));
            return;
        }

        // Объединяем все аргументы в одну строку
        String fullArgs = String.join(" ", args);

        // Разбиваем строку по символу ';'
        String[] parts = fullArgs.split(";");

        // Проверяем минимальное количество частей
        if (parts.length < 3) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("ctitle_usage"));
            return;
        }

        // Первая часть — игрок
        String playerArg = parts[0].trim();
        List<Player> targetPlayers = getTargetPlayer(sender, playerArg);
        if (targetPlayers.isEmpty()) return;

        // Вторая часть — заголовок
        String title = parts[1].trim();

        // Третья часть — подзаголовок
        String subtitle = parts[2].trim();

        // Остальные части — время появления, задержки и исчезновения
        int fadeIn = 10; // Значение по умолчанию
        int stay = 70;   // Значение по умолчанию
        int fadeOut = 20; // Значение по умолчанию

        if (parts.length >= 4) {
            fadeIn = parseTime(sender, parts[3].trim(), "fade_in");
            if (fadeIn == -1) return;
        }

        if (parts.length >= 5) {
            stay = parseTime(sender, parts[4].trim(), "stay");
            if (stay == -1) return;
        }

        if (parts.length >= 6) {
            fadeOut = parseTime(sender, parts[5].trim(), "fade_out");
            if (fadeOut == -1) return;
        }

        // Применяем цветовые коды
        title = translateColorCodes(title);
        subtitle = translateColorCodes(subtitle);

        // Отправляем заголовок и подзаголовок игрокам
        for (Player player : targetPlayers) {
            sendTitleToPlayer(player, title, subtitle, fadeIn, stay, fadeOut);
        }

        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("message_sent"));
    }

    private void handleChatCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("cchat_usage"));
            return;
        }

        // Первая часть — игрок
        String playerArg = args[0].trim();
        List<Player> targetPlayers = getTargetPlayer(sender, playerArg);
        if (targetPlayers.isEmpty()) return;

        // Вторая часть — сообщение
        String message = args[1].trim();
        message = translateColorCodes(message);

        // Отправляем сообщение игрокам
        for (Player player : targetPlayers) {
            sendChatMessageToPlayer(player, message, "", "");
        }

        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("message_sent"));
    }

    private void handleActionBarCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("cactionbar_usage"));
            return;
        }

        // Первая часть — игрок
        String playerArg = args[0].trim();
        List<Player> targetPlayers = getTargetPlayer(sender, playerArg);
        if (targetPlayers.isEmpty()) return;

        // Вторая часть — сообщение
        String message = args[1].trim();
        message = translateColorCodes(message);

        // Отправляем сообщение в action bar
        for (Player player : targetPlayers) {
            sendActionBarToPlayer(player, message);
        }

        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("message_sent"));
    }

    private List<Player> getTargetPlayer(CommandSender sender, String target) {
        List<Player> targetPlayers = new ArrayList<>();

        if (target.equalsIgnoreCase("-")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("player_only_command"));
                return targetPlayers;
                //return empty
            }
            targetPlayers.add((Player) sender);
        } else if (target.equalsIgnoreCase("*")) {
            targetPlayers.addAll(plugin.getServer().getOnlinePlayers());
        } else {
            Player targetPlayer = plugin.getServer().getPlayer(target);
            if (targetPlayer == null) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage("player_not_found"));
                return targetPlayers;
            }
            targetPlayers.add(targetPlayer);
        }

        return targetPlayers;
    }

    private int parseTime(CommandSender sender, String timeStr, String timeType) {
        try {
            return Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("invalid_" + timeType));
            return -1;
        }
    }

    private void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private void sendChatMessageToPlayer(Player player, String message, String action, String hoverText) {
        TextComponent textComponent = new TextComponent(message);

        if (!action.isEmpty()) {
            try {
                ClickEvent.Action clickAction = ClickEvent.Action.valueOf(action.toUpperCase());
                textComponent.setClickEvent(new ClickEvent(clickAction, message));
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessageManager().getFormattedMessage("invalid_action"));
                return;
            }
        }

        if (!hoverText.isEmpty()) {
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        }

        player.spigot().sendMessage(textComponent);
    }

    private void sendActionBarToPlayer(Player player, String message) {
        player.sendActionBar(message);
    }

    private void sendHelpMessage(CommandSender sender) {
        if (!sender.hasPermission("cst.use")) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("no_permission"));
            return;
        }
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_separator"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_title"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_separator"));
        sender.sendMessage("");
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_ctitle_usage"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_ctitle_description"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_ctitle_example"));
        sender.sendMessage("");
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cchat_usage"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cchat_description"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cchat_example"));
        sender.sendMessage("");

        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cactionbar_usage"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cactionbar_description"));
        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_cactionbar_example"));
        sender.sendMessage("");

        sender.sendMessage(plugin.getMessageManager().getFormattedMessage("help_separator"));
    }

    private String translateColorCodes(String text) {

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}