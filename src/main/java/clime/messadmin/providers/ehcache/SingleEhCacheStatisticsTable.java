/**
 *
 */
package clime.messadmin.providers.ehcache;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;

import clime.messadmin.i18n.I18NSupport;
import clime.messadmin.providers.spi.BaseTabularDataProvider;
import clime.messadmin.utils.Longs;
import clime.messadmin.utils.StringUtils;

/**
 * Display Ehcache statistics
 * @author C&eacute;drik LIME
 */
class SingleEhCacheStatisticsTable extends BaseTabularDataProvider {
	private static final String BUNDLE_NAME = EhCacheStatistics.class.getName();

	/**
	 *
	 */
	public SingleEhCacheStatisticsTable() {
		super();
	}

	/** {@inheritDoc} */
	public StringBuffer getXHTMLApplicationData(StringBuffer buffer, ServletContext context, Ehcache ehCache) {
		String[] labels = getApplicationTabularDataLabels(context);
		Object[][] values = getApplicationTabularData(context, ehCache);
		String tableId = StringUtils.escapeXml("extraApplicationAttributesTable-"+getClass().getName()+'-'+ehCache.getCacheManager().getName()+'-'+ehCache.getName());
		buildXHTML(buffer, labels, values, tableId, getTableCaption(ehCache));
		return buffer;
	}

	protected String getTableCaption(Ehcache ehCache) {
		//FIXME add ajax links to: clearStatistics, flush/removeAll
		CacheConfiguration cacheConfiguration = ehCache.getCacheConfiguration();
		List argsTableCaption = new ArrayList();
		argsTableCaption.add(StringUtils.escapeXml(ehCache.getName()));
		argsTableCaption.add(cacheConfiguration.isEternal() ? Longs.valueOf(1) : Longs.valueOf(0));
		argsTableCaption.add(Longs.valueOf(cacheConfiguration.getTimeToLiveSeconds()));
		argsTableCaption.add(Longs.valueOf(cacheConfiguration.getTimeToIdleSeconds()));
		// disk persistence is only relevant if overflowToDisk is true
		argsTableCaption.add(cacheConfiguration.isOverflowToDisk() && cacheConfiguration.isDiskPersistent()
				? Longs.valueOf(1) : Longs.valueOf(0));
		String caption = I18NSupport.getLocalizedMessage(BUNDLE_NAME, I18NSupport.getAdminLocale(), "caption", argsTableCaption.toArray());//$NON-NLS-1$
		return caption;
	}

	public String[] getApplicationTabularDataLabels(ServletContext context) {
		return new String[] {
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.name"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.value"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.details")//$NON-NLS-1
		};
	}

