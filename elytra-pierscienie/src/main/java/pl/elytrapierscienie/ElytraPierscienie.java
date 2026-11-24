package pl.elytrapierscienie;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ElytraPierscienie extends JavaPlugin implements Listener, CommandExecutor {
    private static final String PREFIX = ChatColor.AQUA + "[Elytra] " + ChatColor.RESET;
    private final Map<String, Ring> rings = new LinkedHashMap<>();
    private final Map<UUID, Participant> players = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> lastPass = new HashMap<>();
    private boolean running;
    private BukkitTask timer;
    private int remaining;
    private File ringsFile;

    private record Ring(String name, String world, double x, double y, double z, double nx, double nz, double radius, int points) {}
    private static final class Participant {
        String name;
        int points;
        Participant(String name) { this.name = name; }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        ringsFile = new File(getDataFolder(), "pierscienie.yml");
        loadRings();
        Objects.requireNonNull(getCommand("elytra")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::drawRings, 0L, 10L);
    }

    private void loadRings() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ringsFile);
        ConfigurationSection section = yaml.getConfigurationSection("pierscienie");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(name);
            if (s == null) continue;
            String world = s.getString("swiat", "");
            double nx = s.getDouble("nx"), nz = s.getDouble("nz"), length = Math.hypot(nx, nz);
            double x = s.getDouble("x"), y = s.getDouble("y"), z = s.getDouble("z"), radius = s.getDouble("promien");
            int points = s.getInt("punkty");
            if (!name.matches("[a-z0-9_-]{1,24}") || world.isBlank() || !Double.isFinite(length) || length < 0.01 ||
                !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Double.isFinite(radius) || radius < 1 || radius > 20 || points < 1 || points > 10000) {
                getLogger().warning("Pomijam niepoprawny pierścień: " + name); continue;
            }
            rings.put(name, new Ring(name, world, x, y, z, nx / length, nz / length, radius, points));
        }
    }

    private boolean saveRings() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Ring r : rings.values()) {
            String path = "pierscienie." + r.name() + ".";
            yaml.set(path + "swiat", r.world()); yaml.set(path + "x", r.x()); yaml.set(path + "y", r.y()); yaml.set(path + "z", r.z());
            yaml.set(path + "nx", r.nx()); yaml.set(path + "nz", r.nz());
            yaml.set(path + "promien", r.radius()); yaml.set(path + "punkty", r.points());
        }
        try { yaml.save(ringsFile); return true; }
        catch (IOException e) { getLogger().severe("Nie udało się zapisać pierścieni: " + e.getMessage()); return false; }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!running || event instanceof PlayerTeleportEvent || !event.getPlayer().isGliding() || event.getTo() == null) return;
        Player player = event.getPlayer();
        Participant participant = players.get(player.getUniqueId());
        if (participant == null) return;
        Location from = event.getFrom(), to = event.getTo();
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        String world = from.getWorld().getName();
        long now = System.nanoTime();
        long cooldown = Math.max(1, Math.min(3600, getConfig().getInt("przerwa-sekundy", 5))) * 1_000_000_000L;
        for (Ring ring : rings.values()) {
            if (!ring.world().equals(world) || !RingGeometry.crosses(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(),
                    ring.x(), ring.y(), ring.z(), ring.nx(), ring.nz(), ring.radius())) continue;
            Map<String, Long> passes = lastPass.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
            Long previous = passes.get(ring.name());
            if (previous != null && now - previous < cooldown) continue;
            passes.put(ring.name(), now);
            participant.points = (int) Math.min(Integer.MAX_VALUE, (long) participant.points + ring.points());
            player.sendMessage(PREFIX + ChatColor.GREEN + "+" + ring.points() + " pkt za " + ring.name() + "! Razem: " + participant.points);
        }
    }

    private void drawRings() {
        for (Ring ring : rings.values()) {
            World world = Bukkit.getWorld(ring.world());
            if (world == null || world.getPlayers().isEmpty() || !world.isChunkLoaded(((int) Math.floor(ring.x())) >> 4, ((int) Math.floor(ring.z())) >> 4)) continue;
            for (int i = 0; i < 48; i++) {
                double angle = 2 * Math.PI * i / 48;
                double side = Math.cos(angle) * ring.radius();
                world.spawnParticle(Particle.END_ROD, ring.x() - ring.nz() * side, ring.y() + Math.sin(angle) * ring.radius(), ring.z() + ring.nx() * side, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc")) { help(sender); return true; }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("dolacz")) {
            if (!(sender instanceof Player p)) { msg(sender, "Ta komenda jest dostępna tylko dla gracza."); return true; }
            if (players.containsKey(p.getUniqueId())) { msg(sender, "Już bierzesz udział w grze."); return true; }
            players.put(p.getUniqueId(), new Participant(p.getName()));
            msg(p, running ? "Dołączono do trwającej rundy. Leć przez pierścienie!" : "Dołączono do gry. Poczekaj na start rundy."); return true;
        }
        if (action.equals("wyjdz")) {
            if (!(sender instanceof Player p)) { msg(sender, "Ta komenda jest dostępna tylko dla gracza."); return true; }
            if (players.remove(p.getUniqueId()) == null) msg(sender, "Nie bierzesz udziału w grze.");
            else { lastPass.remove(p.getUniqueId()); msg(sender, "Opuszczono grę, Twój wynik został usunięty."); }
            return true;
        }
        if (action.equals("wyniki")) { showResults(sender); return true; }
        if (!sender.hasPermission("elytrapierscienie.admin")) { msg(sender, "Nie masz uprawnień do tej komendy."); return true; }
        switch (action) {
            case "lista" -> {
                if (rings.isEmpty()) msg(sender, "Brak pierścieni.");
                else rings.values().forEach(r -> msg(sender, r.name() + " | " + r.world() + " | promień " + r.radius() + " | " + r.points() + " pkt"));
            }
            case "dodaj" -> addRing(sender, args);
            case "usun" -> removeRing(sender, args);
            case "start" -> start(sender, args);
            case "stop" -> { if (!running) msg(sender, "Runda nie trwa."); else endRound(); }
            default -> help(sender);
        }
        return true;
    }

    private void addRing(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { msg(sender, "Pierścień może dodać tylko gracz."); return; }
        if (running) { msg(sender, "Zakończ rundę przed zmianą pierścieni."); return; }
        if (args.length < 2 || args.length > 4 || !args[1].toLowerCase(Locale.ROOT).matches("[a-z0-9_-]{1,24}")) {
            msg(sender, "Użycie: /elytra dodaj <nazwa> [promień 1-20] [punkty 1-10000]"); return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (rings.containsKey(name)) { msg(sender, "Pierścień o tej nazwie już istnieje."); return; }
        if (rings.size() >= 64) { msg(sender, "Limit to 64 pierścienie."); return; }
        try {
            double radius = args.length >= 3 ? Double.parseDouble(args[2]) : 3;
            int points = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
            if (!Double.isFinite(radius) || radius < 1 || radius > 20 || points < 1 || points > 10000) throw new NumberFormatException();
            Location l = p.getLocation();
            double yaw = Math.toRadians(l.getYaw());
            Ring ring = new Ring(name, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), -Math.sin(yaw), Math.cos(yaw), radius, points);
            rings.put(name, ring);
            if (!saveRings()) { rings.remove(name); msg(sender, "Błąd zapisu. Sprawdź log serwera."); return; }
            msg(sender, "Dodano pierścień " + name + " w Twojej pozycji, prostopadle do kierunku patrzenia.");
        } catch (NumberFormatException e) { msg(sender, "Podaj promień 1-20 i liczbę punktów 1-10000."); }
    }

    private void removeRing(CommandSender sender, String[] args) {
        if (running) { msg(sender, "Zakończ rundę przed zmianą pierścieni."); return; }
        if (args.length != 2) { msg(sender, "Użycie: /elytra usun <nazwa>"); return; }
        String name = args[1].toLowerCase(Locale.ROOT);
        Ring removed = rings.remove(name);
        if (removed == null) { msg(sender, "Nie znaleziono pierścienia."); return; }
        if (!saveRings()) { rings.put(name, removed); msg(sender, "Błąd zapisu. Sprawdź log serwera."); return; }
        msg(sender, "Usunięto pierścień " + name + ".");
    }

    private void start(CommandSender sender, String[] args) {
        if (running) { msg(sender, "Runda już trwa."); return; }
        if (rings.isEmpty()) { msg(sender, "Dodaj co najmniej jeden pierścień."); return; }
        if (players.isEmpty()) { msg(sender, "Najpierw przynajmniej jeden gracz musi wpisać /elytra dolacz."); return; }
        if (args.length > 2) { msg(sender, "Użycie: /elytra start [sekundy]"); return; }
        try {
            int seconds = args.length == 2 ? Integer.parseInt(args[1]) : getConfig().getInt("czas-rundy", 180);
            if (seconds < 10 || seconds > 3600) throw new NumberFormatException();
            players.values().forEach(p -> p.points = 0);
            lastPass.clear(); remaining = seconds; running = true;
            announce("Runda wystartowała! Czas: " + seconds + " s. Leć przez pierścienie na elytrze.");
            timer = Bukkit.getScheduler().runTaskTimer(this, () -> {
                remaining--;
                if (remaining == 60 || remaining == 30 || remaining == 10) announce("Pozostało " + remaining + " s.");
                if (remaining <= 0) endRound();
            }, 20L, 20L);
        } catch (NumberFormatException e) { msg(sender, "Czas rundy musi wynosić od 10 do 3600 sekund."); }
    }

    private void endRound() {
        running = false;
        if (timer != null) { timer.cancel(); timer = null; }
        announce("Koniec rundy! Wyniki:");
        List<String> lines = resultLines();
        if (lines.isEmpty()) announce("Brak uczestników.");
        else lines.forEach(this::announce);
        lastPass.clear();
    }
    private List<String> resultLines() {
        return players.values().stream().sorted(Comparator.comparingInt((Participant p) -> p.points).reversed().thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER))
                .limit(10).map(p -> p.name + " — " + p.points + " pkt").toList();
    }
    private void showResults(CommandSender sender) {
        List<String> lines = resultLines();
        msg(sender, running ? "Wyniki trwającej rundy:" : "Ostatnie wyniki / uczestnicy:");
        if (lines.isEmpty()) msg(sender, "Brak uczestników."); else lines.forEach(line -> msg(sender, line));
    }
    private void announce(String text) {
        for (UUID id : players.keySet()) { Player p = Bukkit.getPlayer(id); if (p != null) msg(p, text); }
    }
    private void msg(CommandSender sender, String text) { sender.sendMessage(PREFIX + text); }
    private void help(CommandSender sender) {
        msg(sender, "/elytra dolacz | wyjdz | wyniki");
        if (sender.hasPermission("elytrapierscienie.admin")) {
            msg(sender, "/elytra dodaj <nazwa> [promień] [punkty] | usun <nazwa> | lista");
            msg(sender, "/elytra start [sekundy] | stop");
        }
    }
}
