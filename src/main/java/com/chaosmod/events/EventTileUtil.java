package com.chaosmod.events;

import net.runelite.api.coords.LocalPoint;

final class EventTileUtil
{
	private static final int TILE_SIZE = 128;
	private static final int TILE_CENTER_OFFSET = TILE_SIZE / 2;

	private EventTileUtil() { }

	static LocalPoint snapToTileCenter(LocalPoint point)
	{
		if (point == null)
		{
			return null;
		}

		int centeredX = (point.getX() / TILE_SIZE) * TILE_SIZE + TILE_CENTER_OFFSET;
		int centeredY = (point.getY() / TILE_SIZE) * TILE_SIZE + TILE_CENTER_OFFSET;
		return new LocalPoint(centeredX, centeredY, point.getWorldView());
	}
}
