package com.chaosmod.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Locale;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.util.Text;

@Getter
final class GuitarHeroEvent implements ChaosMod
{
	private static final int READY_TICKS = 12;
	private static final int FIRST_NOTE_DELAY_TICKS = 3;
	private static final int NOTE_TRAVEL_TICKS = 1;
	private static final int NOTE_INTERVAL_TICKS = 1;
	private static final int TARGET_SCORE = 10;
	private static final int MAX_MISSES = 3;
	private static final int GAME_CYCLES_PER_TICK = 30;
	private static final int FEEDBACK_TICKS = 2;
	private static final long MOVEMENT_LOCK_NANOS = 5_000_000_000L;
	private static final int PRAYER_WIDGET_SEARCH_LIMIT = 100;
	private static final int NOTE_RADIUS = 13;
	private static final int NOTE_TRACK_LENGTH = 160;

	private static final PrayerTarget[] TARGETS =
	{
		new PrayerTarget(Prayer.PROTECT_FROM_MAGIC, "Protect from Magic", new Color(70, 170, 255)),
		new PrayerTarget(Prayer.PROTECT_FROM_MISSILES, "Protect from Missiles", new Color(80, 220, 100)),
		new PrayerTarget(Prayer.PROTECT_FROM_MELEE, "Protect from Melee", new Color(255, 90, 75))
	};

	private final String name = "Guitar Hero";
	private final String description = "Reach 10 notes; 3 misses locks movement for 5 seconds";
	private final int durationSeconds;
	private final Random random;

	private int readyUntilTick = -1;
	private int noteSpawnCycle = -1;
	private int noteHitTick = -1;
	private int feedbackUntilTick = -1;
	private int score;
	private int missedNotes;
	private Boolean lastHit;
	private PrayerTarget target;
	private long movementLockedUntilNanos;
	private boolean completed;
	private boolean failed;

	GuitarHeroEvent(int durationSeconds, Random random)
	{
		this.durationSeconds = durationSeconds;
		this.random = random;
	}

	@Override
	public void start(Client client)
	{
		stop(client);
		readyUntilTick = client.getTickCount() + READY_TICKS;
	}

	@Override
	public void onGameTick(Client client)
	{
		int tick = client.getTickCount();
		if (readyUntilTick < 0 || tick < readyUntilTick)
		{
			return;
		}

		if (completed)
		{
			return;
		}

		if (target == null)
		{
			scheduleNote(client, tick + FIRST_NOTE_DELAY_TICKS);
			return;
		}

		if (tick >= noteHitTick)
		{
			if (client.isPrayerActive(target.prayer))
			{
				score++;
				lastHit = true;
				feedbackUntilTick = tick + FEEDBACK_TICKS;
				client.playSoundEffect(1029, 100);
				if (score >= TARGET_SCORE)
				{
					completed = true;
					target = null;
					return;
				}
			}
			else
			{
				client.playSoundEffect(1031, 100);
				registerMiss(tick);
			}

			if (!completed)
			{
				scheduleNote(client, tick + NOTE_INTERVAL_TICKS - NOTE_TRAVEL_TICKS);
			}
		}
	}

	@Override
	public boolean isComplete()
	{
		return completed && (!failed || !isMovementLocked());
	}

	@Override
	public boolean canStop()
	{
		return !isMovementLocked();
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
	public void renderWidgets(Client client, Graphics2D graphics)
	{
		if (readyUntilTick < 0)
		{
			return;
		}

		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int tick = client.getTickCount();
			if (tick < readyUntilTick)
			{
				drawReadyCountdown(client, g, tick);
				return;
			}

			drawScore(client, g);
			if (failed)
			{
				drawFailure(client, g);
			}
			else if (lastHit != null && tick < feedbackUntilTick)
			{
				EventOverlayUtil.drawCentered(client, g, lastHit ? "PERFECT!" : "MISS!", 105,
					lastHit ? new Color(80, 255, 120) : new Color(255, 80, 80), 22f);
			}

			if (target == null)
			{
				return;
			}

			Widget prayerWidget = findPrayerWidget(client, target.widgetName);
			if (prayerWidget == null || prayerWidget.isHidden())
			{
				EventOverlayUtil.drawCentered(client, g, "Open your Prayer tab!", client.getCanvasHeight() / 2,
					Color.YELLOW, 22f);
				return;
			}

			Rectangle bounds = prayerWidget.getBounds();
			if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
			{
				return;
			}

			int targetX = bounds.x + bounds.width / 2;
			int targetY = bounds.y + bounds.height / 2;
			int startY = Math.max(NOTE_RADIUS + 4, targetY - NOTE_TRACK_LENGTH);
			double travelCycles = NOTE_TRAVEL_TICKS * (double) GAME_CYCLES_PER_TICK;
			double progress = Math.max(0.0, Math.min(1.0,
				(client.getGameCycle() - noteSpawnCycle) / travelCycles));
			int noteY = (int) Math.round(startY + (targetY - startY) * progress);

			g.setStroke(new BasicStroke(3f));
			g.setColor(new Color(target.color.getRed(), target.color.getGreen(), target.color.getBlue(), 80));
			g.drawLine(targetX, startY, targetX, targetY);
			g.setColor(new Color(0, 0, 0, 180));
			g.fillOval(targetX - NOTE_RADIUS - 3, noteY - NOTE_RADIUS - 3,
				(NOTE_RADIUS + 3) * 2, (NOTE_RADIUS + 3) * 2);
			g.setColor(target.color);
			g.fillOval(targetX - NOTE_RADIUS, noteY - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);
			g.setColor(Color.WHITE);
			g.drawOval(targetX - NOTE_RADIUS, noteY - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);

			g.setStroke(new BasicStroke(4f));
			g.setColor(target.color);
			g.drawOval(bounds.x - 3, bounds.y - 3, bounds.width + 6, bounds.height + 6);
		}
		finally
		{
			g.dispose();
		}
	}

