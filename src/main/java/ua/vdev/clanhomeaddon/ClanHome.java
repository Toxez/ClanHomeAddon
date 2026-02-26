package ua.vdev.clanhomeaddon;

public record ClanHome(
        String clanName,
        String homeName,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public String formatCoords() {
        return String.format("%d, %d, %d",
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z));
    }
}