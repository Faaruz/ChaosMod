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

/** Draws event effects over the 3D scene but underneath all game widgets. */
final class ChaosModEffectOverlay extends Overlay
{
	private final Client client;
	private final ChaosModPlugin plugin;
	private final ChaosModConfig config;

	@Inject
	ChaosModEffectOverlay(Client client, ChaosModPlugin plugin, ChaosModConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		ChaosMod activeEvent = plugin.getActiveEvent();
		if (activeEvent != null)
		{
			activeEvent.render(client, graphics);
			if (config.showEventTileIndicators())
			{
				activeEvent.renderTileMarkers(client, graphics);
			}
		}
		return null;
	}
}
