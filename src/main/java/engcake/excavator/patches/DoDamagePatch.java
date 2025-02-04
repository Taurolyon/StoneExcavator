package tauro.excavator.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.ObjectDamageResult;
import necesse.entity.mobs.Attacker;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(
        target = DamagedObjectEntity.class,
        name = "doObjectDamage",
        arguments = {
                int.class,
                int.class,
                int.class,
                Attacker.class,
                ServerClient.class,
                boolean.class,
                int.class,
                int.class
        }
)
public class DoDamagePatch {

    // Define a constant for the maximum number of objects to affect
    private static final int MAX_AFFECTED_OBJECTS = 50;

    @Advice.OnMethodExit
    static void onExit(
            @Advice.This DamagedObjectEntity self,
            @Advice.Argument(0) int objectLayerID,
            @Advice.Argument(1) int damage,
            @Advice.Argument(2) int toolTier,
            @Advice.Argument(3) Attacker attacker,
            @Advice.Argument(4) ServerClient client,
            @Advice.Argument(5) boolean showEffects,
            @Advice.Argument(6) int mouseX,
            @Advice.Argument(7) int mouseY,
            @Advice.Return(readOnly = true) ObjectDamageResult result
    ) {
        if (client == null) {
            return;
        }

        Level level = client.getLevel();

        if (result == null || !result.destroyed) {
            return;
        }

        if (!shouldPropagate(result.levelObject.object)) {
            return;
        }

        int tileX = self.getTileX();
        int tileY = self.getTileY();

        // Track the number of affected objects
        int affectedObjects = 0;

        LevelObject[] adjacentObjects = level.getAdjacentLevelObjects(tileX, tileY);
        for (LevelObject levelObject : adjacentObjects) {
            if (affectedObjects >= MAX_AFFECTED_OBJECTS) {
                break; // Stop if we've reached the maximum number of objects
            }

            int currentTileX = levelObject.tileX;
            int currentTileY = levelObject.tileY;

            if (!shouldPropagate(levelObject.object) || (currentTileX != tileX && currentTileY != tileY)) {
                continue;
            }

            level.entityManager.doObjectDamage(
                    objectLayerID,
                    levelObject.tileX,
                    levelObject.tileY,
                    levelObject.object.objectHealth,
                    toolTier,
                    attacker,
                    client,
                    showEffects,
                    mouseX,
                    mouseY
            );

            // Increment the counter
            affectedObjects++;
        }
    }

    public static boolean shouldPropagate(GameObject gameObject) {
        return gameObject.isRock;
    }
}