	public Object[][] getApplicationTabularData(ServletContext context, Ehcache ehCache) {
		NumberFormat numberFormatter = NumberFormat.getNumberInstance(I18NSupport.getAdminLocale());
		Statistics ehStats = ehCache.getStatistics();
		CacheConfiguration cacheConfiguration = ehCache.getCacheConfiguration();
		List data = new LinkedList();

		// Start by adding some of the cache properties

		//data.add(new Object[] {"Guid", ehCache.getGuid(), null});
		String keyObjectCountDetails = "ObjectCount.details";//$NON-NLS-1$
		List argsObjectCountDetails = new ArrayList();
		String keyCacheHitsDetails = "CacheHits.details";//$NON-NLS-1$
		List argsCacheHitsDetails = new ArrayList();
		String keyCacheMissesDetails = "CacheMisses.details";//$NON-NLS-1$
		List argsCacheMissesDetails = new ArrayList();
		argsObjectCountDetails.add(Longs.valueOf(ehCache.getMemoryStoreSize()));//Ehcache 1.6: ==ehStats.getMemoryStoreObjectCount()
		argsObjectCountDetails.add(Longs.valueOf(cacheConfiguration.getMaxElementsInMemory()));
		argsCacheHitsDetails.add(Longs.valueOf(ehStats.getInMemoryHits()));
		if (EhcacheHelper.hasOffHeap) {
			if (! EhcacheHelper.isOverflowToOffHeap(cacheConfiguration)) {
				// additional information available, but BigMemory is disabled (no .withOffHeap key)
				keyCacheHitsDetails += ".23";
				keyCacheMissesDetails += ".23";
			}
			argsCacheHitsDetails.add(new Double(EhcacheHelper.getInMemoryCacheHitPercentage(ehStats)));
			argsCacheMissesDetails.add(Longs.valueOf(EhcacheHelper.getInMemoryMisses(ehStats)));
		}
		if (EhcacheHelper.hasOffHeap && EhcacheHelper.isOverflowToOffHeap(cacheConfiguration)) {
			keyObjectCountDetails += ".withOffHeap";//$NON-NLS-1$
			argsObjectCountDetails.add(Longs.valueOf(EhcacheHelper.getOffHeapStoreObjectCount(ehStats)));
			argsObjectCountDetails.add(EhcacheHelper.getMaxMemoryOffHeap(cacheConfiguration));
			argsCacheHitsDetails.add(Longs.valueOf(EhcacheHelper.getOffHeapHits(ehStats)));
			argsCacheHitsDetails.add(new Double(EhcacheHelper.getOffHeapCacheHitPercentage(ehStats)));
			argsCacheMissesDetails.add(Longs.valueOf(EhcacheHelper.getOffHeapMisses(ehStats)));
		}
		if (cacheConfiguration.isOverflowToDisk()) {
			keyObjectCountDetails += ".withDisk";//$NON-NLS-1$
			argsObjectCountDetails.add(Longs.valueOf(ehCache.getDiskStoreSize()));//Ehcache 1.6: ==ehStats.getDiskStoreObjectCount()
			argsObjectCountDetails.add(Longs.valueOf(cacheConfiguration.getMaxElementsOnDisk()));
			argsCacheHitsDetails.add(Longs.valueOf(ehStats.getOnDiskHits()));
			if (EhcacheHelper.hasOffHeap) {
				argsCacheHitsDetails.add(new Double(EhcacheHelper.getOnDiskCacheHitPercentage(ehStats)));
				argsCacheMissesDetails.add(Longs.valueOf(EhcacheHelper.getOnDiskMisses(ehStats)));
			}
		}

		data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "StatisticsAccuracy"),//$NON-NLS-1
				ehStats.getStatisticsAccuracyDescription(), null
		});

		data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "ObjectCount"),//$NON-NLS-1
				numberFormatter.format(ehStats.getObjectCount()),
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, keyObjectCountDetails,
						argsObjectCountDetails.toArray()
				)
		});

		// Only display statistics if those are enabled!
		if (EhcacheHelper.isStatisticsEnabled(ehCache)) {
			data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "CacheHits"),//$NON-NLS-1
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "CacheHits.value",
							new Object[] {Longs.valueOf(ehStats.getCacheHits()), new Double(EhcacheHelper.getCacheHitPercentage(ehStats))}),
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, keyCacheHitsDetails,
							argsCacheHitsDetails.toArray()
					)
			});
			data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "CacheMisses"),//$NON-NLS-1
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "CacheMisses.value",
							new Object[] {Longs.valueOf(ehStats.getCacheMisses()), new Double(EhcacheHelper.getCacheMissPercentage(ehStats))}),
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, keyCacheMissesDetails,
							argsCacheMissesDetails.toArray()
					)
			});
			try {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "EvictionCount"),//$NON-NLS-1
						numberFormatter.format(ehStats.getEvictionCount()), null
				});
			} catch (Throwable ignore) {
				// Ehcache < 1.4
			}
			try {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "AverageGetTime"),//$NON-NLS-1
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, "AverageGetTime.value", new Object[] {//$NON-NLS-1$
								new Float(ehStats.getAverageGetTime())
						}),
						null
				});
			} catch (Throwable ignore) {
				// Ehcache < 1.4
			}
			if (EhcacheHelper.hasSearch) {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "AverageSearchTime"),//$NON-NLS-1
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, "AverageSearchTime.value", new Object[] {//$NON-NLS-1$
								Longs.valueOf(EhcacheHelper.getAverageSearchTime(ehStats))
						}),
						null
				});
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, "SearchesPerSecond"),//$NON-NLS-1
						numberFormatter.format(EhcacheHelper.getSearchesPerSecond(ehStats)), null
				});
			}
		}

		Object[][] result = (Object[][]) data.toArray(new Object[data.size()][]);
		return result;
	}
}
