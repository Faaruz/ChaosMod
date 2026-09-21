package com.chaosmod.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOptionClicked;

/** A client-side recreation of Vardorvis' click-the-blood QTE. */
@Getter
final class VardorvisCaptchaEvent implements ChaosMod
{
	private static final int TARGET_COUNT = 3;
	private static final int TARGET_SIZE = 54;
	private static final int TARGET_PADDING = 18;
	private static final int CLUSTER_RADIUS = 95;
	private static final long WAVE_INTERVAL_NANOS = 5_000_000_000L;
	private static final long WAVE_DURATION_NANOS = 2_500_000_000L;
	private static final long PUNISHMENT_NANOS = 5_000_000_000L;

	private final String name = "Vardorvis Captcha";
	private final String description = "Clear the captcha or lose movement and prayer for 5 seconds";
	private final int durationSeconds;
	private final Random random;
	private final List<Rectangle> targets = new ArrayList<>();

	private long nextWaveNanos;
	private long waveDeadlineNanos;
	private long lockedUntilNanos;

	VardorvisCaptchaEvent(int durationSeconds, Random random)
	{
		this.durationSeconds = durationSeconds;
		this.random = random;
	}

	@Override
	public void start(Client client)
	{
		stop(client);
		nextWaveNanos = System.nanoTime() + WAVE_INTERVAL_NANOS;
	}

	@Override
	public void onClientTick(Client client)
	{
		long now = System.nanoTime();
		if (!targets.isEmpty() && now >= waveDeadlineNanos)
		{
			failWave(now);
			return;
		}

		if (targets.isEmpty() && !isLocked(now) && now >= nextWaveNanos)
		{
			spawnWave(client, now);
		}
	}

	@Override
	public void onMenuOptionClicked(Client client, MenuOptionClicked event)
	{
		long now = System.nanoTime();
		if (!targets.isEmpty())
		{
			Point mouse = client.getMouseCanvasPosition();
			if (mouse != null)
			{
				for (int i = targets.size() - 1; i >= 0; i--)
				{
					if (targets.get(i).contains(mouse.getX(), mouse.getY()))
					{
						event.consume();
						targets.remove(i);
						if (targets.isEmpty())
						{
							waveDeadlineNanos = 0L;
							nextWaveNanos = now + WAVE_INTERVAL_NANOS;
						}
						return;
					}
				}
			}

			if (EventInputUtil.isWorldInteractionClick(event.getMenuAction()))
			{
				event.consume();
				return;
			}
		}

		if (isLocked(now) && (EventInputUtil.isWalkClick(event)
				|| EventInputUtil.isPrayerActivationClick(event)))
		{
			event.consume();
		}
	}

