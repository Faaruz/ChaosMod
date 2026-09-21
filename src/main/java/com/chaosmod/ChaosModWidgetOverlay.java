package com.chaosmod;

import com.chaosmod.events.ChaosMod;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/** Draws the event effects that intentionally need to appear over game widgets. */
final class ChaosModWidgetOverlay extends Overlay
{
	private final Client client;
	private final ChaosModPlugin plugin;

	@Inject
	ChaosModWidgetOverlay(Client client, ChaosModPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		ChaosMod activeEvent = plugin.getActiveEvent();
		if (activeEvent != null)
		{
			activeEvent.renderWidgets(client, graphics);
		}
		return null;
	}
}
