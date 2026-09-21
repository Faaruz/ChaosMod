package com.chaosmod;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("stream-events")
public interface ChaosModConfig extends Config
{
	@ConfigItem(keyName = "autoStart", name = "Enable random events", description = "Automatically start a random event after each countdown")
	default boolean autoStart() { return true; }

	@Range(min = 0, max = 120)
	@ConfigItem(keyName = "betweenRoundsSeconds", name = "Time until next event", description = "Seconds to wait before the next random event")
	default int betweenRoundsSeconds() { return 10; }

	@Range(min = 5, max = 180)
	@ConfigItem(keyName = "eventSeconds", name = "Default event duration", description = "Used unless that event has its own duration below")
	default int eventSeconds() { return 30; }

	@ConfigItem(keyName = "showEventTileIndicators", name = "Show event tile indicators", description = "Show optional danger and target tiles for events")
	default boolean showEventTileIndicators() { return false; }

	@ConfigSection(name = "Event duration overrides", description = "Set an event to 0 to use the default event duration", position = 10)
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

}