	@Override
	public void renderWidgets(Client client, Graphics2D graphics)
	{
		long now = System.nanoTime();
		if (targets.isEmpty() && !isLocked(now))
		{
			return;
		}

		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (!targets.isEmpty())
			{
				drawCaptcha(client, g, now);
			}
			else
			{
				long remaining = Math.max(0L, lockedUntilNanos - now);
				EventOverlayUtil.drawCentered(client, g,
						String.format("STUNNED! MOVEMENT & PRAYER LOCKED: %.1fs", remaining / 1_000_000_000.0),
						200, new Color(255, 55, 55), 22f);
			}
		}
		finally
		{
			g.dispose();
		}
	}

	@Override
	public boolean canStop()
	{
		return !isLocked(System.nanoTime());
	}

	@Override
	public void stop(Client client)
	{
		targets.clear();
		nextWaveNanos = 0L;
		waveDeadlineNanos = 0L;
		lockedUntilNanos = 0L;
	}

	private void spawnWave(Client client, long now)
	{
		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		if (width < TARGET_SIZE + TARGET_PADDING * 2
				|| height < TARGET_SIZE + TARGET_PADDING * 2)
		{
			nextWaveNanos = now + 100_000_000L;
			return;
		}

		targets.clear();

		int halfSize = TARGET_SIZE / 2;
		int clusterMargin = CLUSTER_RADIUS + halfSize + TARGET_PADDING;
		int centerX = clusterMargin + random.nextInt(Math.max(1, width - clusterMargin * 2 + 1));
		int centerY = clusterMargin + random.nextInt(Math.max(1, height - clusterMargin * 2 + 1));

		for (int i = 0; i < TARGET_COUNT; i++)
		{
			Rectangle candidate = null;
			for (int attempts = 0; attempts < 80; attempts++)
			{
				int cx = centerX + random.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;
				int cy = centerY + random.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;
				int x = Math.max(TARGET_PADDING, Math.min(cx - halfSize, width - TARGET_SIZE - TARGET_PADDING));
				int y = Math.max(TARGET_PADDING, Math.min(cy - halfSize, height - TARGET_SIZE - TARGET_PADDING));
				Rectangle attempt = new Rectangle(x, y, TARGET_SIZE, TARGET_SIZE);
				if (!overlapsExisting(attempt))
				{
					candidate = attempt;
					break;
				}
			}

			if (candidate != null)
			{
				targets.add(candidate);
			}
		}

		if (targets.size() != TARGET_COUNT)
		{
			targets.clear();
			nextWaveNanos = now + 100_000_000L;
			return;
		}

		waveDeadlineNanos = now + WAVE_DURATION_NANOS;
		nextWaveNanos = Long.MAX_VALUE;
	}

	private boolean overlapsExisting(Rectangle candidate)
	{
		Rectangle padded = new Rectangle(candidate);
		padded.grow(TARGET_PADDING, TARGET_PADDING);
		for (Rectangle target : targets)
		{
			if (padded.intersects(target))
			{
				return true;
			}
		}
		return false;
	}

	private void failWave(long now)
	{
		targets.clear();
		waveDeadlineNanos = 0L;
		lockedUntilNanos = now + PUNISHMENT_NANOS;
		nextWaveNanos = lockedUntilNanos + WAVE_INTERVAL_NANOS;
	}

	private void drawCaptcha(Client client, Graphics2D g, long now)
	{
		g.setColor(new Color(35, 0, 0, 95));
		g.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());

		EventOverlayUtil.drawCentered(client, g, "VARDORVIS CAPTCHA", 55,
				new Color(255, 90, 90), 20f);
		long remaining = Math.max(0L, waveDeadlineNanos - now);
		EventOverlayUtil.drawCentered(client, g, String.format("%.1fs", remaining / 1_000_000_000.0),
				80, Color.WHITE, 17f);

		int barWidth = Math.min(320, client.getCanvasWidth() - 40);
		int barX = (client.getCanvasWidth() - barWidth) / 2;
		double progress = Math.max(0.0, Math.min(1.0, remaining / (double) WAVE_DURATION_NANOS));
		g.setColor(new Color(20, 20, 20, 210));
		g.fillRect(barX, 88, barWidth, 8);
		g.setColor(new Color(185, 18, 26));
		g.fillRect(barX, 88, (int) Math.round(barWidth * progress), 8);

		for (Rectangle target : targets)
		{
			drawBloodGrowth(g, target);
		}
	}

	private static void drawBloodGrowth(Graphics2D g, Rectangle target)
	{
		int cx = target.x + target.width / 2;
		int cy = target.y + target.height / 2;
		int radius = target.width / 2;

		g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(35, 0, 0, 220));
		g.fillOval(target.x - 4, target.y - 4, target.width + 8, target.height + 8);
		g.setColor(new Color(130, 0, 10));
		for (int i = 0; i < 8; i++)
		{
			double angle = i * Math.PI / 4.0;
			int innerX = cx + (int) Math.round(Math.cos(angle) * (radius - 8));
			int innerY = cy + (int) Math.round(Math.sin(angle) * (radius - 8));
			int outerX = cx + (int) Math.round(Math.cos(angle) * (radius + 8));
			int outerY = cy + (int) Math.round(Math.sin(angle) * (radius + 8));
			g.drawLine(innerX, innerY, outerX, outerY);
		}
		g.setColor(new Color(215, 20, 30));
		g.fillOval(target.x + 5, target.y + 5, target.width - 10, target.height - 10);
		g.setColor(new Color(255, 95, 95));
		g.fillOval(cx - 8, cy - 10, 12, 12);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(2f));
		g.drawOval(target.x + 2, target.y + 2, target.width - 4, target.height - 4);
	}

	private boolean isLocked(long now)
	{
		return now < lockedUntilNanos;
	}

}
