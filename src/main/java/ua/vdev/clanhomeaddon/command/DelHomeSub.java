package ua.vdev.clanhomeaddon.command;

import org.bukkit.entity.Player;
import ua.vdev.clanhomeaddon.ClanHome;
import ua.vdev.clanhomeaddon.HomeConfig;
import ua.vdev.clanhomeaddon.database.HomeDatabase;
import ua.vdev.clanhomeaddon.ClanHomeAddon;
import ua.vdev.primeclans.api.command.AddonSubCommand;
import ua.vdev.primeclans.manager.ClanManager;
import ua.vdev.vlibapi.player.PlayerMsg;

import java.util.List;

public class DelHomeSub implements AddonSubCommand {

    private final HomeDatabase database;
    private final HomeConfig config;
    private final ClanManager clanManager;

    public DelHomeSub(HomeDatabase database, HomeConfig config, ClanManager clanManager) {
        this.database = database;
        this.config = config;
        this.clanManager = clanManager;
    }

    @Override
    public String getName() { return "delhome"; }

    @Override
    public void execute(Player player, String[] args) {
        clanManager.getPlayerClan(player.getUniqueId()).ifPresentOrElse(clan -> {

            if (!clan.isOwner(player.getUniqueId())
                    && !clan.hasPerm(player.getUniqueId(), ClanHomeAddon.PERM_DEL)) {
                send(player, config.getMessage("no-permission"));
                return;
            }

            if (args.length < 2) {
                send(player, config.getMessage("delhome.usage"));
                return;
            }

            String homeName = args[1];
            boolean deleted = database.deleteHome(clan.name(), homeName);

            if (deleted) {
                send(player, config.getMessage("delhome.success")
                        .replace("{home}", homeName)
                        .replace("{clan}", clan.name()));
            } else {
                send(player, config.getMessage("delhome.not-found")
                        .replace("{home}", homeName));
            }

        }, () -> send(player, config.getMessage("no-clan")));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return clanManager.getPlayerClan(player.getUniqueId())
                    .map(clan -> database.getHomes(clan.name()).stream()
                            .map(ClanHome::homeName)
                            .filter(n -> n.toLowerCase().startsWith(input))
                            .toList())
                    .orElse(List.of());
        }
        return List.of();
    }

    @Override
    public boolean requiresClan() { return true; }

    @Override
    public boolean requiresNoClan() { return false; }

    private void send(Player player, String msg) {
        PlayerMsg.send(player, msg);
    }
}