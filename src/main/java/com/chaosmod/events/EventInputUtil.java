package com.chaosmod.events;

import java.util.Locale;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;

final class EventInputUtil
{
	private EventInputUtil() { }

	static boolean isWalkClick(MenuOptionClicked event)
	{
		return event.getMenuAction() == MenuAction.WALK;
	}

	static boolean isPrayerActivationClick(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if (option == null || !"activate".equals(option.toLowerCase(Locale.ROOT)))
		{
			return false;
		}
		Widget widget = event.getWidget();
		if (widget != null && (widget.getId() >>> 16) == WidgetID.PRAYER_GROUP_ID)
		{
			return true;
		}
		String target = event.getMenuTarget();
		return target != null && target.toLowerCase(Locale.ROOT).contains("prayer");
	}

	static boolean isInventoryClick(MenuOptionClicked event)
	{
		Widget widget = event.getWidget();
		if (widget != null && (widget.getId() >>> 16) == WidgetID.INVENTORY_GROUP_ID)
		{
			return true;
		}
		return event.getMenuAction() == MenuAction.WIDGET_TARGET
			&& (event.getParam1() >>> 16) == WidgetID.INVENTORY_GROUP_ID;
	}

	// whatever the player can interact on the world map should be blocked here
	static boolean isWorldInteractionClick(MenuAction action)
	{
		switch (action)
		{
			case WALK: case GAME_OBJECT_FIRST_OPTION: case GAME_OBJECT_SECOND_OPTION: case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION: case GAME_OBJECT_FIFTH_OPTION: case NPC_FIRST_OPTION: case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION: case NPC_FOURTH_OPTION: case NPC_FIFTH_OPTION: case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION: case PLAYER_THIRD_OPTION: case PLAYER_FOURTH_OPTION: case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION: case PLAYER_SEVENTH_OPTION: case PLAYER_EIGHTH_OPTION: case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION: case GROUND_ITEM_THIRD_OPTION: case GROUND_ITEM_FOURTH_OPTION: case GROUND_ITEM_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GAME_OBJECT: case WIDGET_TARGET_ON_NPC: case WIDGET_TARGET_ON_PLAYER:
			case WIDGET_TARGET_ON_GROUND_ITEM: case WORLD_ENTITY_FIRST_OPTION: case WORLD_ENTITY_SECOND_OPTION:
			case WORLD_ENTITY_THIRD_OPTION: case WORLD_ENTITY_FOURTH_OPTION: case WORLD_ENTITY_FIFTH_OPTION:
			case EXAMINE_OBJECT: case EXAMINE_NPC: case EXAMINE_ITEM_GROUND: case EXAMINE_WORLD_ENTITY:
				return true;
			default:
				return false;
		}
	}
}
