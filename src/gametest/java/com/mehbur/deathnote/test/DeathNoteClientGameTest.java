package com.mehbur.deathnote.test;

import com.mehbur.deathnote.client.screen.DeathNoteScreen;
import com.mehbur.deathnote.item.DeathNoteItems;
import com.mehbur.deathnote.registry.ModEnchantments;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * End-to-end client test (run with {@code gradlew runClientGameTest}): a real client right-clicks a
 * Death Note, types a name in the screen, sends it, and the integrated server kills that player.
 * Screenshots of every step land in {@code build/run/clientGameTest/screenshots}.
 */
public final class DeathNoteClientGameTest implements FabricClientGameTest {
	private static final String VICTIM = "Victim";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();

			// Survival writer with 40 levels: notebook in slot 0 (selected), quill in slot 1. Plus a victim.
			server.runOnServer(minecraftServer -> {
				ServerPlayer player = connection.getServerPlayer();
				player.setGameMode(GameType.SURVIVAL);
				player.giveExperienceLevels(40 - player.experienceLevel);
				Holder<Enchantment> deathNote = minecraftServer.registryAccess()
						.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.DEATH_NOTE);
				player.getInventory().setItem(0, DeathNoteItems.create(Items.WRITABLE_BOOK, deathNote, 30));
				player.getInventory().setItem(1, DeathNoteItems.create(Items.FEATHER, deathNote, 30));
				spawnFakePlayer(minecraftServer, VICTIM);
			});
			context.getInput().lookAt(0.0F, -90.0F); // look at the sky so the click is a plain item use
			context.waitTicks(20);
			context.takeScreenshot("deathnote-1-hotbar");

			// Right-click opens the name screen instead of the vanilla book screen.
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(DeathNoteScreen.class);
			context.getInput().typeChars(VICTIM);
			context.waitTick();
			context.takeScreenshot("deathnote-2-name-screen");
			if (!context.tryClickScreenButton("deathnote.screen.write")) {
				context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
			}
			context.waitForScreen(null);
			connection.waitForServerboundPackets();
			context.waitTicks(10);

			boolean victimDead = server.computeOnServer(minecraftServer -> {
				ServerPlayer victim = minecraftServer.getPlayerList().getPlayerByName(VICTIM);
				return victim != null && victim.isDeadOrDying();
			});
			int usesLeft = server.computeOnServer(minecraftServer ->
					DeathNoteItems.getUsesRemaining(connection.getServerPlayer().getInventory().getItem(0)));
			int levelsLeft = server.computeOnServer(minecraftServer -> connection.getServerPlayer().experienceLevel);
			check(victimDead, "the victim must be dead");
			check(usesLeft == 29, "uses left must be 29 but was " + usesLeft);
			check(levelsLeft == 10, "levels left must be 10 but was " + levelsLeft);
			context.takeScreenshot("deathnote-3-after-kill");

			// Tooltips: open the inventory and hover the two hotbar slots.
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitForScreen(InventoryScreen.class);
			hoverHotbarSlot(context, 0);
			context.takeScreenshot("deathnote-4-tooltip-notebook");
			hoverHotbarSlot(context, 1);
			context.takeScreenshot("deathnote-5-tooltip-quill");
			context.setScreen(() -> null);
		}
	}

	/** Adds a server-side player with no real client, like vanilla's game-test mock players. */
	private static void spawnFakePlayer(MinecraftServer server, String name) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
	}

	/** Moves the cursor over a hotbar slot of the survival inventory screen (176x166 GUI). */
	private static void hoverHotbarSlot(ClientGameTestContext context, int slot) {
		double[] position = context.computeOnClient(minecraft -> {
			int scale = minecraft.getWindow().getGuiScale();
			int left = (minecraft.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (minecraft.getWindow().getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + 8 + slot * 18 + 8) * (double) scale, (top + 142 + 8) * (double) scale};
		});
		context.getInput().setCursorPos(position[0], position[1]);
		context.waitTicks(3);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
