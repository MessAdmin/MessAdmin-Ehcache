/**
 *
 */
package clime.messadmin.providers.ehcache;

import java.lang.reflect.Method;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;

/**
 * Access new Ehcache methods via reflexion, to avoid requiring linking the absolute newest version.
 * @author C&eacute;drik LIME
 */
public abstract class EhcacheHelper {

	private EhcacheHelper() {
		throw new AssertionError();
	}

	/* **********************************************************************
	 *  Ehcache.class
	 * **********************************************************************/

	private static transient Method isStatisticsEnabled = null;
	private static transient Method setStatisticsEnabled = null;
	static transient boolean hasStatistics = false;

	static {
		// @since Ehcache 1.7
		try {
			isStatisticsEnabled = Ehcache.class.getMethod("isStatisticsEnabled");//$NON-NLS-1$
			setStatisticsEnabled = Ehcache.class.getMethod("setStatisticsEnabled", Boolean.TYPE);//$NON-NLS-1$
			hasStatistics = true;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (Exception e) {// Ehcache 1.5.0 will throw ClassNotFoundException: net.sf.jsr107cache.CacheLoader, from Ehcache -> CacheLoader extends jsr107cache.CacheLoader
		}
	}

	/**
	 * By default (in case of error), consider statistics to be enabled,
	 * so that we at least try to display something...
	 * @since Ehcache 1.7
	 */
	public static boolean isStatisticsEnabled(Ehcache ehCache) {
		if (isStatisticsEnabled == null || ehCache == null) {
			return true;
		}
		//return ehCache.isStatisticsEnabled();
		try {
			return ((Boolean) isStatisticsEnabled.invoke(ehCache)).booleanValue();
		} catch (Exception ignore) {
			return true;
		}
	}
	/**
	 * @since Ehcache 1.7
	 */
	public static void setStatisticsEnabled(Ehcache ehCache, boolean enableStatistics) {
		if (setStatisticsEnabled == null || ehCache == null) {
			return;
		}
		//ehCache.setStatisticsEnabled(enableStatistics);
		try {
			setStatisticsEnabled.invoke(ehCache, new Object[] {Boolean.valueOf(enableStatistics)});
		} catch (Exception ignore) {
		}
	}


	/* **********************************************************************
	 *  Statistics.class and co.
	 * **********************************************************************/

	// Since Ehcache 2.3
	// Statistics
	private static transient Method getInMemoryMisses = null;
	private static transient Method getOnDiskMisses = null;
	// off-heap store, since Ehcache 2.3
	// CacheConfiguration
	private static transient Method isOverflowToOffHeap = null;
	private static transient Method getMaxMemoryOffHeap = null;
	private static transient Method getMaxMemoryOffHeapInBytes = null;
	// Statistics
	private static transient Method getOffHeapStoreObjectCount = null;
	private static transient Method getOffHeapHits = null;
	private static transient Method getOffHeapMisses = null;

	static transient boolean hasOffHeap = false;

	// Since Ehcache 2.4
	private static transient Method getAverageSearchTime = null;
	private static transient Method getSearchesPerSecond = null;
	static transient boolean hasSearch = false;

	static {
		// @since Ehcache 2.3
		try {
			isOverflowToOffHeap        = CacheConfiguration.class.getMethod("isOverflowToOffHeap");//$NON-NLS-1$
			getMaxMemoryOffHeap        = CacheConfiguration.class.getMethod("getMaxMemoryOffHeap");//$NON-NLS-1$
			getMaxMemoryOffHeapInBytes = CacheConfiguration.class.getMethod("getMaxMemoryOffHeapInBytes");//$NON-NLS-1$
			getInMemoryMisses          = Statistics.class.getMethod("getInMemoryMisses");//$NON-NLS-1$
			getOnDiskMisses            = Statistics.class.getMethod("getOnDiskMisses");//$NON-NLS-1$
			getOffHeapStoreObjectCount = Statistics.class.getMethod("getOffHeapStoreObjectCount");//$NON-NLS-1$
			getOffHeapHits             = Statistics.class.getMethod("getOffHeapHits");//$NON-NLS-1$
			getOffHeapMisses           = Statistics.class.getMethod("getOffHeapMisses");//$NON-NLS-1$
			hasOffHeap = true;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		// @since Ehcache 2.4
		try {
			getAverageSearchTime = Statistics.class.getMethod("getAverageSearchTime");//$NON-NLS-1$
			getSearchesPerSecond = Statistics.class.getMethod("getSearchesPerSecond");//$NON-NLS-1$
			hasSearch = true;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
	}

	/**
	 * @since Ehcache 2.3
	 */
	public static long getInMemoryMisses(Statistics stats) {
		if (getInMemoryMisses == null) {
			return -1;
		}
		//return stats.getInMemoryMisses();
		try {
			return ((Long) getInMemoryMisses.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static long getOnDiskMisses(Statistics stats) {
		if (getOnDiskMisses == null) {
			return -1;
		}
		//return stats.getOffHeapHits();
		try {
			return ((Long) getOnDiskMisses.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}

	/**
	 * @since Ehcache 2.3
	 */
	public static boolean isOverflowToOffHeap(CacheConfiguration config) {
		if (isOverflowToOffHeap == null) {
			return false;
		}
		//return config.isOverflowToOffHeap();
		try {
			return ((Boolean) isOverflowToOffHeap.invoke(config)).booleanValue();
		} catch (Exception ignore) {
			return false;
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static String getMaxMemoryOffHeap(CacheConfiguration config) {
		if (getMaxMemoryOffHeap == null) {
			return "";
		}
		//return config.getMaxMemoryOffHeap();
		try {
			return (String) getMaxMemoryOffHeap.invoke(config);
		} catch (Exception ignore) {
			return "";
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static long getMaxMemoryOffHeapInBytes(CacheConfiguration config) {
		if (getMaxMemoryOffHeapInBytes == null) {
			return -1;
		}
		//return config.getMaxMemoryOffHeapInBytes();
		try {
			return ((Long) getMaxMemoryOffHeapInBytes.invoke(config)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static long getOffHeapStoreObjectCount(Statistics stats) {
		if (getOffHeapStoreObjectCount == null) {
			return -1;
		}
		//return stats.getOffHeapHits();
		try {
			return ((Long) getOffHeapStoreObjectCount.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static long getOffHeapHits(Statistics stats) {
		if (getOffHeapHits == null) {
			return -1;
		}
		//return stats.getOffHeapHits();
		try {
			return ((Long) getOffHeapHits.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}
	/**
	 * @since Ehcache 2.3
	 */
	public static long getOffHeapMisses(Statistics stats) {
		if (getOffHeapMisses == null) {
			return -1;
		}
		//return stats.getOffHeapMisses();
		try {
			return ((Long) getOffHeapMisses.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}

	/**
	 * Get the average search execution time (in millis) for searches that have completed in the last sample period
	 * @since Ehcache 2.4
	 */
	public static long getAverageSearchTime(Statistics stats) {
		if (getAverageSearchTime == null) {
			return -1;
		}
		//return stats.getAverageSearchTime();
		try {
			return ((Long) getAverageSearchTime.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}
	/**
	 * Get the number of search executions that have completed in the last second
	 * @since Ehcache 2.4
	 */
	public static long getSearchesPerSecond(Statistics stats) {
		if (getSearchesPerSecond == null) {
			return -1;
		}
		//return stats.getSearchesPerSecond();
		try {
			return ((Long) getSearchesPerSecond.invoke(stats)).longValue();
		} catch (Exception ignore) {
			return -1;
		}
	}

	/* **********************************************************************
	 *  Misc utilities, shamelessly inspired by, and contributed back to, net.sf.ehcache.management.CacheStatistics
	 * **********************************************************************/

	private static double getPercentage(long number, long total) {
		if (total == 0) {
			return 0.0;
		} else {
			return number / (double)total;
		}
	}

	/**
	 * Returns the percentage of cache accesses that found a requested item in the cache.
	 *
	 * @return the percentage of successful hits
	 */
	public static double getCacheHitPercentage(Statistics statistics) {
		long hits = statistics.getCacheHits();
		long misses = statistics.getCacheMisses();
		long total = hits + misses;
		return getPercentage(hits, total);
	}

	/**
	 * Returns the percentage of cache accesses that did not find a requested element in the cache.
	 *
	 * @return the percentage of accesses that failed to find anything
	 */
	public static double getCacheMissPercentage(Statistics statistics) {
		long hits = statistics.getCacheHits();
		long misses = statistics.getCacheMisses();
		long total = hits + misses;
		return getPercentage(misses, total);
	}

	/**
	 * Returns the percentage of in-memory cache accesses that found a requested item cached.
	 *
	 * @return the percentage of successful MemoryStore hits
	 * @since Ehcache 2.3
	 */
	public static double getInMemoryCacheHitPercentage(Statistics statistics) {
		long hits = statistics.getInMemoryHits();
		long misses = getInMemoryMisses(statistics);
		long total = hits + misses;
		return getPercentage(hits, total);
	}

	/**
	 * Returns the percentage of all cache accesses that found a requested item cached in-memory.
	 *
	 * @return the percentage of successful hits from the MemoryStore
	 */
	public static double getCacheHitInMemoryPercentage(Statistics statistics) {
		long memoryHits = statistics.getInMemoryHits();
		long hits = statistics.getCacheHits();
		long misses = statistics.getCacheMisses();
		long total = hits + misses;
		return getPercentage(memoryHits, total);
	}

	/**
	 * Returns the percentage of off-heap cache accesses that found a requested item cached.
	 *
	 * @return the percentage of successful BigMemoryStore hits
	 * @since Ehcache 2.3
	 */
	public static double getOffHeapCacheHitPercentage(Statistics statistics) {
		long hits = getOffHeapHits(statistics);
		long misses = getOffHeapMisses(statistics);
		long total = hits + misses;
		return getPercentage(hits, total);
	}

	/**
	 * Returns the percentage of all cache accesses that found a requested item cached off-heap.
	 *
	 * @return the percentage of successful hits from the BigMemoryStore
	 * @since Ehcache 2.3
	 */
	public static double getCacheHitOffHeapPercentage(Statistics statistics) {
		long offHeapHits = getOffHeapHits(statistics);
		long hits = statistics.getCacheHits();
		long misses = statistics.getCacheMisses();
		long total = hits + misses;
		return getPercentage(offHeapHits, total);
	}

	/**
	 * Returns the percentage of disk cache accesses that found a requested item cached.
	 *
	 * @return the percentage of successful DiskStore hits
	 * @since Ehcache 2.3
	 */
	public static double getOnDiskCacheHitPercentage(Statistics statistics) {
		long hits = statistics.getOnDiskHits();
		long misses = getOnDiskMisses(statistics);
		long total = hits + misses;
		return getPercentage(hits, total);
	}

	/**
	 * Returns the percentage of all cache accesses that found a requested item cached on disk.
	 *
	 * @return the percentage of successful hits from the DiskStore
	 */
	public static double getCacheHitOnDiskPercentage(Statistics statistics) {
		long diskHits = statistics.getOnDiskHits();
		long hits = statistics.getCacheHits();
		long misses = statistics.getCacheMisses();
		long total = hits + misses;
		return getPercentage(diskHits, total);
	}

}
