package com.chaosmod.events;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Random;
import lombok.Getter;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;

@Getter
final class SpawnJadEvent implements ChaosMod
{
    private static final int TILE_SIZE = 128;
    private static final int SPAWN_DISTANCE = 10;
    private static final int ATTACK_SPEED_TICKS = 8;
    private static final int ATTACK_WINDUP_TICKS = 4;
    private static final int PRAYER_DISABLE_TICKS = 5;

    private static final int JAD_NPC_ID = 3127;
    private static final int JAD_IDLE_ANIMATION = 2650;
    private static final int JAD_RANGE_ANIMATION = 2652;
    private static final int JAD_MAGIC_ANIMATION = 2656;
    private static final int JAD_MAGIC_PROJECTILE = 448;
    private static final int JAD_RANGE_IMPACT = 451;

    private static final int JAD_MAGIC_SOUND = 162;
    private static final int JAD_RANGE_SOUND = 163;

    private static final int RANGE_SPOTANIM_KEY = 0x5A4D;

    private final String name = "Spawn Jad";
    private final String description = "Flick Jad correctly or lose prayer activation for 5 ticks";
    private final int durationSeconds;
    private final Random random;

    private RuneLiteObject jad;
    private LocalPoint jadLocation;
    private Attack pendingAttack;
    private int resolveAttackTick = -1;
    private int nextAttackTick = -1;
    private int prayerDisabledUntilTick = -1;
    private int startingRegionId = -1;
    private boolean complete;

    SpawnJadEvent(int durationSeconds, Random random)
    {
        this.durationSeconds = durationSeconds;
        this.random = random;
    }

    @Override
    public void start(Client client)
    {
        stop(client);
        Player player = client.getLocalPlayer();
        if (player == null || player.getLocalLocation() == null)
        {
            return;
        }

        startingRegionId = player.getWorldLocation().getRegionID();
        complete = false;

        Model model = buildJadModel(client);
        if (model == null)
        {
            return;
        }

		LocalPoint origin = EventTileUtil.snapToTileCenter(player.getLocalLocation());
        int side = random.nextInt(4);
        int dx = side == 0 ? SPAWN_DISTANCE : side == 1 ? -SPAWN_DISTANCE : 0;
        int dy = side == 2 ? SPAWN_DISTANCE : side == 3 ? -SPAWN_DISTANCE : 0;
        jadLocation = new LocalPoint(origin.getX() + dx * TILE_SIZE,
                origin.getY() + dy * TILE_SIZE, origin.getWorldView());

        jad = client.createRuneLiteObject();
        jad.setModel(model);
        jad.setLocation(jadLocation, client.getPlane());
        setJadPose(client, JAD_IDLE_ANIMATION);
        jad.setActive(true);

        nextAttackTick = client.getTickCount() + 2;
    }

    @Override
    public void onGameTick(Client client)
    {
        Player player = client.getLocalPlayer();
        if (player != null && startingRegionId >= 0
                && player.getWorldLocation().getRegionID() != startingRegionId)
        {
            complete = true;
            return;
        }

        if (jad == null || !jad.isActive())
        {
            return;
        }

        int tick = client.getTickCount();

        facePlayer(client);

        if (pendingAttack != null && tick >= resolveAttackTick)
        {
            Attack resolvedAttack = pendingAttack;
            resolveAttack(client, resolvedAttack);
            pendingAttack = null;
            resolveAttackTick = -1;
            jad.setAnimationController(null);

            facePlayer(client);
        }

        // A missed flick pauses Jad completely until the lockout has ended.
        if (isPrayerDisabled(client))
        {
            return;
        }

        if (prayerDisabledUntilTick >= 0 && tick >= prayerDisabledUntilTick)
        {
            prayerDisabledUntilTick = -1;
            // "tries to hit the player again" immediately after the punishment.
            nextAttackTick = tick;
        }

        if (pendingAttack == null && tick >= nextAttackTick)
        {
            beginAttack(client);
        }
    }

    @Override
    public boolean isComplete()
    {
        return complete;
    }

    private void facePlayer(Client client)
    {
        if (jad == null || client.getLocalPlayer() == null)
        {
            return;
        }

        LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        if (jadLocation == null || playerLocation == null)
        {
            return;
        }

        int dx = playerLocation.getX() - jadLocation.getX();
        int dy = playerLocation.getY() - jadLocation.getY();

        if (dx == 0 && dy == 0)
        {
            return;
        }

        int orientation = ((int) Math.round(
                Math.atan2(dx, dy) * (1024.0 / Math.PI)
        ) + 1024) & 2047;

        jad.setOrientation(orientation);
    }

    @Override
    public void onMenuOptionClicked(Client client, MenuOptionClicked event)
    {
		if (!isPrayerDisabled(client) || !EventInputUtil.isPrayerActivationClick(event))
        {
            return;
        }

        event.consume();
    }

