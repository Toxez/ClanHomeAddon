package ua.vdev.clanhomeaddon;

import ua.vdev.clanhomeaddon.command.*;
import ua.vdev.clanhomeaddon.database.HomeDatabase;
import ua.vdev.primeclans.addon.AbstractAddon;
import ua.vdev.primeclans.api.AddonAPI;

public class ClanHomeAddon extends AbstractAddon {

    public static final String PERM_SET = "HOME_SET";
    public static final String PERM_DEL = "HOME_DEL";
    public static final String PERM_USE = "HOME_USE";

    private HomeDatabase database;
    private HomeConfig homeConfig;

    @Override
    protected void onEnable() {
        saveDefaultConfig();
        homeConfig = new HomeConfig(getConfig());

        database = new HomeDatabase(getDataFolder());
        database.init();

        AddonAPI.registerPerm(PERM_SET, "home_set", "<white>Установка домов", "Право устанавливать клановые дома");
        AddonAPI.registerPerm(PERM_DEL, "home_del", "<white>Удаление домов", "Право удалять клановые дома");
        AddonAPI.registerPerm(PERM_USE, "home_use", "<white>Использование домов", "Право телепортироваться к клановым домам");

        AddonAPI.registerSubCommand(new SetHomeSub(database, homeConfig, getClanManager()));
        AddonAPI.registerSubCommand(new HomeSub(database, homeConfig, getClanManager()));
        AddonAPI.registerSubCommand(new DelHomeSub(database, homeConfig, getClanManager()));

        AddonAPI.onClanDelete(event -> database.deleteAllHomes(event.clan().name())
        );

        getLogger().info("ClanHomeAddon включён");
    }

    @Override
    protected void onDisable() {
        AddonAPI.unregisterSubCommand("sethome");
        AddonAPI.unregisterSubCommand("home");
        AddonAPI.unregisterSubCommand("delhome");
        AddonAPI.unregisterPerm(PERM_SET);
        AddonAPI.unregisterPerm(PERM_DEL);
        AddonAPI.unregisterPerm(PERM_USE);

        if (database != null) database.close();

        getLogger().info("ClanHomeAddon выключён");
    }
}