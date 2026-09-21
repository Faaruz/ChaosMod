package com.chaosmod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;

import com.chaosmod.events.ChaosMod;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class ChaosModOverlay extends Overlay
{
	private static final Color COUNTDOWN_TITLE_COLOR = new Color(0x55D9FF);
	private static final Color ACTIVE_TITLE_COLOR = new Color(0xFFB347);
	private final ChaosModPlugin plugin;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	ChaosModOverlay(ChaosModPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
		panel.setPreferredSize(new Dimension(310, 0));
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		panel.getChildren().clear();
		long seconds = Math.max(0, Duration.between(Instant.now(), plugin.getDeadline()).getSeconds() + 1);
		if (plugin.getPhase() == ChaosModPlugin.Phase.ACTIVE && plugin.getActiveEvent() != null)
		{
			ChaosMod activeEvent = plugin.getActiveEvent();
			panel.getChildren().add(TitleComponent.builder().text("EVENT ACTIVE  •  " + seconds + "s").color(ACTIVE_TITLE_COLOR).build());
			panel.getChildren().add(LineComponent.builder().left(activeEvent.getName()).build());
			panel.getChildren().add(LineComponent.builder().left(activeEvent.getDescription()).leftColor(Color.LIGHT_GRAY).build());
		}
		else
		{
			panel.getChildren().add(TitleComponent.builder().text("NEXT RANDOM EVENT  •  " + seconds + "s").color(COUNTDOWN_TITLE_COLOR).build());
		}
		return panel.render(graphics);
	}
}
