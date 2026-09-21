package com.chaosmod.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpotanimID;

@Getter
final class ExplodingBarrelsEvent implements ChaosMod
{
	private static final int BARREL_COUNT = 10;
	private static final int MIN_BARREL_DISTANCE = 3;
	private static final int TRIGGER_DISTANCE = 1;
	private static final int TILE_SIZE = 128;
	private static final int FIREBALL_INTERVAL_TICKS = 1;
	private static final int FIREBALL_FLIGHT_TICKS = 2;
	private static final int FIREBALL_SOURCE_DISTANCE = 8;
	private static final int FIREBALL_STUN_TICKS = 3;
	private static final int VORKATH_RAPID_FIRE_SPOTANIM = 1483;
	private static final int VORKATH_RAPID_FIRE_SOUND = 3749;
	private static final int FIREBALL_PLAYER_SPOTANIM_KEY = 0x5646;
	private static final BasicStroke DANGER_STROKE = new BasicStroke(2);
	private static final Color CLOSE_FILL = new Color(255, 45, 20, 70);
	private static final Color NORMAL_FILL = new Color(255, 150, 20, 35);
	private static final Color FIREBALL_FILL = new Color(255, 45, 10, 85);

	private final String name = "Exploding Barrels";
	private final String description = "Avoid the barrels and Vorkath's rapid-fire target tiles";
	private final int durationSeconds;
	private final Random random;
	private final List<ExplodingBarrel> barrels = new ArrayList<>();
	private final List<PendingFireball> fireballs = new ArrayList<>();
	private LocalPoint lastPlayerLocation;
	private int movementUnlockTick = -1;
	private int nextFireballTick = -1;

	ExplodingBarrelsEvent(int durationSeconds, Random random)
	{
		this.durationSeconds = durationSeconds;
		this.random = random;
	}

	@Override public void start(Client client)
	{
		stop(client);
		Player player = client.getLocalPlayer();
		if (player == null || player.getLocalLocation() == null)
		{
			return;
		}

		ItemComposition barrelItem = client.getItemDefinition(ItemID.CASTLE_DRAKAN_POTENT_BARREL);
		ModelData barrelModelData = client.loadModelData(barrelItem.getInventoryModel());
		if (barrelModelData == null)
		{
			return;
		}
		Model barrelModel = barrelModelData.light(64, 850, -30, -50, -30);

		LocalPoint origin = EventTileUtil.snapToTileCenter(player.getLocalLocation());
		for (int i = 0; i < BARREL_COUNT; i++)
		{
			int x;
			int y;
			do
			{
				x = random.nextInt(13) - 6;
				y = random.nextInt(13) - 6;
			}
			while ((Math.abs(x) <= TRIGGER_DISTANCE && Math.abs(y) <= TRIGGER_DISTANCE)
				|| isTooCloseToExisting(origin, x, y));

			LocalPoint location = new LocalPoint(origin.getX() + x * TILE_SIZE,
				origin.getY() + y * TILE_SIZE, origin.getWorldView());
			RuneLiteObject object = client.createRuneLiteObject();
			object.setModel(barrelModel);
			object.setLocation(location, client.getPlane());
			object.setDrawFrontTilesFirst(true);
			object.setActive(true);
			barrels.add(new ExplodingBarrel(location, object));
		}
		nextFireballTick = client.getTickCount() + 2;
	}

	@Override public void onGameTick(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player != null && movementUnlockTick >= 0 && client.getTickCount() >= movementUnlockTick)
		{
			player.removeSpotAnim(FIREBALL_PLAYER_SPOTANIM_KEY);
			player.setAnimationFrame(0);
			player.setAnimation(-1);
			movementUnlockTick = -1;
		}

		if (player == null || player.getLocalLocation() == null)
		{
			return;
		}

		int tick = client.getTickCount();
		if (isPlayerMovementLocked(client))
		{
			fireballs.clear();
			nextFireballTick = movementUnlockTick + 1;
			return;
		}

		boolean fireballHit = false;
		Iterator<PendingFireball> iterator = fireballs.iterator();
		while (iterator.hasNext())
		{
			PendingFireball fireball = iterator.next();
			if (tick < fireball.impactTick)
			{
				continue;
			}
			iterator.remove();
			if (player.getWorldLocation().equals(fireball.worldLocation))
			{
				fireballHit = true;
				stunWithFireball(client, player);
				break;
			}
		}

