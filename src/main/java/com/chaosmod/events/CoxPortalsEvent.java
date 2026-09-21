package com.chaosmod.events;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;

/**
 * Olm-style portal challenge.
 *
 * A portal is placed on a collision-reachable tile no more than five tiles
 * from the player. The player has three game ticks to stand on it. Missing a
 * portal blocks inventory interactions for ten seconds. Portal spawning is
 * paused for the entire punishment.
 */
@Getter
final class CoxPortalsEvent implements ChaosMod
{
    private static final int MAX_PORTAL_DISTANCE = 5;
    private static final int COUNTDOWN_TICKS = 5;
    private static final long PORTAL_DELAY_NANOS = 5_000_000_000L;
    private static final long INVENTORY_LOCK_NANOS = 10_000_000_000L;

    // Apparently the portal is a NPC.
    private static final int PORTAL_NPC_ID = 3086;
    private static final int PORTAL_ANIMATION = 504;
    private final String name = "CoX Portals";
    private final String description = "Reach Olm's portal before the 3-tick countdown ends";
    private final int durationSeconds;
    private final Random random;

    private LocalPoint portalLocation;
    private int portalEndTick = -1;
    private long nextPortalNanos;
    private long inventoryLockedUntilNanos;
    private RuneLiteObject portalObject;

    CoxPortalsEvent(int durationSeconds, Random random)
    {
        this.durationSeconds = durationSeconds;
        this.random = random;
    }

    @Override
    public void start(Client client)
    {
        stop(client);
        nextPortalNanos = System.nanoTime();
    }

    @Override
    public void onClientTick(Client client)
    {
        if (portalLocation != null)
        {
            updatePortalVisual(client);
            return;
        }

        if (isInventoryLocked())
        {
            return;
        }

        if (System.nanoTime() >= nextPortalNanos)
        {
            spawnPortal(client);
        }
    }

    @Override
    public void onGameTick(Client client)
    {
        if (portalLocation == null)
        {
            return;
        }

        int tick = client.getTickCount();
        if (tick < portalEndTick)
        {
            return;
        }

        Player player = client.getLocalPlayer();
        boolean success = player != null
                && player.getLocalLocation() != null
                && isSameTile(player.getLocalLocation(), portalLocation);

        destroyPortalVisual();
        portalLocation = null;
        portalEndTick = -1;

        if (!success)
        {
            inventoryLockedUntilNanos = System.nanoTime() + INVENTORY_LOCK_NANOS;
            nextPortalNanos = inventoryLockedUntilNanos + PORTAL_DELAY_NANOS;
        }
        else
        {
            nextPortalNanos = System.nanoTime() + PORTAL_DELAY_NANOS;
        }
    }

    @Override
    public void onMenuOptionClicked(Client client, MenuOptionClicked event)
    {
		if (isInventoryLocked() && EventInputUtil.isInventoryClick(event))
        {
            event.consume();
        }
    }

    @Override
    public boolean canStop()
    {
        return !isInventoryLocked();
    }

    @Override
    public void render(Client client, Graphics2D graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (portalLocation != null)
            {
                drawPortal(client, g);
            }

            if (isInventoryLocked())
            {
                long remainingMillis = Math.max(0L,
                        (inventoryLockedUntilNanos - System.nanoTime()) / 1_000_000L);
                EventOverlayUtil.drawCentered(client, g,
                        String.format("INVENTORY LOCKED: %.1fs", remainingMillis / 1000.0),
                        200, Color.RED, 20f);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    @Override
    public void stop(Client client)
    {
        destroyPortalVisual();
        portalLocation = null;
        portalEndTick = -1;
        nextPortalNanos = 0L;
        inventoryLockedUntilNanos = 0L;
    }

    private void spawnPortal(Client client)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getWorldLocation() == null)
        {
            nextPortalNanos = System.nanoTime() + 100_000_000L;
            return;
        }

        WorldPoint target = findReachablePortalTile(client, player.getWorldLocation());
        if (target == null)
        {
            nextPortalNanos = System.nanoTime() + 250_000_000L;
            return;
        }

        LocalPoint local = LocalPoint.fromWorld(client, target);
        if (local == null)
        {
            nextPortalNanos = System.nanoTime() + 250_000_000L;
            return;
        }

        portalLocation = EventTileUtil.snapToTileCenter(local);
        portalEndTick = client.getTickCount() + COUNTDOWN_TICKS;
        updatePortalVisual(client);
    }


