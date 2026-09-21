package com.chaosmod.events;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;


@Getter
final class NoobOutfitEvent implements ChaosMod
{
    private static final int ADAMANT_FULL_HELM = 1161;
    private static final int ADAMANT_PLATEBODY = 1123;
    private static final int ADAMANT_PLATELEGS = 1073;
    private static final int RUNE_2H_SWORD = 1319;
    private static final int AMULET_OF_STRENGTH = 1725;

    private static final int CLIMBING_BOOTS = 3105;
    private static final int BRONZE_GLOVES = 7453;

    private static final int RUNE_2H_IDLE = 2065;
    private static final int RUNE_2H_WALK = 2064;
    private static final int RUNE_2H_RUN = 1664;
    private static final int RUNE_2H_SLASH = 407;

    private final String name = "Noob Outfit";
    private final String description = "Who is the cute little noob.";
    private final int durationSeconds;

    private int[] originalEquipment;
    private int originalIdle;
    private int originalWalk;
    private int originalWalkLeft;
    private int originalWalkRight;
    private int originalWalk180;
    private int originalRun;
    private boolean active;

    NoobOutfitEvent(int durationSeconds)
    {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void start(Client client)
    {
        stop(client);

        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null)
        {
            return;
        }

        PlayerComposition composition = player.getPlayerComposition();
        originalEquipment = composition.getEquipmentIds().clone();

        originalIdle = player.getIdlePoseAnimation();
        originalWalk = player.getWalkAnimation();
        originalWalkLeft = player.getWalkRotateLeft();
        originalWalkRight = player.getWalkRotateRight();
        originalWalk180 = player.getWalkRotate180();
        originalRun = player.getRunAnimation();

        active = true;
        applyOutfit(composition);
        applyRune2hMovement(player);
    }

    @Override
    public void onClientTick(Client client)
    {
        maintain(client);
    }

    private void maintain(Client client)
    {
        if (!active)
        {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null)
        {
            return;
        }

        applyOutfit(player.getPlayerComposition());
        applyRune2hMovement(player);

        if (player.getInteracting() != null
            && player.getAnimation() != -1
            && player.getAnimation() != RUNE_2H_SLASH)
        {
            player.setAnimation(RUNE_2H_SLASH);
            player.setAnimationFrame(0);
        }
    }

    @Override
    public void stop(Client client)
    {
        if (active)
        {
            Player player = client.getLocalPlayer();
            if (player != null)
            {
                if (originalEquipment != null && player.getPlayerComposition() != null)
                {
                    PlayerComposition composition = player.getPlayerComposition();
                    int[] equipment = composition.getEquipmentIds();
                    System.arraycopy(originalEquipment, 0, equipment, 0,
                        Math.min(originalEquipment.length, equipment.length));
                    composition.setHash();
                }

                player.setIdlePoseAnimation(originalIdle);
                player.setWalkAnimation(originalWalk);
                player.setWalkRotateLeft(originalWalkLeft);
                player.setWalkRotateRight(originalWalkRight);
                player.setWalkRotate180(originalWalk180);
                player.setRunAnimation(originalRun);
            }
        }

        active = false;
        originalEquipment = null;
    }

    private static void applyRune2hMovement(Player player)
    {
        player.setIdlePoseAnimation(RUNE_2H_IDLE);
        player.setWalkAnimation(RUNE_2H_WALK);
        player.setWalkRotateLeft(RUNE_2H_WALK);
        player.setWalkRotateRight(RUNE_2H_WALK);
        player.setWalkRotate180(RUNE_2H_WALK);
        player.setRunAnimation(RUNE_2H_RUN);
    }

    private void applyOutfit(PlayerComposition composition)
    {
        int[] equipment = composition.getEquipmentIds();

        for (int i = 0; i < equipment.length; i++)
        {
            if (equipment[i] >= PlayerComposition.ITEM_OFFSET)
            {
                equipment[i] = 0;
            }
        }

        setItem(equipment, KitType.HEAD, ADAMANT_FULL_HELM);
        setItem(equipment, KitType.AMULET, AMULET_OF_STRENGTH);
        setItem(equipment, KitType.WEAPON, RUNE_2H_SWORD);
        setItem(equipment, KitType.TORSO, ADAMANT_PLATEBODY);
        setItem(equipment, KitType.LEGS, ADAMANT_PLATELEGS);
        setItem(equipment, KitType.HANDS, BRONZE_GLOVES);
        setItem(equipment, KitType.BOOTS, CLIMBING_BOOTS);

        equipment[KitType.ARMS.getIndex()] = 0;
        equipment[KitType.SHIELD.getIndex()] = 0;
        composition.setHash();
    }

    private static void setItem(int[] equipment, KitType slot, int itemId)
    {
        equipment[slot.getIndex()] = itemId + PlayerComposition.ITEM_OFFSET;
    }
}
