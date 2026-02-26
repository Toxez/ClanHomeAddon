package ua.vdev.clanhomeaddon.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ua.vdev.clanhomeaddon.ClanHome;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HomeDatabase {

    private HikariDataSource ds;
    private final File dataFolder;

    public HomeDatabase(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void init() {
        File dbFile = new File(dataFolder, "homes.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("ClanHome-Pool");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(10_000);

        ds = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS clan_homes (
                    clan_name VARCHAR(32) NOT NULL COLLATE NOCASE,
                    home_name VARCHAR(64) NOT NULL COLLATE NOCASE,
                    world VARCHAR(128) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw REAL NOT NULL DEFAULT 0,
                    pitch REAL NOT NULL DEFAULT 0,
                    PRIMARY KEY (clan_name, home_name)
                )
                """;
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<ClanHome> getHomes(String clanName) {
        List<ClanHome> result = new ArrayList<>();
        String sql = "SELECT * FROM clan_homes WHERE clan_name = ? ORDER BY home_name";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
        }
        return result;
    }

    public Optional<ClanHome> getHome(String clanName, String homeName) {
        String sql = "SELECT * FROM clan_homes WHERE clan_name = ? AND home_name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanName);
            ps.setString(2, homeName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
        }
        return Optional.empty();
    }

    public int countHomes(String clanName) {
        String sql = "SELECT COUNT(*) FROM clan_homes WHERE clan_name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
        }
        return 0;
    }

    public void setHome(ClanHome home) {
        String sql = """
                INSERT OR REPLACE INTO clan_homes
                (clan_name, home_name, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, home.clanName());
            ps.setString(2, home.homeName());
            ps.setString(3, home.world());
            ps.setDouble(4, home.x());
            ps.setDouble(5, home.y());
            ps.setDouble(6, home.z());
            ps.setFloat(7, home.yaw());
            ps.setFloat(8, home.pitch());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public boolean deleteHome(String clanName, String homeName) {
        String sql = "DELETE FROM clan_homes WHERE clan_name = ? AND home_name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanName);
            ps.setString(2, homeName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public void deleteAllHomes(String clanName) {
        String sql = "DELETE FROM clan_homes WHERE clan_name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanName);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }


    private ClanHome mapRow(ResultSet rs) throws SQLException {
        return new ClanHome(
                rs.getString("clan_name"),
                rs.getString("home_name"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
    }

    public void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}