		if (fireballHit)
		{
			fireballs.clear();
			nextFireballTick = movementUnlockTick + 1;
			return;
		}

		if (tick >= nextFireballTick)
		{
			launchFireball(client, player, tick);
			nextFireballTick = tick + FIREBALL_INTERVAL_TICKS;
		}
	}

	@Override public void onClientTick(Client client)
	{
		triggerBarrelEntry(client);
	}

	@Override public void onMenuOptionClicked(Client client, MenuOptionClicked event)
	{
		if (!EventInputUtil.isWalkClick(event))
		{
			return;
		}
		triggerBarrelEntry(client);
		if (isPlayerMovementLocked(client))
		{
			event.consume();
		}
	}

	@Override public void renderTileMarkers(Client client, Graphics2D graphics)
	{
		Graphics2D barrelGraphics = (Graphics2D) graphics.create();
		try
		{
			barrelGraphics.setStroke(DANGER_STROKE);
			for (ExplodingBarrel barrel : barrels)
			{
				if (barrel.exploded)
				{
					continue;
				}
				Polygon tile = Perspective.getCanvasTileAreaPoly(client, barrel.location, 3);
				if (tile == null)
				{
					continue;
				}
				boolean close = barrelIsClose(client, barrel);
				barrelGraphics.setColor(close ? CLOSE_FILL : NORMAL_FILL);
				barrelGraphics.fill(tile);
				barrelGraphics.setColor(close ? Color.RED : Color.ORANGE);
				barrelGraphics.draw(tile);
			}

			barrelGraphics.setColor(FIREBALL_FILL);
			for (PendingFireball fireball : fireballs)
			{
				Polygon tile = Perspective.getCanvasTileAreaPoly(client, fireball.localLocation, 1);
				if (tile != null)
				{
					barrelGraphics.fill(tile);
					barrelGraphics.setColor(Color.RED);
					barrelGraphics.draw(tile);
					barrelGraphics.setColor(FIREBALL_FILL);
				}
			}
		}
		finally
		{
			barrelGraphics.dispose();
		}
	}

	@Override public void stop(Client client)
	{
		for (ExplodingBarrel barrel : barrels)
		{
			barrel.object.setActive(false);
			barrel.object.setModel(null);
		}
		barrels.clear();
		fireballs.clear();
		if (client.getLocalPlayer() != null)
		{
			client.getLocalPlayer().removeSpotAnim(FIREBALL_PLAYER_SPOTANIM_KEY);
		}
		if (movementUnlockTick >= 0 && client.getLocalPlayer() != null)
		{
			client.getLocalPlayer().setAnimationFrame(0);
			client.getLocalPlayer().setAnimation(-1);
		}
		movementUnlockTick = -1;
		nextFireballTick = -1;
		lastPlayerLocation = null;
	}

	private void triggerBarrelEntry(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getLocalLocation() == null)
		{
			return;
		}
		LocalPoint playerLocation = player.getLocalLocation();
		for (ExplodingBarrel barrel : barrels)
		{
			if (!barrel.exploded && enteredDangerArea(lastPlayerLocation, playerLocation, barrel.location))
			{
				explode(client, player, barrel);
				break;
			}
		}
		lastPlayerLocation = playerLocation;
	}

	private boolean isPlayerMovementLocked(Client client)
	{
		return movementUnlockTick >= 0 && client.getTickCount() < movementUnlockTick;
	}

	private boolean barrelIsClose(Client client, ExplodingBarrel barrel)
	{
		Player player = client.getLocalPlayer();
		return player != null && player.getLocalLocation() != null
			&& !barrel.exploded && isInDangerArea(player.getLocalLocation(), barrel.location);
	}

	private void explode(Client client, Player player, ExplodingBarrel barrel)
	{
		barrel.exploded = true;
		barrel.object.setActive(false);
		WorldPoint explosionPoint = WorldPoint.fromLocal(client, barrel.location);
		int startCycle = client.getGameCycle();
		client.createProjectile(SpotanimID.BRAIN_BARREL_EXPLOSION,
			explosionPoint, 0, null, explosionPoint, 0, null,
			startCycle, startCycle + 30, 0, 0);
		player.setAnimationFrame(0);
		player.setAnimation(AnimationID.ROGUESDEN_PLAYER_EXPLOSION);
		movementUnlockTick = client.getTickCount() + 3;
	}

	private void launchFireball(Client client, Player player, int tick)
	{
		LocalPoint localTarget = EventTileUtil.snapToTileCenter(player.getLocalLocation());
		WorldPoint worldTarget = player.getWorldLocation();
		if (localTarget == null || worldTarget == null)
		{
			return;
		}

		int side = random.nextInt(4);
		int sourceX = worldTarget.getX();
		int sourceY = worldTarget.getY();
		if (side == 0) sourceX += FIREBALL_SOURCE_DISTANCE;
		else if (side == 1) sourceX -= FIREBALL_SOURCE_DISTANCE;
		else if (side == 2) sourceY += FIREBALL_SOURCE_DISTANCE;
		else sourceY -= FIREBALL_SOURCE_DISTANCE;

		WorldPoint source = new WorldPoint(sourceX, sourceY, worldTarget.getPlane());
		int startCycle = client.getGameCycle();
		client.playSoundEffect(VORKATH_RAPID_FIRE_SOUND);
		client.createProjectile(VORKATH_RAPID_FIRE_SPOTANIM,
			source, 80, null, worldTarget, 0, null,
			startCycle, startCycle + FIREBALL_FLIGHT_TICKS * 30, 16, 64);
		fireballs.add(new PendingFireball(localTarget, worldTarget,
			tick + FIREBALL_FLIGHT_TICKS));
	}

	private void stunWithFireball(Client client, Player player)
	{
		player.removeSpotAnim(FIREBALL_PLAYER_SPOTANIM_KEY);
		player.createSpotAnim(FIREBALL_PLAYER_SPOTANIM_KEY,
			VORKATH_RAPID_FIRE_SPOTANIM, 0, 0);
		player.setAnimationFrame(0);
		player.setAnimation(AnimationID.ROGUESDEN_PLAYER_EXPLOSION);
		movementUnlockTick = client.getTickCount() + FIREBALL_STUN_TICKS;
	}

	private boolean isTooCloseToExisting(LocalPoint origin, int x, int y)
	{
		int targetX = origin.getX() + x * TILE_SIZE;
		int targetY = origin.getY() + y * TILE_SIZE;
		for (ExplodingBarrel barrel : barrels)
		{
			if (Math.abs(barrel.location.getX() - targetX) < MIN_BARREL_DISTANCE * TILE_SIZE
				&& Math.abs(barrel.location.getY() - targetY) < MIN_BARREL_DISTANCE * TILE_SIZE)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks scene tiles, not raw local-coordinate distance. This matches the
	 * rendered 3x3 danger area exactly: the barrel's tile plus one tile on each
	 * side. The current location is always checked, so an entry is caught on the
	 * first client tick in that area; the previous location also handles a jump
	 * between client updates.
	 */
	private static boolean enteredDangerArea(LocalPoint previous, LocalPoint current, LocalPoint barrel)
	{
		return isInDangerArea(current, barrel)
			&& (previous == null || !isInDangerArea(previous, barrel));
	}

	private static boolean isInDangerArea(LocalPoint player, LocalPoint barrel)
	{
		return Math.abs(player.getSceneX() - barrel.getSceneX()) <= TRIGGER_DISTANCE
			&& Math.abs(player.getSceneY() - barrel.getSceneY()) <= TRIGGER_DISTANCE;
	}

	private static final class ExplodingBarrel
	{
		private final LocalPoint location;
		private final RuneLiteObject object;
		private boolean exploded;

		private ExplodingBarrel(LocalPoint location, RuneLiteObject object)
		{
			this.location = location;
			this.object = object;
		}
	}

	private static final class PendingFireball
	{
		private final LocalPoint localLocation;
		private final WorldPoint worldLocation;
		private final int impactTick;

		private PendingFireball(LocalPoint localLocation, WorldPoint worldLocation,
			int impactTick)
		{
			this.localLocation = localLocation;
			this.worldLocation = worldLocation;
			this.impactTick = impactTick;
		}
	}
}
