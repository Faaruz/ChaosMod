package com.chaosmod.events;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import net.runelite.api.Client;

final class EventOverlayUtil
{
	private EventOverlayUtil()
	{
	}

	static void drawCentered(Client client, Graphics2D graphics, String text, int y,
		Color color, float fontSize)
	{
		graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, fontSize));
		int x = (client.getCanvasWidth() - graphics.getFontMetrics().stringWidth(text)) / 2;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 2, y + 2);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}
}