    @Override
    public void render(Client client, Graphics2D graphics)
    {
        if (!isPrayerDisabled(client))
        {
            return;
        }

        int ticks = Math.max(0, prayerDisabledUntilTick - client.getTickCount());
        String text = "PRAYERS DISABLED: " + ticks + " tick" + (ticks == 1 ? "" : "s");
        Graphics2D warningGraphics = (Graphics2D) graphics.create();
        try
        {
            warningGraphics.setFont(warningGraphics.getFont().deriveFont(Font.BOLD, 18f));
            int width = warningGraphics.getFontMetrics().stringWidth(text);
            int x = (client.getCanvasWidth() - width) / 2;
            int y = 200;
            warningGraphics.setColor(Color.BLACK);
            warningGraphics.drawString(text, x + 2, y + 2);
            warningGraphics.setColor(Color.RED);
            warningGraphics.drawString(text, x, y);
        }
        finally
        {
            warningGraphics.dispose();
        }
    }

    @Override
    public void stop(Client client)
    {
        if (jad != null)
        {
            jad.setAnimationController(null);
            jad.setPoseAnimationController(null);
            jad.setActive(false);
            jad.setModel(null);
            jad = null;
        }

        Player player = client.getLocalPlayer();
        if (player != null)
        {
            player.removeSpotAnim(RANGE_SPOTANIM_KEY);
        }

        jadLocation = null;
        pendingAttack = null;
        resolveAttackTick = -1;
        nextAttackTick = -1;
        prayerDisabledUntilTick = -1;
        startingRegionId = -1;
        complete = false;
    }

    private Model buildJadModel(Client client)
    {
        NPCComposition composition = client.getNpcDefinition(JAD_NPC_ID);
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

    private void beginAttack(Client client)
    {
        pendingAttack = random.nextBoolean() ? Attack.RANGE : Attack.MAGIC;
        resolveAttackTick = client.getTickCount() + ATTACK_WINDUP_TICKS;
        nextAttackTick = client.getTickCount() + ATTACK_SPEED_TICKS;

        if (pendingAttack == Attack.RANGE)
        {
            playAttackAnimation(client, JAD_RANGE_ANIMATION);
            client.playSoundEffect(JAD_RANGE_SOUND);
        }
        else
        {
            playAttackAnimation(client, JAD_MAGIC_ANIMATION);
            client.playSoundEffect(JAD_MAGIC_SOUND);
        }
        facePlayer(client);
    }

    private void resolveAttack(Client client, Attack attack)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getLocalLocation() == null || jadLocation == null)
        {
            return;
        }

        if (attack == Attack.MAGIC)
        {
            WorldPoint from = WorldPoint.fromLocal(client, jadLocation);
            WorldPoint to = player.getWorldLocation();
            int startCycle = client.getGameCycle();
            client.createProjectile(JAD_MAGIC_PROJECTILE, from, 80, null, to, 40, player,
                    startCycle, startCycle + 30, 16, 64);
        }
        else
        {
            player.removeSpotAnim(RANGE_SPOTANIM_KEY);
            player.createSpotAnim(RANGE_SPOTANIM_KEY, JAD_RANGE_IMPACT, 0, 0);
        }

        boolean protectedCorrectly = attack == Attack.MAGIC
                ? client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)
                : client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);

        if (!protectedCorrectly)
        {
            prayerDisabledUntilTick = client.getTickCount() + PRAYER_DISABLE_TICKS;
            nextAttackTick = Integer.MAX_VALUE;
        }
    }

    private boolean isPrayerDisabled(Client client)
    {
        return prayerDisabledUntilTick >= 0 && client.getTickCount() < prayerDisabledUntilTick;
    }

    private void setJadPose(Client client, int animationId)
    {
        if (jad == null)
        {
            return;
        }

        Animation animation = client.loadAnimation(animationId);
        if (animation == null)
        {
            return;
        }

        AnimationController poseController = new AnimationController(client, animation);
        poseController.setOnFinished(AnimationController::loop);
        jad.setPoseAnimationController(poseController);
        facePlayer(client);
    }

    private void playAttackAnimation(Client client, int animationId)
    {
        if (jad == null)
        {
            return;
        }
        Animation animation = client.loadAnimation(animationId);
        if (animation == null)
        {
            return;
        }
        AnimationController actionController = new AnimationController(client, animation);
        actionController.setOnFinished(finishedController ->
        {
            int frames = animation.getNumFrames();
            if (frames > 0)
            {
                finishedController.setFrame(frames - 1);
            }
        });
        jad.setAnimationController(actionController);
        facePlayer(client);
    }

    private enum Attack
    {
        RANGE,
        MAGIC
    }
}
