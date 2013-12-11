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
		final ClassLoader cl = I18NSupport.getClassLoader(context);
		String[] labels = getApplicationTabularDataLabels(context);
		Object[][] values = getApplicationTabularData(context, ehCache);
		String tableId = StringUtils.escapeXml("extraApplicationAttributesTable-"+getClass().getName()+'-'+ehCache.getCacheManager().getName()+'-'+ehCache.getName());
		buildXHTML(buffer, labels, values, tableId, getTableCaption(ehCache, cl));
		return buffer;
	}

	protected String getTableCaption(Ehcache ehCache, ClassLoader cl) {
		//FIXME add ajax links to: clearStatistics, flush/removeAll
		CacheConfiguration cacheConfiguration = ehCache.getCacheConfiguration();
		List<Object> argsTableCaption = new ArrayList<Object>();
		argsTableCaption.add(StringUtils.escapeXml(ehCache.getName()));
		argsTableCaption.add(cacheConfiguration.isEternal() ? Long.valueOf(1) : Long.valueOf(0));
		argsTableCaption.add(Long.valueOf(cacheConfiguration.getTimeToLiveSeconds()));
		argsTableCaption.add(Long.valueOf(cacheConfiguration.getTimeToIdleSeconds()));
		// disk persistence is only relevant if overflowToDisk is true
		argsTableCaption.add(cacheConfiguration.isOverflowToDisk() && cacheConfiguration.isDiskPersistent()
				? Long.valueOf(1) : Long.valueOf(0));
		String caption = I18NSupport.getLocalizedMessage(BUNDLE_NAME, I18NSupport.getAdminLocale(), cl, "caption", argsTableCaption.toArray());//$NON-NLS-1$
		return caption;
	}

	public String[] getApplicationTabularDataLabels(ServletContext context) {
		final ClassLoader cl = I18NSupport.getClassLoader(context);
		return new String[] {
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "label.name"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "label.value"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "label.details")//$NON-NLS-1
		};
	}

	public Object[][] getApplicationTabularData(ServletContext context, Ehcache ehCache) {
		final ClassLoader cl = I18NSupport.getClassLoader(context);
		NumberFormat numberFormatter = NumberFormat.getNumberInstance(I18NSupport.getAdminLocale());
		Statistics ehStats = ehCache.getStatistics();
		CacheConfiguration cacheConfiguration = ehCache.getCacheConfiguration();
		List<Object> data = new LinkedList<Object>();

		// Start by adding some of the cache properties

		//data.add(new Object[] {"Guid", ehCache.getGuid(), null});
		String keyObjectCountDetails = "ObjectCount.details";//$NON-NLS-1$
		List<Object> argsObjectCountDetails = new ArrayList<Object>();
		String keyCacheHitsDetails = "CacheHits.details";//$NON-NLS-1$
		List<Object> argsCacheHitsDetails = new ArrayList<Object>();
		String keyCacheMissesDetails = "CacheMisses.details";//$NON-NLS-1$
		List<Object> argsCacheMissesDetails = new ArrayList<Object>();
		argsObjectCountDetails.add(Long.valueOf(ehCache.getMemoryStoreSize()));//Ehcache 1.6: ==ehStats.getMemoryStoreObjectCount()
		argsObjectCountDetails.add(Long.valueOf(cacheConfiguration.getMaxElementsInMemory()));
		argsCacheHitsDetails.add(Long.valueOf(ehStats.getInMemoryHits()));
		if (EhcacheHelper.hasOffHeap) {
			if (! EhcacheHelper.isOverflowToOffHeap(cacheConfiguration)) {
				// additional information available, but BigMemory is disabled (no .withOffHeap key)
				keyCacheHitsDetails += ".23";
				keyCacheMissesDetails += ".23";
			}
			argsCacheHitsDetails.add(new Double(EhcacheHelper.getInMemoryCacheHitPercentage(ehStats)));
			argsCacheMissesDetails.add(Long.valueOf(EhcacheHelper.getInMemoryMisses(ehStats)));
		}
		if (EhcacheHelper.hasOffHeap && EhcacheHelper.isOverflowToOffHeap(cacheConfiguration)) {
			keyObjectCountDetails += ".withOffHeap";//$NON-NLS-1$
			argsObjectCountDetails.add(Long.valueOf(EhcacheHelper.getOffHeapStoreObjectCount(ehStats)));
			argsObjectCountDetails.add(EhcacheHelper.getMaxMemoryOffHeap(cacheConfiguration));
			argsCacheHitsDetails.add(Long.valueOf(EhcacheHelper.getOffHeapHits(ehStats)));
			argsCacheHitsDetails.add(new Double(EhcacheHelper.getOffHeapCacheHitPercentage(ehStats)));
			argsCacheMissesDetails.add(Long.valueOf(EhcacheHelper.getOffHeapMisses(ehStats)));
		}
		if (cacheConfiguration.isOverflowToDisk()) {
			keyObjectCountDetails += ".withDisk";//$NON-NLS-1$
			argsObjectCountDetails.add(Long.valueOf(ehCache.getDiskStoreSize()));//Ehcache 1.6: ==ehStats.getDiskStoreObjectCount()
			argsObjectCountDetails.add(Long.valueOf(cacheConfiguration.getMaxElementsOnDisk()));
			argsCacheHitsDetails.add(Long.valueOf(ehStats.getOnDiskHits()));
			if (EhcacheHelper.hasOffHeap) {
				argsCacheHitsDetails.add(new Double(EhcacheHelper.getOnDiskCacheHitPercentage(ehStats)));
				argsCacheMissesDetails.add(Long.valueOf(EhcacheHelper.getOnDiskMisses(ehStats)));
			}
		}

		data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "StatisticsAccuracy"),//$NON-NLS-1
				ehStats.getStatisticsAccuracyDescription(), null
		});

		data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "ObjectCount"),//$NON-NLS-1
				numberFormatter.format(ehStats.getObjectCount()),
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, keyObjectCountDetails,
						argsObjectCountDetails.toArray()
				)
		});

		// Only display statistics if those are enabled!
		if (EhcacheHelper.isStatisticsEnabled(ehCache)) {
			data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "CacheHits"),//$NON-NLS-1
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "CacheHits.value",
							Long.valueOf(ehStats.getCacheHits()), new Double(EhcacheHelper.getCacheHitPercentage(ehStats))),
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, keyCacheHitsDetails,
							argsCacheHitsDetails.toArray()
					)
			});
			data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "CacheMisses"),//$NON-NLS-1
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "CacheMisses.value",
							Long.valueOf(ehStats.getCacheMisses()), new Double(EhcacheHelper.getCacheMissPercentage(ehStats))),
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, keyCacheMissesDetails,
							argsCacheMissesDetails.toArray()
					)
			});
			try {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "EvictionCount"),//$NON-NLS-1
						numberFormatter.format(ehStats.getEvictionCount()), null
				});
			} catch (Throwable ignore) {
				// Ehcache < 1.4
			}
			try {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "AverageGetTime"),//$NON-NLS-1
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "AverageGetTime.value",//$NON-NLS-1$
								new Float(ehStats.getAverageGetTime())
						),
						null
				});
			} catch (Throwable ignore) {
				// Ehcache < 1.4
			}
			if (EhcacheHelper.hasSearch) {
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "AverageSearchTime"),//$NON-NLS-1
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "AverageSearchTime.value",//$NON-NLS-1$
								Long.valueOf(EhcacheHelper.getAverageSearchTime(ehStats))
						),
						null
				});
				data.add(new Object[] {I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "SearchesPerSecond"),//$NON-NLS-1
						numberFormatter.format(EhcacheHelper.getSearchesPerSecond(ehStats)), null
				});
			}
		}

		Object[][] result = data.toArray(new Object[data.size()][]);
		return result;
	}
}
