package com.chaosmod.events;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;

@Getter
final class DirtyScreenEvent implements ChaosMod
{
	private static final int CLEAN_RADIUS = 4;
	private static final int MUD_BLOTCH_COUNT = 260;

	private final String name = "Dirty Screen";
	private final String description = "Clean the mud by moving your mouse over the screen";
	private final int durationSeconds;
	private final Random random;

	private BufferedImage mud;
	private Point lastMouse;
	private int canvasWidth;
	private int canvasHeight;

	DirtyScreenEvent(int durationSeconds, Random random)
	{
		this.durationSeconds = durationSeconds;
		this.random = random;
	}

	@Override
	public synchronized void start(Client client)
	{
		stop(client);
		createMud(client.getCanvasWidth(), client.getCanvasHeight());
	}

	@Override
	public synchronized void onClientTick(Client client)
	{
		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		if (mud == null || width != canvasWidth || height != canvasHeight)
		{
			createMud(width, height);
			if (mud == null)
			{
				return;
			}
		}

		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || mouse.getX() < 0 || mouse.getY() < 0
			|| mouse.getX() >= canvasWidth || mouse.getY() >= canvasHeight)
		{
			lastMouse = null;
			return;
		}

		Graphics2D g = mud.createGraphics();
		try
		{
			g.setComposite(AlphaComposite.Clear);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setStroke(new BasicStroke(CLEAN_RADIUS * 2f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
			if (lastMouse == null)
			{
				g.fillOval(mouse.getX() - CLEAN_RADIUS, mouse.getY() - CLEAN_RADIUS,
					CLEAN_RADIUS * 2, CLEAN_RADIUS * 2);
			}
			else
			{
				g.drawLine(lastMouse.getX(), lastMouse.getY(), mouse.getX(), mouse.getY());
			}
		}
		finally
		{
			g.dispose();
		}
		lastMouse = mouse;
	}

	@Override
	public synchronized void renderWidgets(Client client, Graphics2D graphics)
	{
		if (mud != null)
		{
			graphics.drawImage(mud, 0, 0, null);
		}
	}

	@Override
	public synchronized void stop(Client client)
	{
		mud = null;
		lastMouse = null;
		canvasWidth = 0;
		canvasHeight = 0;
	}

	private void createMud(int width, int height)
	{
		if (width <= 0 || height <= 0)
		{
			mud = null;
			lastMouse = null;
			return;
		}

		canvasWidth = width;
		canvasHeight = height;
		lastMouse = null;
		mud = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mud.createGraphics();
		try
		{
			g.setColor(new Color(70, 43, 20, 252));
			g.fillRect(0, 0, width, height);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Color[] colors =
			{
				new Color(45, 28, 13, 235),
				new Color(88, 55, 25, 245),
				new Color(112, 73, 32, 225),
				new Color(58, 37, 17, 240)
			};
			for (int i = 0; i < MUD_BLOTCH_COUNT; i++)
			{
				int blotchWidth = 24 + random.nextInt(100);
				int blotchHeight = 16 + random.nextInt(70);
				int x = random.nextInt(width + blotchWidth) - blotchWidth;
				int y = random.nextInt(height + blotchHeight) - blotchHeight;
				g.setColor(colors[random.nextInt(colors.length)]);
				g.fillOval(x, y, blotchWidth, blotchHeight);
			}
		}
		finally
		{
			g.dispose();
		}
	}
}