    // actually find reachable tiles for the player
    private WorldPoint findReachablePortalTile(Client client, WorldPoint start)
    {
        for (int attempt = 0; attempt < 24; attempt++)
        {
            WorldPoint current = start;
            int steps = 1 + random.nextInt(MAX_PORTAL_DISTANCE);

            for (int step = 0; step < steps; step++)
            {
                List<int[]> directions = new ArrayList<>();
                directions.add(new int[] { 1, 0 });
                directions.add(new int[] { -1, 0 });
                directions.add(new int[] { 0, 1 });
                directions.add(new int[] { 0, -1 });
                directions.add(new int[] { 1, 1 });
                directions.add(new int[] { 1, -1 });
                directions.add(new int[] { -1, 1 });
                directions.add(new int[] { -1, -1 });
                Collections.shuffle(directions, random);

                boolean moved = false;
                for (int[] direction : directions)
                {
                    WorldArea area = new WorldArea(current, 1, 1);
                    if (!area.canTravelInDirection(
                            client.getTopLevelWorldView(),
                            direction[0],
                            direction[1]))
                    {
                        continue;
                    }

                    WorldPoint next = new WorldPoint(
                            current.getX() + direction[0],
                            current.getY() + direction[1],
                            current.getPlane());

                    if (start.distanceTo2D(next) > MAX_PORTAL_DISTANCE)
                    {
                        continue;
                    }

                    current = next;
                    moved = true;
                    break;
                }

                if (!moved)
                {
                    break;
                }
            }

            if (!current.equals(start) && start.distanceTo2D(current) <= MAX_PORTAL_DISTANCE)
            {
                return current;
            }
        }

        return null;
    }

    private void drawPortal(Client client, Graphics2D g)
    {
        if (portalLocation == null)
        {
            return;
        }

        Point point = Perspective.localToCanvas(
                client, portalLocation, client.getPlane());
        if (point == null)
        {
            return;
        }

        int ticksLeft = Math.max(0, portalEndTick - client.getTickCount());
        String countdown = Integer.toString(ticksLeft);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
        int textWidth = g.getFontMetrics().stringWidth(countdown);
        int x = point.getX() - textWidth / 2;
        int y = point.getY() - 28;
        g.setColor(Color.BLACK);
        g.drawString(countdown, x + 2, y + 2);
        g.setColor(Color.WHITE);
        g.drawString(countdown, x, y);
    }

    private Model buildPortalModel(Client client)
    {
        NPCComposition composition = client.getNpcDefinition(PORTAL_NPC_ID);
        if (composition == null)
        {
            return null;
        }

        int[] modelIds = composition.getModels();
        if (modelIds == null || modelIds.length == 0)
        {
            return null;
        }

        ModelData[] parts = new ModelData[modelIds.length];
        for (int i = 0; i < modelIds.length; i++)
        {
            parts[i] = client.loadModelData(modelIds[i]);
            if (parts[i] == null)
            {
                return null;
            }
        }

        ModelData modelData = parts.length == 1 ? parts[0] : client.mergeModels(parts);
        if (modelData == null)
        {
            return null;
        }

        short[] find = composition.getColorToReplace();
        short[] replace = composition.getColorToReplaceWith();
        if (find != null && replace != null)
        {
            modelData = modelData.cloneColors();
            int count = Math.min(find.length, replace.length);
            for (int i = 0; i < count; i++)
            {
                modelData.recolor(find[i], replace[i]);
            }
        }

        int widthScale = composition.getWidthScale();
        int heightScale = composition.getHeightScale();
        if (widthScale != 128 || heightScale != 128)
        {
            modelData = modelData.cloneVertices();
            modelData.scale(widthScale, heightScale, widthScale);
        }

        return modelData.light(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST,
                ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
    }

    private void updatePortalVisual(Client client)
    {
        if (portalLocation == null)
        {
            destroyPortalVisual();
            return;
        }

        if (portalObject == null)
        {
            Model model = buildPortalModel(client);
            if (model == null)
            {
                return;
            }

            portalObject = client.createRuneLiteObject();
            portalObject.setModel(model);
            portalObject.setLocation(portalLocation, client.getPlane());
            portalObject.setDrawFrontTilesFirst(true);
			Animation animation = client.loadAnimation(PORTAL_ANIMATION);
            if (animation != null)
            {
                AnimationController controller = new AnimationController(client, animation);
                controller.setOnFinished(AnimationController::loop);
                portalObject.setAnimationController(controller);
            }

            portalObject.setActive(true);
            return;
        }

        portalObject.setLocation(portalLocation, client.getPlane());
    }

    private void destroyPortalVisual()
    {
        if (portalObject != null)
        {
            portalObject.setActive(false);
            portalObject.setModel(null);
            portalObject = null;
        }
    }

    private boolean isInventoryLocked()
    {
        return System.nanoTime() < inventoryLockedUntilNanos;
    }

    private static boolean isSameTile(LocalPoint a, LocalPoint b)
    {
        return a != null && b != null
                && a.getSceneX() == b.getSceneX()
                && a.getSceneY() == b.getSceneY();
    }

}
