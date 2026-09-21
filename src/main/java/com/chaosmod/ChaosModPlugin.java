package com.chaosmod;

import com.google.inject.Provides;
import com.chaosmod.events.ChaosMod;
import com.chaosmod.events.ChaosModFactory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(name = "Chaos Mod", description = "Random client-side events for livestreams",
	tags = {"stream", "events", "random"}, internalName = "chaos-mod")
public class ChaosModPlugin extends Plugin
{
	enum Phase { WAITING, ACTIVE }

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private ChaosModConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private ChaosModOverlay overlay;
	@Inject private ChaosModEffectOverlay eventEffectOverlay;
	@Inject private ChaosModWidgetOverlay eventWidgetOverlay;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ChaosModPanel panel;
	@Inject private ScheduledExecutorService executor;

	private final Random random = new Random();
	@Getter private Phase phase = Phase.WAITING;
	@Getter private Instant deadline = Instant.EPOCH;
	@Getter private ChaosMod activeEvent;
	private ScheduledFuture<?> transitionTask;
	private NavigationButton navigationButton;

	@Override protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(eventEffectOverlay);
		overlayManager.add(eventWidgetOverlay);
		panel.rebuildEventButtons();
		navigationButton = NavigationButton.builder()
			.tooltip("Chaos Mod")
			.icon(createSidebarIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		if (config.autoStart()) scheduleNewRound(1);
		log.info("Chaos Mod started");
	}

	@Override protected void shutDown()
	{
		cancelTransition();
		overlayManager.remove(overlay);
		overlayManager.remove(eventEffectOverlay);
		overlayManager.remove(eventWidgetOverlay);
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		ChaosMod eventToStop = activeEvent;
		activeEvent = null;
		phase = Phase.WAITING;
		deadline = Instant.EPOCH;
		if (eventToStop != null)
		{
			clientThread.invokeLater(() -> eventToStop.stop(client));
		}
		log.info("Chaos Mod stopped");
	}

	@Subscribe public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && phase == Phase.WAITING && config.autoStart()) scheduleNewRound(1);
	}

	@Subscribe public void onConfigChanged(ConfigChanged event)
	{
		if (!ChaosModConfig.GROUP.equals(event.getGroup()) || !"autoStart".equals(event.getKey()))
		{
			return;
		}

		if (!config.autoStart())
		{
			if (phase == Phase.WAITING)
			{
				cancelTransition();
				deadline = Instant.EPOCH;
			}
			return;
		}

		clientThread.invoke(() ->
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage("<col=ff0000>Chaos Mod warning:</col> Random events can cause deaths. Hardcore Ironmen should enable them only if they accept that risk.")
				.build());
			if (phase == Phase.WAITING)
			{
				scheduleNewRound(1);
			}
		});
	}

	@Subscribe public void onGameTick(GameTick tick)
	{
		if (activeEvent != null)
		{
			activeEvent.onGameTick(client);
			if (activeEvent != null && activeEvent.isComplete())
			{
				finishEvent();
			}
		}
	}

	@Subscribe public void onClientTick(ClientTick tick)
	{
		if (activeEvent != null) activeEvent.onClientTick(client);
	}

	@Subscribe public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (activeEvent != null) activeEvent.onMenuOptionClicked(client, event);
	}

	@Subscribe public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (activeEvent != null) activeEvent.onScriptCallbackEvent(client, event);
	}

	void startRound() { clientThread.invoke(this::startRandomEvent); }

	List<String> getAvailableEventNames()
	{
		List<String> names = new ArrayList<>();
		for (ChaosMod event : ChaosModFactory.createAllEvents(random, config))
		{
			names.add(event.getName());
		}
		return names;
	}

	void startTestEvent(String eventName)
	{
		clientThread.invoke(() ->
		{
			ChaosMod selected = null;
			for (ChaosMod event : ChaosModFactory.createAllEvents(random, config))
			{
				if (event.getName().equals(eventName))
				{
					selected = event;
					break;
				}
			}

			if (selected == null)
			{
				return;
			}

			cancelTransition();
			if (activeEvent != null)
			{
				activeEvent.stop(client);
			}
			activeEvent = selected;
			phase = Phase.ACTIVE;
			deadline = Instant.now().plusSeconds(activeEvent.getDurationSeconds());
			activeEvent.start(client);

			transitionTask = executor.schedule(
				() -> clientThread.invoke(this::finishEvent),
				activeEvent.getDurationSeconds(), TimeUnit.SECONDS);
		});
	}

	void stopTestEvent()
	{
		clientThread.invoke(this::forceFinishEvent);
	}

	private void startRandomEvent()
	{
		cancelTransition();
		if (!config.autoStart())
		{
			phase = Phase.WAITING;
			deadline = Instant.EPOCH;
			return;
		}
		if (activeEvent != null) activeEvent.stop(client);
		List<ChaosMod> pool = ChaosModFactory.createRandomPool(random, config);
		if (pool.isEmpty()) return;
		activeEvent = pool.get(random.nextInt(pool.size()));
		phase = Phase.ACTIVE; deadline = Instant.now().plusSeconds(activeEvent.getDurationSeconds()); activeEvent.start(client);
		transitionTask = executor.schedule(() -> clientThread.invoke(this::finishEvent), activeEvent.getDurationSeconds(), TimeUnit.SECONDS);
	}

	private void finishEvent()
	{
		finishEventInternal(false);
	}

	private void forceFinishEvent()
	{
		finishEventInternal(true);
	}

	private void finishEventInternal(boolean force)
	{
		cancelTransition();
		if (!force && activeEvent != null && !activeEvent.canStop())
		{
			transitionTask = executor.schedule(
				() -> clientThread.invoke(this::finishEvent), 100, TimeUnit.MILLISECONDS);
			return;
		}
		if (activeEvent != null) activeEvent.stop(client);
		activeEvent = null; phase = Phase.WAITING; deadline = Instant.EPOCH;
		if (config.autoStart()) scheduleNewRound(config.betweenRoundsSeconds());
	}

	private void scheduleNewRound(int seconds)
	{
		cancelTransition();
		int delay = Math.max(0, seconds);
		phase = Phase.WAITING;
		deadline = Instant.now().plusSeconds(delay);
		transitionTask = executor.schedule(this::startRound, delay, TimeUnit.SECONDS);
	}

	private void cancelTransition()
	{
		if (transitionTask != null) transitionTask.cancel(false);
		transitionTask = null;
	}

	private static BufferedImage createSidebarIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(35, 155, 205));
			graphics.fillOval(1, 1, 14, 14);
			graphics.setColor(new Color(255, 190, 45));
			graphics.fill(new Polygon(
				new int[] {9, 5, 7, 4, 10, 8},
				new int[] {2, 8, 8, 14, 7, 7}, 6));
		}
		finally
		{
			graphics.dispose();
		}
		return icon;
	}

	@Provides
	ChaosModConfig provideConfig(ConfigManager manager) { return manager.getConfig(ChaosModConfig.class); }
}
