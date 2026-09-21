package com.chaosmod.events;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.VarClientID;

final class GiantPlayerEvent implements ChaosMod
{
    private static final int GIANT_SCALE = 700;
    private static final long REFRESH_INTERVAL_MS = 4000L;

    private final int durationSeconds;

    private RuneLiteObject giant;
    private boolean active;
    private long nextRefreshTime;
    private int previousSmallZoom;
    private int previousBigZoom;

    GiantPlayerEvent(int durationSeconds)
    {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String getName()
    {
        return "Giant Player";
    }

    @Override
    public String getDescription()
    {
        return "Makes your character look gigantic.";
    }

    @Override
    public int getDurationSeconds()
    {
        return durationSeconds;
    }

    @Override
    public void start(Client client)
    {
        destroyGiant();

        previousSmallZoom = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL);
        previousBigZoom = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG);
        active = true;
        forceMaximumZoomDistance(client);

        createGiant(client);

        nextRefreshTime =
                System.currentTimeMillis() + REFRESH_INTERVAL_MS;

        updateGiant(client);
    }

    @Override
    public void onClientTick(Client client)
    {
        if (!active)
        {
            return;
        }

        forceMaximumZoomDistance(client);

        long now = System.currentTimeMillis();

        if (now >= nextRefreshTime)
        {
            rebuildGiant(client);

            nextRefreshTime =
                    now + REFRESH_INTERVAL_MS;
        }
        else
        {
            updateGiant(client);
        }
    }

    @Override
    public void onScriptCallbackEvent(Client client, ScriptCallbackEvent event)
    {
        if (!active || !"scrollWheelZoom".equals(event.getEventName()))
        {
            return;
        }

        int stackSize = client.getIntStackSize();
        if (stackSize > 0)
        {
            client.getIntStack()[stackSize - 1] = 1;
        }
    }

    private void forceMaximumZoomDistance(Client client)
    {
        int smallOuterLimit = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL_MIN);
        int bigOuterLimit = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG_MIN);

        if (client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL) != smallOuterLimit
                || client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG) != bigOuterLimit)
        {
            client.runScript(ScriptID.CAMERA_DO_ZOOM, smallOuterLimit, bigOuterLimit);
        }
    }

    private void rebuildGiant(Client client)
    {
        destroyGiant();
        createGiant(client);
        updateGiant(client);
    }

    private void createGiant(Client client)
    {
        giant = client.createRuneLiteObject();
        giant.setRadius(120);
        giant.setActive(true);
    }

    private void updateGiant(Client client)
    {
        if (giant == null)
        {
            createGiant(client);
        }

        Player player = client.getLocalPlayer();

        if (player == null)
        {
            giant.setActive(false);
            return;
        }

        LocalPoint location = player.getLocalLocation();
        Model playerModel = player.getModel();

        if (location == null || playerModel == null)
        {
            giant.setActive(false);
            return;
        }

        Model giantModel = client.mergeModels(
                new Model[] { playerModel },
                1
        );

        if (giantModel == null)
        {
            giant.setActive(false);
            return;
        }

        giantModel.scale(
                GIANT_SCALE,
                GIANT_SCALE,
                GIANT_SCALE
        );

        giantModel.calculateBoundsCylinder();

        giant.setModel(giantModel);
        giant.setLocation(
                location,
                client.getPlane()
        );

        giant.setOrientation(
                player.getCurrentOrientation()
        );

        giant.setActive(true);
    }

    private void destroyGiant()
    {
        if (giant == null)
        {
            return;
        }

        giant.setActive(false);
        giant.setModel(null);
        giant = null;
    }

    @Override
    public void stop(Client client)
    {
        active = false;
        nextRefreshTime = 0L;

        client.runScript(ScriptID.CAMERA_DO_ZOOM, previousSmallZoom, previousBigZoom);

        destroyGiant();
    }
}