	@Override
	public void stop(Client client)
	{
		readyUntilTick = -1;
		noteSpawnCycle = -1;
		noteHitTick = -1;
		feedbackUntilTick = -1;
		score = 0;
		missedNotes = 0;
		lastHit = null;
		target = null;
		movementLockedUntilNanos = 0L;
		completed = false;
		failed = false;
	}

	private void scheduleNote(Client client, int spawnTick)
	{
		PrayerTarget previous = target;
		do
		{
			target = TARGETS[random.nextInt(TARGETS.length)];
		}
		while (TARGETS.length > 1 && target == previous);

		noteSpawnCycle = client.getGameCycle()
			+ (spawnTick - client.getTickCount()) * GAME_CYCLES_PER_TICK;
		noteHitTick = spawnTick + NOTE_TRAVEL_TICKS;
	}

	private void drawReadyCountdown(Client client, Graphics2D g, int tick)
	{
		int ticksRemaining = readyUntilTick - tick;
		int seconds = Math.max(1, (ticksRemaining + 1) / 2);
		EventOverlayUtil.drawCentered(client, g, "Get ready for Guitar Hero!", client.getCanvasHeight() / 2 - 20,
			Color.WHITE, 26f);
		EventOverlayUtil.drawCentered(client, g, Integer.toString(seconds), client.getCanvasHeight() / 2 + 28,
			Color.YELLOW, 34f);
	}

	private void drawScore(Client client, Graphics2D g)
	{
		String text = String.format("Reach %d to end  •  %d misses = punishment  •  Score: %d / %d",
			TARGET_SCORE, MAX_MISSES, score, TARGET_SCORE);
		EventOverlayUtil.drawCentered(client, g, text, 76, Color.WHITE, 18f);
	}

	private void drawFailure(Client client, Graphics2D g)
	{
		long remainingMillis = Math.max(0L,
			(movementLockedUntilNanos - System.nanoTime()) / 1_000_000L);
		EventOverlayUtil.drawCentered(client, g,
			String.format("FAILED! MOVEMENT LOCKED: %.1fs", remainingMillis / 1000.0),
			105, Color.RED, 22f);
	}

	private void registerMiss(int tick)
	{
		missedNotes++;
		lastHit = false;
		feedbackUntilTick = tick + FEEDBACK_TICKS;
		if (missedNotes >= MAX_MISSES)
		{
			completed = true;
			failed = true;
			target = null;
			movementLockedUntilNanos = System.nanoTime() + MOVEMENT_LOCK_NANOS;
		}
	}

	private boolean isMovementLocked()
	{
		return System.nanoTime() < movementLockedUntilNanos;
	}

	private static Widget findPrayerWidget(Client client, String prayerName)
	{
		for (int child = 0; child < PRAYER_WIDGET_SEARCH_LIMIT; child++)
		{
			Widget match = findPrayerWidget(client.getWidget(InterfaceID.PRAYERBOOK, child), prayerName);
			if (match != null)
			{
				return match;
			}
		}
		return null;
	}

	private static Widget findPrayerWidget(Widget widget, String prayerName)
	{
		if (widget == null)
		{
			return null;
		}

		String rawName = widget.getName();
		String name = rawName == null ? null : Text.removeTags(rawName);
		if (name != null && name.toLowerCase(Locale.ROOT).contains(prayerName.toLowerCase(Locale.ROOT)))
		{
			return widget;
		}

		Widget match = findPrayerWidget(widget.getStaticChildren(), prayerName);
		if (match == null) match = findPrayerWidget(widget.getDynamicChildren(), prayerName);
		if (match == null) match = findPrayerWidget(widget.getNestedChildren(), prayerName);
		return match;
	}

	private static Widget findPrayerWidget(Widget[] children, String prayerName)
	{
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			Widget match = findPrayerWidget(child, prayerName);
			if (match != null)
			{
				return match;
			}
		}
		return null;
	}

	private static final class PrayerTarget
	{
		private final Prayer prayer;
		private final String widgetName;
		private final Color color;

		private PrayerTarget(Prayer prayer, String widgetName, Color color)
		{
			this.prayer = prayer;
			this.widgetName = widgetName;
			this.color = color;
		}
	}
}
