package com.chaosmod;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("stream-events")
public interface ChaosModConfig extends Config
{
	String GROUP = "stream-events";

	@ConfigItem(keyName = "autoStart", name = "Enable random events", description = "Warning: events can cause deaths. Hardcore Ironmen should enable this only if they accept that risk.")
	default boolean autoStart() { return false; }

	@ConfigItem(keyName = "showSidebarPanel", name = "Show sidebar panel", description = "Show the Chaos Mod event tester in RuneLite's sidebar")
	default boolean showSidebarPanel() { return false; }

	@Range(min = 0, max = 120)
	@ConfigItem(keyName = "betweenRoundsSeconds", name = "Time until next event", description = "Seconds to wait before the next random event")
	default int betweenRoundsSeconds() { return 60; }

	@Range(min = 5, max = 180)
	@ConfigItem(keyName = "eventSeconds", name = "Default event duration", description = "Used unless that event has its own duration below")
	default int eventSeconds() { return 30; }

	@ConfigItem(keyName = "showEventTileIndicators", name = "Show event tile indicators", description = "Show optional danger and target tiles for events")
	default boolean showEventTileIndicators() { return true; }

	@ConfigSection(name = "Event duration overrides", description = "Set an event to 0 to use the default event duration", closedByDefault = true, position = 10)
	String eventDurationOverrides = "eventDurationOverrides";

	@Range(min = 0, max = 180) @ConfigItem(keyName = "barrelsSeconds", name = "Exploding Barrels", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int barrelsSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "lightsOutSeconds", name = "Lights Out", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int lightsOutSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "jadSeconds", name = "Spawn Jad", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int jadSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "meteorSeconds", name = "Yama Meteor Shower", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int meteorSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "noobOutfitSeconds", name = "Noob Outfit", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int noobOutfitSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "giantPlayerSeconds", name = "Giant Player", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int giantPlayerSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "guitarHeroSeconds", name = "Guitar Hero", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int guitarHeroSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "vardorvisSeconds", name = "Vardorvis Captcha", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int vardorvisSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "dirtyScreenSeconds", name = "Dirty Screen", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int dirtyScreenSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "coxPortalsSeconds", name = "CoX Portals", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int coxPortalsSeconds() { return 0; }
	@Range(min = 0, max = 180) @ConfigItem(keyName = "leviathanSeconds", name = "Leviathan Boulders", description = "0 uses Default event duration", section = eventDurationOverrides)
	default int leviathanSeconds() { return 0; }

	@ConfigSection(name = "Random event pool", description = "Choose which events can be selected by the random timer", closedByDefault = true, position = 20)
	String randomEventPool = "randomEventPool";

	@ConfigItem(keyName = "includeBarrels", name = "Exploding Barrels", description = "Include Exploding Barrels in random events", section = randomEventPool)
	default boolean includeBarrels() { return true; }
	@ConfigItem(keyName = "includeLightsOut", name = "Lights Out", description = "Include Lights Out in random events", section = randomEventPool)
	default boolean includeLightsOut() { return true; }
	@ConfigItem(keyName = "includeJad", name = "Spawn Jad", description = "Include Spawn Jad in random events", section = randomEventPool)
	default boolean includeJad() { return true; }
	@ConfigItem(keyName = "includeMeteor", name = "Yama Meteor Shower", description = "Include Yama Meteor Shower in random events", section = randomEventPool)
	default boolean includeMeteor() { return true; }
	@ConfigItem(keyName = "includeNoobOutfit", name = "Noob Outfit", description = "Include Noob Outfit in random events", section = randomEventPool)
	default boolean includeNoobOutfit() { return true; }
	@ConfigItem(keyName = "includeGiantPlayer", name = "Giant Player", description = "Include Giant Player in random events", section = randomEventPool)
	default boolean includeGiantPlayer() { return true; }
	@ConfigItem(keyName = "includeGuitarHero", name = "Guitar Hero", description = "Include Guitar Hero in random events", section = randomEventPool)
	default boolean includeGuitarHero() { return true; }
	@ConfigItem(keyName = "includeVardorvis", name = "Vardorvis Captcha", description = "Include Vardorvis Captcha in random events", section = randomEventPool)
	default boolean includeVardorvis() { return true; }
	@ConfigItem(keyName = "includeDirtyScreen", name = "Dirty Screen", description = "Include Dirty Screen in random events", section = randomEventPool)
	default boolean includeDirtyScreen() { return true; }
	@ConfigItem(keyName = "includeCoxPortals", name = "CoX Portals", description = "Include CoX Portals in random events", section = randomEventPool)
	default boolean includeCoxPortals() { return true; }
	@ConfigItem(keyName = "includeLeviathan", name = "Leviathan Boulders", description = "Include Leviathan Boulders in random events", section = randomEventPool)
	default boolean includeLeviathan() { return true; }

}
