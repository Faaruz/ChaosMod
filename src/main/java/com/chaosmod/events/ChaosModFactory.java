package com.chaosmod.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import com.chaosmod.ChaosModConfig;

public final class ChaosModFactory
{
	private ChaosModFactory() { }

	public static List<ChaosMod> createAllEvents(Random random, ChaosModConfig config)
	{
		return createEvents(random, config, true);
	}

	public static List<ChaosMod> createRandomPool(Random random, ChaosModConfig config)
	{
		return createEvents(random, config, false);
	}

	private static List<ChaosMod> createEvents(Random random, ChaosModConfig config,
		boolean includeDisabledEvents)
	{
		List<ChaosMod> events = new ArrayList<>();
		add(events, includeDisabledEvents || config.includeBarrels(), () -> new ExplodingBarrelsEvent(duration(config, config.barrelsSeconds()), random));
		add(events, includeDisabledEvents || config.includeLightsOut(), () -> new LightsOutEvent(duration(config, config.lightsOutSeconds())));
		add(events, includeDisabledEvents || config.includeJad(), () -> new SpawnJadEvent(duration(config, config.jadSeconds()), random));
		add(events, includeDisabledEvents || config.includeMeteor(), () -> new YamaMeteorShowerEvent(duration(config, config.meteorSeconds())));
		add(events, includeDisabledEvents || config.includeNoobOutfit(), () -> new NoobOutfitEvent(duration(config, config.noobOutfitSeconds())));
		add(events, includeDisabledEvents || config.includeGiantPlayer(), () -> new GiantPlayerEvent(duration(config, config.giantPlayerSeconds())));
		add(events, includeDisabledEvents || config.includeGuitarHero(), () -> new GuitarHeroEvent(duration(config, config.guitarHeroSeconds()), random));
		add(events, includeDisabledEvents || config.includeVardorvis(), () -> new VardorvisCaptchaEvent(duration(config, config.vardorvisSeconds()), random));
		add(events, includeDisabledEvents || config.includeDirtyScreen(), () -> new DirtyScreenEvent(duration(config, config.dirtyScreenSeconds()), random));
		add(events, includeDisabledEvents || config.includeCoxPortals(), () -> new CoxPortalsEvent(duration(config, config.coxPortalsSeconds()), random));
		add(events, includeDisabledEvents || config.includeLeviathan(), () -> new LeviathanBouldersEvent(duration(config, config.leviathanSeconds())));
		return events;
	}

	private static void add(List<ChaosMod> events, boolean enabled, Supplier<ChaosMod> event)
	{
		if (enabled)
		{
			events.add(event.get());
		}
	}

	private static int duration(ChaosModConfig config, int overrideSeconds)
	{
		return overrideSeconds > 0 ? overrideSeconds : config.eventSeconds();
	}
}
