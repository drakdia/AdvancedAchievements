package com.hm.achievement.listener;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.NormalAchievements;

/**
 * Listener class to deal with Distance and PlayedTime achievements.
 * 
 * @author Pyves
 *
 */
public class AchieveQuitListener extends AbstractListener implements Listener {

	public AchieveQuitListener(AdvancedAchievements plugin) {

		super(plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {

		final String playerUUID = event.getPlayer().getUniqueId().toString();

		// Clean cooldown HashMaps for book and list commands.
		plugin.getAchievementBookCommand().getPlayersBookTime().remove(playerUUID);
		plugin.getAchievementListCommand().getPlayersListTime().remove(playerUUID);

		processAndCleanDistances(playerUUID);

		processAndCleanPlayedTime(playerUUID);

		// Remove player from HashSet cache for MaxLevel achievements.
		if (plugin.getXpListener() != null) {
			for (Integer achievementThreshold : plugin.getXpListener().getAchievementsCache().keySet()) {
				plugin.getXpListener().getAchievementsCache().remove(achievementThreshold, playerUUID);
			}
		}

		// Remove player from HashSet for Connection achievements.
		if (plugin.getConnectionListener() != null) {
			plugin.getConnectionListener().getPlayersAchieveConnectionRan().remove(playerUUID);
		}
	}

	/**
	 * Writes the distances to the database and cleans the various in memory objects containing information about the
	 * disconnected player.
	 * 
	 * @param event
	 * @param playerUUID
	 */
	private void processAndCleanDistances(final String playerUUID) {

		// Remove player from Multimap caches for distance achievements.
		if (plugin.getAchieveDistanceRunnable() != null
				&& plugin.getAchieveDistanceRunnable().getPlayerLocations().remove(playerUUID) != null) {
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getFootAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getFootAchievementsCache().remove(achievementThreshold, playerUUID);
			}
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getHorseAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getHorseAchievementsCache().remove(achievementThreshold,
						playerUUID);
			}
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getPigAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getPigAchievementsCache().remove(achievementThreshold, playerUUID);
			}
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getBoatAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getBoatAchievementsCache().remove(achievementThreshold, playerUUID);
			}
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getMinecartAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getMinecartAchievementsCache().remove(achievementThreshold,
						playerUUID);
			}
			for (Integer achievementThreshold : plugin.getAchieveDistanceRunnable().getGlidingAchievementsCache()
					.keySet()) {
				plugin.getAchieveDistanceRunnable().getGlidingAchievementsCache().remove(achievementThreshold,
						playerUUID);
			}

			// Update database statistics for distances and clean HashMaps.
			if (plugin.isAsyncPooledRequestsSender()) {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {

						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEFOOT);
						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEPIG);
						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEHORSE);
						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEBOAT);
						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEMINECART);
						updateAndRemoveDistance(playerUUID, NormalAchievements.DISTANCEGLIDING);
					}

					private void updateAndRemoveDistance(final String playerUUID, NormalAchievements category) {

						// Items must be removed from HashMaps AFTER write to DB has finished. As this is an async task,
						// we could end up in a scenario where the player reconnects and data is not yet updated in the
						// database; in this case, the cached variables will still be valid.
						Map<String, Integer> map = plugin.getPoolsManager().getHashMap(category);
						Integer distance = map.get(playerUUID);
						if (distance != null) {
							plugin.getDb().updateDistance(playerUUID, distance, category.toDBName());
							map.remove(playerUUID);
						}
					}
				});
			} else {
				// Items can be removed from HashMaps directly, as this is done in the main thread of execution.
				Integer distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEFOOT)
						.remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEFOOT.toDBName());
				}

				distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEPIG).remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEPIG.toDBName());
				}

				distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEHORSE).remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEHORSE.toDBName());
				}

				distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEBOAT).remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEBOAT.toDBName());
				}

				distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEMINECART).remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEMINECART.toDBName());
				}

				distance = plugin.getPoolsManager().getHashMap(NormalAchievements.DISTANCEGLIDING).remove(playerUUID);
				if (distance != null) {
					plugin.getDb().updateDistance(playerUUID, distance, NormalAchievements.DISTANCEGLIDING.toDBName());
				}
			}
		}
	}

	/**
	 * Writes the played time to the database and cleans the various in memory objects containing information about the
	 * disconnected player.
	 * 
	 * @param playerUUID
	 */
	private void processAndCleanPlayedTime(final String playerUUID) {

		if (plugin.getAchievePlayTimeRunnable() != null) {
			// Update database statistics for played time and clean HashMaps.
			if (plugin.isAsyncPooledRequestsSender()) {

				Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {

						// Items must be removed from HashMap AFTER write to DB has finished. As this is an async task,
						// we could end up in a scenario where the player reconnects and data is not yet updated in the
						// database; in this case, the cached variables will still be valid.
						Long playTime = plugin.getPoolsManager().getPlayedTimeHashMap().get(playerUUID);

						if (playTime != null) {
							plugin.getDb().updatePlaytime(playerUUID, playTime);
							plugin.getPoolsManager().getPlayedTimeHashMap().remove(playerUUID);
						}
					}
				});
			} else {
				// Items can be removed from HashMaps directly, as this is done in the main thread of execution.
				Long playTime = plugin.getPoolsManager().getPlayedTimeHashMap().remove(playerUUID);

				if (playTime != null) {
					plugin.getDb().updatePlaytime(playerUUID, playTime);
				}
			}
			// Remove player from Multimap cache for PlayedTime achievements.
			for (Integer achievementThreshold : plugin.getAchievePlayTimeRunnable().getAchievementsCache().keySet()) {
				plugin.getAchievePlayTimeRunnable().getAchievementsCache().remove(achievementThreshold, playerUUID);
			}
		}
	}
}
