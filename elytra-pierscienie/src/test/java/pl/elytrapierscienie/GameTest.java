package pl.elytrapierscienie;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private ServerMock server;
    private PlayerMock admin;
    private PlayerMock player;
    private World world;
    private ElytraPierscienie plugin;

    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(ElytraPierscienie.class);
        admin = server.addPlayer("Admin");
        player = server.addPlayer("Gracz");
        admin.setOp(true);
        admin.teleport(new Location(world, 0, 100, 0, 0, 0));
    }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void commandsAndScoringRespectRoundAndCooldown() {
        assertTrue(admin.performCommand("elytra dodaj test 3 5"));
        assertTrue(player.performCommand("elytra dolacz"));
        assertTrue(admin.performCommand("elytra start 10"));
        player.setGliding(true);
        server.getPluginManager().callEvent(new PlayerMoveEvent(player,
                new Location(world, 0, 100, -2), new Location(world, 0, 100, 2)));
        assertTrue(player.nextMessage().contains("Dołączono"));
        assertTrue(player.nextMessage().contains("wystartowała"));
        assertTrue(player.nextMessage().contains("+5"));
        server.getPluginManager().callEvent(new PlayerMoveEvent(player,
                new Location(world, 0, 100, 2), new Location(world, 0, 100, -2)));
        assertNull(player.nextMessage());
        server.getScheduler().performTicks(200);
        assertTrue(player.nextMessage().contains("Koniec rundy"));
        assertTrue(player.nextMessage().contains("Gracz — 5 pkt"));
    }
}
