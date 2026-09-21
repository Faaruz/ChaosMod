package com.chaosmod.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.SpotanimID;

@Getter
final class YamaMeteorShowerEvent implements ChaosMod
{
	private static final int TILE_SIZE = 128;
	private static final int DANGER_RADIUS_TILES = 2;
	private static final long ATTACK_INTERVAL_NANOS = 5_000_000_000L;
	private static final int IMPACT_DELAY_TICKS = 3;
	private static final int METEOR_SOURCE_HEIGHT = 400;
	private static final int METEOR_SLOPE = 100;
	private static final int METEOR_IMPACT_SOUND = 4930;
	private static final long MOVEMENT_LOCK_NANOS = 5_000_000_000L;
	private static final BasicStroke AREA_STROKE = new BasicStroke(2.0f);
	private static final Color AREA_FILL = new Color(190, 35, 15, 75);
	private static final int[][] METEOR_SOURCE_OFFSETS =
	{
		{ -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 },
		{ 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 }
	};

	private final String name = "Yama Meteor Shower";
	private final String description = "Escape each 5x5 meteor impact or lose movement for 5 seconds";
	private final int durationSeconds;

	private LocalPoint targetLocation;
	private int impactTick = -1;
	private long nextAttackNanos;
	private long movementLockedUntilNanos;

	YamaMeteorShowerEvent(int durationSeconds)
	{
		this.durationSeconds = durationSeconds;
	}

	@Override
	public void start(Client client)
	{
		stop(client);
		nextAttackNanos = System.nanoTime();
	}

	@Override
	public void onClientTick(Client client)
	{
		if (!isMovementLocked() && targetLocation == null
			&& System.nanoTime() >= nextAttackNanos)
		{
			targetPlayer(client);
		}
	}

	@Override
	public void onGameTick(Client client)
	{
		int tick = client.getTickCount();
		if (targetLocation != null && tick >= impactTick)
		{
			impact(client);
		}
	}

	@Override
	public void onMenuOptionClicked(Client client, MenuOptionClicked event)
	{
		if (EventInputUtil.isWalkClick(event) && isMovementLocked())
		{
			event.consume();
		}
	}

	@Override
	public void render(Client client, Graphics2D graphics)
	{
		Graphics2D meteorGraphics = (Graphics2D) graphics.create();
		try
		{
			if (isMovementLocked())
			{
				long remainingMillis = Math.max(0L,
					(movementLockedUntilNanos - System.nanoTime()) / 1_000_000L);
				String text = String.format("MOVEMENT LOCKED: %.1fs", remainingMillis / 1000.0);
				meteorGraphics.setFont(meteorGraphics.getFont().deriveFont(Font.BOLD, 18f));
				int width = meteorGraphics.getFontMetrics().stringWidth(text);
				int x = (client.getCanvasWidth() - width) / 2;
				int y = 95;
				meteorGraphics.setColor(Color.BLACK);
				meteorGraphics.drawString(text, x + 2, y + 2);
				meteorGraphics.setColor(Color.RED);
				meteorGraphics.drawString(text, x, y);
			}
		}
		finally
		{
			meteorGraphics.dispose();
		}
	}

	@Override
	public void renderTileMarkers(Client client, Graphics2D graphics)
	{
		if (targetLocation == null)
		{
			return;
		}
		Graphics2D markerGraphics = (Graphics2D) graphics.create();
		try
		{
			Polygon dangerArea = Perspective.getCanvasTileAreaPoly(client, targetLocation, 5);
			if (dangerArea == null)
			{
				return;
			}
			markerGraphics.setColor(AREA_FILL);
			markerGraphics.fill(dangerArea);
			markerGraphics.setColor(Color.RED);
			markerGraphics.setStroke(AREA_STROKE);
			markerGraphics.draw(dangerArea);
		}
		finally
		{
			markerGraphics.dispose();
		}
	}

	@Override
	public void stop(Client client)
	{
		targetLocation = null;
		impactTick = -1;
		nextAttackNanos = 0L;
		movementLockedUntilNanos = 0L;
	}

	private void targetPlayer(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getLocalLocation() == null)
		{
			nextAttackNanos = System.nanoTime() + 100_000_000L;
			return;
		}

		targetLocation = EventTileUtil.snapToTileCenter(player.getLocalLocation());
		impactTick = client.getTickCount() + IMPACT_DELAY_TICKS;
		nextAttackNanos = System.nanoTime() + ATTACK_INTERVAL_NANOS;

		WorldPoint target = WorldPoint.fromLocal(client, targetLocation);
		WorldPoint source = findScreenVerticalSource(client, targetLocation, target);
		if (source != null && target != null)
		{
			int startCycle = client.getGameCycle();
			client.createProjectile(SpotanimID.VFX_YAMA_METEOR_PROJECTILE_01,
				source, METEOR_SOURCE_HEIGHT, null, target, 0, null,
				startCycle, startCycle + IMPACT_DELAY_TICKS * 30, METEOR_SLOPE, 0);
		}
	}

	private void impact(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player != null && player.getLocalLocation() != null
			&& isInsideDangerArea(player.getLocalLocation(), targetLocation))
		{
			movementLockedUntilNanos = System.nanoTime() + MOVEMENT_LOCK_NANOS;
			nextAttackNanos = movementLockedUntilNanos + ATTACK_INTERVAL_NANOS;
		}

		WorldPoint target = WorldPoint.fromLocal(client, targetLocation);
		if (target != null)
		{
			int startCycle = client.getGameCycle();
			client.createProjectile(SpotanimID.VFX_YAMA_METEOR_SPOTANIM01,
				target, 0, null, target, 0, null,
				startCycle, startCycle + 30, 0, 0);
		}
		client.playSoundEffect(METEOR_IMPACT_SOUND);

		targetLocation = null;
		impactTick = -1;
	}

	private boolean isMovementLocked()
	{
		return System.nanoTime() < movementLockedUntilNanos;
	}

	private static WorldPoint findScreenVerticalSource(Client client, LocalPoint targetLocation,
		WorldPoint target)
	{
		if (targetLocation == null || target == null)
		{
			return null;
		}

		Point targetCanvas = Perspective.localToCanvas(client, targetLocation, client.getPlane());
		if (targetCanvas == null)
		{
			return null;
		}

		WorldPoint bestSource = null;
		int bestHorizontalOffset = Integer.MAX_VALUE;
		for (int[] offset : METEOR_SOURCE_OFFSETS)
		{
			LocalPoint candidateLocation = targetLocation
				.dx(offset[0] * TILE_SIZE)
				.dy(offset[1] * TILE_SIZE);
			Point candidateCanvas = Perspective.localToCanvas(client, candidateLocation,
				client.getPlane(), METEOR_SOURCE_HEIGHT);
			if (candidateCanvas == null || candidateCanvas.getY() >= targetCanvas.getY())
			{
				continue;
			}

			int horizontalOffset = Math.abs(candidateCanvas.getX() - targetCanvas.getX());
			if (horizontalOffset < bestHorizontalOffset)
			{
				bestHorizontalOffset = horizontalOffset;
				bestSource = new WorldPoint(target.getX() + offset[0],
					target.getY() + offset[1], target.getPlane());
			}
		}
		return bestSource;
	}

	private static boolean isInsideDangerArea(LocalPoint player, LocalPoint target)
	{
		return target != null
			&& Math.abs(player.getX() - target.getX()) <= DANGER_RADIUS_TILES * TILE_SIZE
			&& Math.abs(player.getY() - target.getY()) <= DANGER_RADIUS_TILES * TILE_SIZE;
	}
}
