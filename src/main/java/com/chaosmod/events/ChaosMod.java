package com.chaosmod.events;

import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;

public interface ChaosMod
{
	String getName();
	String getDescription();
	int getDurationSeconds();
	void start(Client client);
	void stop(Client client);

	default void onGameTick(Client client) { }
	default void onClientTick(Client client) { }
	default void onMenuOptionClicked(Client client, MenuOptionClicked event) { }
	default void onScriptCallbackEvent(Client client, ScriptCallbackEvent event) { }
	default void render(Client client, Graphics2D graphics) { }
	default void renderTileMarkers(Client client, Graphics2D graphics) { }
	default void renderWidgets(Client client, Graphics2D graphics) { }
	default boolean isComplete() { return false; }
	default boolean canStop() { return true; }
}
