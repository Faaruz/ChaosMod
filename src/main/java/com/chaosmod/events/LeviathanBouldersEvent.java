package com.chaosmod.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;

@Getter
final class LeviathanBouldersEvent implements ChaosMod
{
    private static final int IMPACT_DELAY_TICKS = 2;
    private static final int BOULDER_INTERVAL_TICKS = 2;
    private static final int SOURCE_DISTANCE = 8;

    // Five seconds in real time, matching the punishment wording.
    private static final long PRAYER_DISABLE_NANOS = 5_000_000_000L;

    private static final int BOULDER_SPOTANIM = 2472;
    private static final int BOULDER_FIRE_SOUND = 7028;
    private static final int TEMP_SPOTANIM_KEY = 0x4C42;

    private static final BasicStroke TARGET_STROKE = new BasicStroke(2f);
    private static final Color TARGET_FILL = new Color(255, 60, 20, 65);

    private final String name = "Leviathan Boulders";
    private final String description =
            "Dodge falling boulders; standing on a fallen boulder disables prayers for 5 seconds";
    private final int durationSeconds;

    private final List<PendingBoulder> pendingBoulders = new ArrayList<>();
    private final List<LandedBoulder> landedBoulders = new ArrayList<>();

    private Model boulderModel;
    private int nextBoulderTick = -1;
    private long prayersDisabledUntilNanos;

    LeviathanBouldersEvent(int durationSeconds)
    {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void start(Client client)
    {
        stop(client);

        boulderModel = loadBoulderModel(client);
        nextBoulderTick = client.getTickCount() + 1;
    }

    @Override
    public void onGameTick(Client client)
    {
        int tick = client.getTickCount();

        resolveBoulderImpacts(client, tick);
        checkFallenBoulderPunishment(client);

        if (tick >= nextBoulderTick)
        {
            launchBoulderAtPlayer(client);
            nextBoulderTick = tick + BOULDER_INTERVAL_TICKS;
        }
    }

    @Override
    public void onMenuOptionClicked(Client client, MenuOptionClicked event)
    {
		if (isPrayerDisabled() && EventInputUtil.isPrayerActivationClick(event))
        {
            event.consume();
        }
    }

    @Override
    public boolean canStop()
    {
        // Let an already-triggered five-second punishment finish even if the
        // normal event timer expires.
        return !isPrayerDisabled();
    }

    @Override
    public void render(Client client, Graphics2D graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            drawPrayerDisabled(client, g);
        }
        finally
        {
            g.dispose();
        }
    }

    @Override
    public void renderTileMarkers(Client client, Graphics2D graphics)
    {
        Graphics2D markerGraphics = (Graphics2D) graphics.create();
        try
        {
            drawPendingTargets(client, markerGraphics);
        }
        finally
        {
            markerGraphics.dispose();
        }
    }

    @Override
    public void stop(Client client)
    {
        for (LandedBoulder boulder : landedBoulders)
        {
            boulder.object.setActive(false);
            boulder.object.setModel(null);
        }

        landedBoulders.clear();
        pendingBoulders.clear();

        Player player = client.getLocalPlayer();
        if (player != null)
        {
            player.removeSpotAnim(TEMP_SPOTANIM_KEY);
        }

        boulderModel = null;
        nextBoulderTick = -1;
        prayersDisabledUntilNanos = 0L;
    }

    private void resolveBoulderImpacts(Client client, int tick)
    {
        Iterator<PendingBoulder> iterator = pendingBoulders.iterator();

        while (iterator.hasNext())
        {
            PendingBoulder boulder = iterator.next();
            if (tick < boulder.impactTick)
            {
                continue;
            }

            iterator.remove();
            landBoulder(client, boulder.location);
        }
    }

