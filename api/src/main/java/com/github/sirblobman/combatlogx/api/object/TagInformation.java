package com.github.sirblobman.combatlogx.api.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.github.sirblobman.api.utility.Validate;

public final class TagInformation {
    private final UUID playerId;
    private final List<CombatTag> tagList;

    public TagInformation(@NotNull OfflinePlayer player) {
        this(player.getUniqueId());
    }

    public TagInformation(@NotNull UUID playerId) {
        this.playerId = playerId;
        this.tagList = new ArrayList<>();
    }

    /**
     * @return The {@link UUID} of the player that is tagged.
     */
    public @NotNull UUID getPlayerId() {
        return this.playerId;
    }

    /**
     * @return The {@link OfflinePlayer} that is tagged.
     * Can be null if the server cache is removed or the player doesn't exist.
     * @see #getPlayerId()
     */
    public @Nullable OfflinePlayer getOfflinePlayer() {
        UUID playerId = getPlayerId();
        return Bukkit.getOfflinePlayer(playerId);
    }


    /**
     * @return The {@link Player} that is tagged.
     * Can be null if the player is offline.
     * @see #getPlayerId()
     */
    public @Nullable Player getPlayer() {
        UUID playerId = getPlayerId();
        return Bukkit.getPlayer(playerId);
    }

    /**
     * @return A list of tags the player currently has.
     * The list is sorted by descending expire time.
     */
    public @NotNull List<CombatTag> getTags() {
        List<CombatTag> tagList = new ArrayList<>(this.tagList);
        tagList.sort(Collections.reverseOrder());
        return Collections.unmodifiableList(tagList);
    }

    /**
     * @return A list of entity {@link UUID}s that the player is currently tagged with.
     * The list is sorted by descending expire time.
     */
    public @NotNull List<UUID> getEnemyIds() {
        List<CombatTag> tagList = getTags();
        List<UUID> enemyIdList = new ArrayList<>();

        for (CombatTag combatTag : tagList) {
            UUID enemyId = combatTag.getEnemyId();
            if (enemyId != null) {
                enemyIdList.add(enemyId);
            }
        }

        return Collections.unmodifiableList(enemyIdList);
    }

    /**
     * @return A list of {@link Entity} objects that the player is currently tagged with.
     * The list is sorted by descending expire time.
     */
    public @NotNull List<Entity> getEnemies() {
        List<CombatTag> tagList = getTags();
        List<Entity> enemyList = new ArrayList<>();

        if (tagList.isEmpty()) return enemyList;

        for (CombatTag combatTag : tagList) {
            if (combatTag == null) continue;

            Entity enemy = combatTag.getEnemy();
            if (enemy != null) {
                enemyList.add(enemy);
            }
        }

        return Collections.unmodifiableList(enemyList);
    }

    public boolean isEnemy(@NotNull Entity entity) {
        List<CombatTag> tagList = getTags();
        for (CombatTag combatTag : tagList) {
            if (combatTag.doesEnemyMatch(entity)) {
                return true;
            }
        }

        return false;
    }

    public void addTag(@NotNull CombatTag combatTag) {
        if (combatTag.isExpired()) {
            throw new IllegalArgumentException("combatTag is already expired!");
        }

        if (this.tagList.contains(combatTag)) {
            throw new IllegalArgumentException("The player already has that combat tag.");
        }

        this.tagList.removeIf(otherTag -> otherTag.doesEnemyMatch(combatTag.getEnemy()));
        this.tagList.add(combatTag);
    }

    public void removeEnemy(@NotNull Entity entity) {
        this.tagList.removeIf(combatTag -> combatTag.doesEnemyMatch(entity));
    }

    public long getExpireMillisCombined() {
        List<CombatTag> tagList = getTags();
        if (tagList.isEmpty()) {
            return 0L;
        }

        CombatTag latestTag = tagList.get(0);
        return latestTag.getExpireMillis();
    }

    public long getMillisLeftCombined() {
        long expireMillis = getExpireMillisCombined();
        if (expireMillis == 0L) {
            return 0L;
        }

        long systemMillis = System.currentTimeMillis();
        long subtractMillis = (expireMillis - systemMillis);
        return Math.max(0L, subtractMillis);
    }

    public boolean isExpired() {
        this.tagList.removeIf(CombatTag::isExpired);
        return this.tagList.isEmpty();
    }

    public @NotNull List<TagType> getTagTypes() {
        List<CombatTag> tagList = getTags();
        List<TagType> tagTypeList = new ArrayList<>();

        for (CombatTag combatTag : tagList) {
            TagType tagType = combatTag.getTagType();
            tagTypeList.add(tagType);
        }

        return Collections.unmodifiableList(tagTypeList);
    }

    public @NotNull TagType getCurrentTagType() {
        List<TagType> tagTypeList = getTagTypes();
        if (tagTypeList.isEmpty()) {
            return TagType.UNKNOWN;
        }

        return tagTypeList.get(0);
    }

    public @Nullable Entity getCurrentEnemy() {
        List<Entity> enemyList = getEnemies();
        if (enemyList.isEmpty()) {
            return null;
        }

        return enemyList.get(0);
    }

    public @Nullable CombatTag getTagForEnemy(Entity entity) {
        Validate.notNull(entity, "entity must not be null!");

        List<CombatTag> tagList = getTags();
        for (CombatTag combatTag : tagList) {
            if (combatTag.doesEnemyMatch(entity)) {
                return combatTag;
            }
        }

        return null;
    }
}
