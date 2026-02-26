package ua.vdev.clanhomeaddon.command;

import org.bukkit.entity.Player;
import ua.vdev.clanhomeaddon.ClanHome;
import ua.vdev.clanhomeaddon.ClanHomeAddon;
import ua.vdev.clanhomeaddon.HomeConfig;
import ua.vdev.clanhomeaddon.database.HomeDatabase;
import ua.vdev.primeclans.api.command.AddonSubCommand;
import ua.vdev.primeclans.manager.ClanManager;
import ua.vdev.vlibapi.player.PlayerMsg;

public class SetHomeSub implements AddonSubCommand {

    private static final String NAME_PATTERN = "[a-zA-Zа-яёА-ЯЁ0-9_\\-]+";

    private final HomeDatabase database;
    private final HomeConfig config;
    private final ClanManager clanManager;

    public SetHomeSub(HomeDatabase database, HomeConfig config, ClanManager clanManager) {
        this.database = database;
        this.config = config;
        this.clanManager = clanManager;
    }

    @Override
    public String getName() { return "sethome"; }

    @Override
    public void execute(Player player, String[] args) {
        clanManager.getPlayerClan(player.getUniqueId()).ifPresentOrElse(clan -> {

            if (!clan.isOwner(player.getUniqueId())
                    && !clan.hasPerm(player.getUniqueId(), ClanHomeAddon.PERM_SET)) {
                send(player, config.getMessage("no-permission"));
                return;
            }

            if (args.length < 2) {
                send(player, config.getMessage("sethome.usage"));
                return;
            }

            String homeName = args[1];

            if (!homeName.matches(NAME_PATTERN)) {
                send(player, config.getMessage("sethome.invalid-name"));
                return;
            }

            int limit = config.getHomeLimit(clan.level());
            int current = database.countHomes(clan.name());
            boolean exists = database.getHome(clan.name(), homeName).isPresent();

            if (!exists && current >= limit) {
                String msg = config.getMessage("sethome.limit-reached")
                        .replace("{current}", String.valueOf(current))
                        .replace("{limit}",   String.valueOf(limit));
                send(player, msg);
                return;
            }

            ClanHome home = new ClanHome(
                    clan.name(),
                    homeName,
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch()
            );

            database.setHome(home);

            String msgKey = exists ? "sethome.updated" : "sethome.success";
            String msg = config.getMessage(msgKey)
                    .replace("{home}", homeName)
                    .replace("{clan}", clan.name());
            send(player, msg);

        }, () -> send(player, config.getMessage("no-clan")));
    }

    @Override
    public boolean requiresClan() { return true; }

    @Override
    public boolean requiresNoClan() { return false; }

    private void send(Player player, String msg) {
        PlayerMsg.send(player, msg);
    }
}