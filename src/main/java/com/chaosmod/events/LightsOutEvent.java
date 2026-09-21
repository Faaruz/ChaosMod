package com.chaosmod.events;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;

@Getter
final class LightsOutEvent implements ChaosMod
{
	private static final int VISIBLE_RADIUS = 125;

	private final String name = "Lights Out";
	private final String description = "Only the area around your character remains visible";
	private final int durationSeconds;

	LightsOutEvent(int durationSeconds)
	{
		this.durationSeconds = durationSeconds;
	}

	@Override public void start(Client client) { }

	@Override public void stop(Client client) { }

	@Override public void render(Client client, Graphics2D graphics)
	{
		Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
			client.getViewportWidth(), client.getViewportHeight());
		Path2D darkness = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		darkness.append(viewport, false);
		Player player = client.getLocalPlayer();
		if (player != null && player.getLocalLocation() != null)
		{
			net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client,
				player.getLocalLocation(), client.getPlane(), player.getLogicalHeight() / 2);
			if (canvasPoint != null)
			{
				darkness.append(new Ellipse2D.Double(
					canvasPoint.getX() - VISIBLE_RADIUS,
					canvasPoint.getY() - VISIBLE_RADIUS,
					VISIBLE_RADIUS * 2.0,
					VISIBLE_RADIUS * 2.0), false);
			}
		}

		Graphics2D darknessGraphics = (Graphics2D) graphics.create();
		try
		{
			darknessGraphics.clip(viewport);
			darknessGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			darknessGraphics.setColor(Color.BLACK);
			darknessGraphics.fill(darkness);
		}
		finally
		{
			darknessGraphics.dispose();
		}
	}
}