    private void checkFallenBoulderPunishment(Client client)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getLocalLocation() == null)
        {
            return;
        }

        if (isOnFallenBoulder(player.getLocalLocation()))
        {
            // Being on a fallen rock is another failure, so the punishment is
            // reset to a full five seconds every game tick the player remains on it.
            prayersDisabledUntilNanos = System.nanoTime() + PRAYER_DISABLE_NANOS;
        }
    }

    private void launchBoulderAtPlayer(Client client)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getLocalLocation() == null
                || player.getWorldLocation() == null)
        {
            return;
        }

        LocalPoint targetLocal =
                EventTileUtil.snapToTileCenter(player.getLocalLocation());

        if (targetLocal == null || isPendingBoulder(targetLocal))
        {
            return;
        }

        WorldPoint targetWorld = WorldPoint.fromLocal(client, targetLocal);
        if (targetWorld == null)
        {
            return;
        }

        WorldPoint sourceWorld = createProjectileSource(targetWorld);
        int startCycle = client.getGameCycle();

        client.playSoundEffect(BOULDER_FIRE_SOUND);

        client.createProjectile(
                BOULDER_SPOTANIM,
                sourceWorld,
                500,
                null,
                targetWorld,
                0,
                null,
                startCycle,
                startCycle + IMPACT_DELAY_TICKS * 30,
                90,
                0);

        pendingBoulders.add(new PendingBoulder(
                targetLocal,
                client.getTickCount() + IMPACT_DELAY_TICKS));
    }

    private static WorldPoint createProjectileSource(WorldPoint target)
    {
        int x = target.getX();
        int y = target.getY();

        switch (ThreadLocalRandom.current().nextInt(4))
        {
            case 0:
                x += SOURCE_DISTANCE;
                break;
            case 1:
                x -= SOURCE_DISTANCE;
                break;
            case 2:
                y += SOURCE_DISTANCE;
                break;
            default:
                y -= SOURCE_DISTANCE;
                break;
        }

        return new WorldPoint(x, y, target.getPlane());
    }

    private void landBoulder(Client client, LocalPoint location)
    {
        if (boulderModel == null)
        {
            boulderModel = loadBoulderModel(client);
        }

        if (boulderModel == null)
        {
            return;
        }

        RuneLiteObject object = client.createRuneLiteObject();
        object.setModel(boulderModel);
        object.setLocation(location, client.getPlane());
        object.setDrawFrontTilesFirst(true);
        object.setActive(true);

        landedBoulders.add(new LandedBoulder(location, object));
    }

    private Model loadBoulderModel(Client client)
    {
        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return null;
        }

        player.removeSpotAnim(TEMP_SPOTANIM_KEY);
        player.createSpotAnim(TEMP_SPOTANIM_KEY, BOULDER_SPOTANIM, 0, 0);

        Model result = null;

        for (ActorSpotAnim spotAnim : player.getSpotAnims())
        {
            if (spotAnim.getId() != BOULDER_SPOTANIM)
            {
                continue;
            }

            Model model = spotAnim.getModel();
            if (model != null)
            {
                result = client.mergeModels(new Model[] { model }, 1);
            }
            break;
        }

        player.removeSpotAnim(TEMP_SPOTANIM_KEY);
        return result;
    }

    private boolean isOnFallenBoulder(LocalPoint playerLocation)
    {
        for (LandedBoulder boulder : landedBoulders)
        {
            if (isSameTile(playerLocation, boulder.location))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isPendingBoulder(LocalPoint location)
    {
        for (PendingBoulder boulder : pendingBoulders)
        {
            if (isSameTile(location, boulder.location))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isPrayerDisabled()
    {
        return System.nanoTime() < prayersDisabledUntilNanos;
    }

    private void drawPendingTargets(Client client, Graphics2D g)
    {
        g.setStroke(TARGET_STROKE);

        for (PendingBoulder boulder : pendingBoulders)
        {
            Polygon tile = Perspective.getCanvasTilePoly(client, boulder.location);
            if (tile == null)
            {
                continue;
            }

            g.setColor(TARGET_FILL);
            g.fill(tile);
            g.setColor(Color.RED);
            g.draw(tile);
        }
    }

    private void drawPrayerDisabled(Client client, Graphics2D g)
    {
        if (!isPrayerDisabled())
        {
            return;
        }

        long remainingMillis = Math.max(
                0L,
                (prayersDisabledUntilNanos - System.nanoTime()) / 1_000_000L);

        String text = String.format(
                Locale.ROOT,
                "PRAYERS DISABLED: %.1fs",
                remainingMillis / 1000.0);

        g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
        int width = g.getFontMetrics().stringWidth(text);
        int x = (client.getCanvasWidth() - width) / 2;
        int y = 70;

        g.setColor(Color.BLACK);
        g.drawString(text, x + 2, y + 2);
        g.setColor(Color.RED);
        g.drawString(text, x, y);
    }

    private static boolean isSameTile(LocalPoint a, LocalPoint b)
    {
        return a != null && b != null
                && a.getSceneX() == b.getSceneX()
                && a.getSceneY() == b.getSceneY();
    }

    private static final class PendingBoulder
    {
        private final LocalPoint location;
        private final int impactTick;

        private PendingBoulder(LocalPoint location, int impactTick)
        {
            this.location = location;
            this.impactTick = impactTick;
        }
    }

    private static final class LandedBoulder
    {
        private final LocalPoint location;
        private final RuneLiteObject object;

        private LandedBoulder(LocalPoint location, RuneLiteObject object)
        {
            this.location = location;
            this.object = object;
        }
    }
}
