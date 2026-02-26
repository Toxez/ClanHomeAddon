package ua.vdev.clanhomeaddon.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ua.vdev.clanhomeaddon.ClanHome;
import ua.vdev.clanhomeaddon.HomeConfig;
import ua.vdev.clanhomeaddon.database.HomeDatabase;
import ua.vdev.clanhomeaddon.ClanHomeAddon;
import ua.vdev.primeclans.api.command.AddonSubCommand;
import ua.vdev.primeclans.manager.ClanManager;
import ua.vdev.primeclans.model.Clan;
import ua.vdev.vlibapi.player.PlayerMsg;
import ua.vdev.vlibapi.util.scheduler.Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeSub implements AddonSubCommand {

    private static final double MOVE_THRESHOLD = 0.3;
    private static final Set<String> ENTRY_PLACEHOLDERS = Set.of("{home}", "{world}", "{x}", "{y}", "{z}");
    private final HomeDatabase database;
    private final HomeConfig config;
    private final ClanManager clanManager;
    private final Map<UUID, Location> pendingTeleports = new ConcurrentHashMap<>();

    public HomeSub(HomeDatabase database, HomeConfig config, ClanManager clanManager) {
        this.database = database;
        this.config = config;
        this.clanManager = clanManager;
    }

    @Override
    public String getName() { return "home"; }

    @Override
    public void execute(Player player, String[] args) {
        clanManager.getPlayerClan(player.getUniqueId()).ifPresentOrElse(
                clan -> {
                    if (!clan.isOwner(player.getUniqueId())
                            && !clan.hasPerm(player.getUniqueId(), ClanHomeAddon.PERM_USE)) {
                        send(player, config.getMessage("no-permission"));
                        return;
                    }
                    if (args.length < 2) {
                        showList(player, clan);
                    } else {
                        startTeleport(player, clan, args[1]);
                    }
                },
                () -> send(player, config.getMessage("no-clan"))
        );
    }

    private void showList(Player player, Clan clan) {
        List<ClanHome> homes = database.getHomes(clan.name());

        if (homes.isEmpty()) {
            send(player, config.getMessage("home.no-homes"));
            return;
        }

        int limit = config.getHomeLimit(clan.level());
        List<String> lines = config.getMessageList("home.list");

        lines.forEach(line -> {
            if (isEntryLine(line)) {
                homes.forEach(home -> send(player, applyEntryPlaceholders(line, home)));
            } else {
                send(player, applyHeaderPlaceholders(line, clan.name(), homes.size(), limit));
            }
        });
    }

    private boolean isEntryLine(String line) {
        return ENTRY_PLACEHOLDERS.stream().anyMatch(line::contains);
    }

    private String applyHeaderPlaceholders(String line, String clanName, int current, int limit) {
        return line
                .replace("{clan}", clanName)
                .replace("{current}", String.valueOf(current))
                .replace("{limit}", String.valueOf(limit));
    }

    private String applyEntryPlaceholders(String line, ClanHome home) {
        return line
                .replace("{home}", home.homeName())
                .replace("{world}", home.world())
                .replace("{x}", String.valueOf((int) Math.floor(home.x())))
                .replace("{y}", String.valueOf((int) Math.floor(home.y())))
                .replace("{z}", String.valueOf((int) Math.floor(home.z())));
    }

    private void startTeleport(Player player, Clan clan, String homeName) {
        database.getHome(clan.name(), homeName).ifPresentOrElse(
                home -> resolveWorld(home).ifPresentOrElse(
                        world -> scheduleTeleport(player, home, world),
                        () -> send(player, config.getMessage("home.world-not-found").replace("{world}", home.world()))
                ),
                () -> send(player, config.getMessage("home.not-found").replace("{home}", homeName))
        );
    }

    private void scheduleTeleport(Player player, ClanHome home, World world) {
        int delay = config.getTeleportDelay();
        if (delay <= 0) {
            doTeleport(player, world, home);
            send(player, config.getMessage("home.teleported")
                    .replace("{home}", home.homeName()));
            return;
        }

        if (pendingTeleports.containsKey(player.getUniqueId())) return;
        Location startLoc = player.getLocation().clone();
        pendingTeleports.put(player.getUniqueId(), startLoc);
        send(player, config.getMessage("home.teleport-countdown")
                .replace("{home}",  home.homeName())
                .replace("{delay}", String.valueOf(delay)));

        Task.later((long) delay * 20L, () -> {
            pendingTeleports.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            if (config.isMoveCancelsTP() && hasMoved(startLoc, player.getLocation())) {
                send(player, config.getMessage("home.teleport-cancelled"));
                return;
            }

            doTeleport(player, world, home);
            send(player, config.getMessage("home.teleported")
                    .replace("{home}", home.homeName()));
        });
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 2) return List.of();
        String input = args[1].toLowerCase();
        return clanManager.getPlayerClan(player.getUniqueId())
                .map(clan -> database.getHomes(clan.name()).stream()
                        .map(ClanHome::homeName)
                        .filter(n -> n.toLowerCase().startsWith(input))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public boolean requiresClan()   { return true; }

    @Override
    public boolean requiresNoClan() { return false; }

    private void doTeleport(Player player, World world, ClanHome home) {
        player.teleport(new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch()));
    }

    private Optional<World> resolveWorld(ClanHome home) {
        return Optional.ofNullable(Bukkit.getWorld(home.world()));
    }

    private boolean hasMoved(Location from, Location to) {
        return Optional.ofNullable(from.getWorld())
                .filter(w -> w.equals(to.getWorld()))
                .map(w -> Math.abs(from.getX() - to.getX()) > MOVE_THRESHOLD
                        || Math.abs(from.getY() - to.getY()) > MOVE_THRESHOLD
                        || Math.abs(from.getZ() - to.getZ()) > MOVE_THRESHOLD)
                .orElse(true);
    }

    private void send(Player player, String msg) {
        PlayerMsg.send(player, msg);
    }
}