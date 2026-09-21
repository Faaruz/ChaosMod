package com.chaosmod.events;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import com.chaosmod.ChaosModConfig;

public final class ChaosModFactory
{
	private ChaosModFactory() { }

	public static List<ChaosMod> createPool(Random random, ChaosModConfig config)
	{
		return Arrays.asList(
			new ExplodingBarrelsEvent(duration(config, config.barrelsSeconds()), random),
			new LightsOutEvent(duration(config, config.lightsOutSeconds())),
			new SpawnJadEvent(duration(config, config.jadSeconds()), random),
			new YamaMeteorShowerEvent(duration(config, config.meteorSeconds())),
			new NoobOutfitEvent(duration(config, config.noobOutfitSeconds())),
			new GiantPlayerEvent(duration(config, config.giantPlayerSeconds())),
			new GuitarHeroEvent(duration(config, config.guitarHeroSeconds()), random),
			new VardorvisCaptchaEvent(duration(config, config.vardorvisSeconds()), random),
			new DirtyScreenEvent(duration(config, config.dirtyScreenSeconds()), random),
			new CoxPortalsEvent(duration(config, config.coxPortalsSeconds()), random),
			new LeviathanBouldersEvent(duration(config, config.leviathanSeconds()))
		);
	}

	private static int duration(ChaosModConfig config, int overrideSeconds)
	{
		return overrideSeconds > 0 ? overrideSeconds : config.eventSeconds();
	}
}

