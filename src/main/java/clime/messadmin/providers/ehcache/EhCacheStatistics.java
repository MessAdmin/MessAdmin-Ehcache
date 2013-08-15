/**
 *
 */
package clime.messadmin.providers.ehcache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import clime.messadmin.admin.AdminActionProvider;
import clime.messadmin.admin.BaseAdminActionWithContext;
import clime.messadmin.i18n.I18NSupport;
import clime.messadmin.model.Server;
import clime.messadmin.providers.spi.ApplicationDataProvider;
import clime.messadmin.utils.StringUtils;

/**
 * Display Ehcache statistics
 * Statistics are disabled by default since EhCache 2.1, so we let the user enable/disable them at runtime [EhCache 1.7].
 *
 * @author C&eacute;drik LIME
 */
public class EhCacheStatistics extends BaseAdminActionWithContext implements ApplicationDataProvider, AdminActionProvider {
	private static final String BUNDLE_NAME = EhCacheStatistics.class.getName();
	private static final SingleEhCacheStatisticsTable tableBuilder = new SingleEhCacheStatisticsTable();

	public static final String ACTION_ID = "ehCache";//$NON-NLS-1$
	public static final String PARAM_EHCACHE_ACTION_NAME         = "ehcacheAction";//$NON-NLS-1$
	public static final String EHCACHE_ACTION_REMOVE_ALL         = "removeAll";//$NON-NLS-1$
	public static final String EHCACHE_ACTION_CLEAR_STATS        = "clearStatistics";//$NON-NLS-1$
	public static final String EHCACHE_ACTION_SET_STATISTICS_ON  = "statsOn";//$NON-NLS-1$
	public static final String EHCACHE_ACTION_SET_STATISTICS_OFF = "statsOff";//$NON-NLS-1$
	public static final String PARAM_CACHE_MANAGER = "cacheManager";//$NON-NLS-1$
	public static final String PARAM_CACHE_GUID    = "ehCacheGUID";//$NON-NLS-1$

	/**
	 *
	 */
	public EhCacheStatistics() {
		super();
	}

	/** {@inheritDoc} */
	public String getApplicationDataTitle(ServletContext context) {
		final ClassLoader cl = Server.getInstance().getApplication(context).getApplicationInfo().getClassLoader();
		int nCaches = 0;
		int nManagers = 0;
		for (CacheManager cacheManager : (List<CacheManager>) CacheManager.ALL_CACHE_MANAGERS) {
			++nManagers;
			nCaches += cacheManager.getCacheNames().length;
		}
		return I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "title", new Object[] {//$NON-NLS-1$
				Integer.valueOf(nCaches), Integer.valueOf(nManagers)
		});
	}

	/** {@inheritDoc} */
	@Override
	public int getPriority() {
		return 300;
	}

	/** {@inheritDoc} */
	public String getXHTMLApplicationData(ServletContext context) {
		final ClassLoader cl = Server.getInstance().getApplication(context).getApplicationInfo().getClassLoader();
		StringBuffer result = new StringBuffer(512);
		for (CacheManager ehCacheManager : (List<CacheManager>) CacheManager.ALL_CACHE_MANAGERS) {
			if (result.length() > 0) {// 2nd+ CacheManager
				result.append("\n<hr/>\n");
			}
			result.append("<h3>");
			result.append(StringUtils.escapeXml(ehCacheManager.getName())).append(" (").append(ehCacheManager.getStatus().toString()).append(')');
			result.append("</h3>\n");
			String urlPrefixRemoveAll    = getUrlPrefix(context, ehCacheManager, EHCACHE_ACTION_REMOVE_ALL);
			String urlPrefixClearStats   = getUrlPrefix(context, ehCacheManager, EHCACHE_ACTION_CLEAR_STATS);
			String urlPrefixEnableStats  = getUrlPrefix(context, ehCacheManager, EHCACHE_ACTION_SET_STATISTICS_ON);
			String urlPrefixDisableStats = getUrlPrefix(context, ehCacheManager, EHCACHE_ACTION_SET_STATISTICS_OFF);
			if (EhcacheHelper.hasStatistics) { // No link if the capability is not available (Ehcache < 1.7)!
				result.append(buildActionLink(urlPrefixDisableStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.disableStats"), this));//$NON-NLS-1$
				result.append("&nbsp;|&nbsp;");
				result.append(buildActionLink(urlPrefixEnableStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.enableStats"), this));//$NON-NLS-1$
				result.append("&nbsp;|&nbsp;");
			}
			result.append(buildActionLink(urlPrefixClearStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.clearStatistics"), this));//$NON-NLS-1$
			result.append("&nbsp;|&nbsp;");
			result.append(buildActionLink(urlPrefixRemoveAll, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.removeAll"),//$NON-NLS-1$
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.removeAll.confirmJS"), this));//$NON-NLS-1$
			result.append("<br />\n");
			urlPrefixRemoveAll    += '&' + PARAM_CACHE_GUID + '=';
			urlPrefixClearStats   += '&' + PARAM_CACHE_GUID + '=';
			urlPrefixEnableStats  += '&' + PARAM_CACHE_GUID + '=';
			urlPrefixDisableStats += '&' + PARAM_CACHE_GUID + '=';
			String[] ehCacheNames = ehCacheManager.getCacheNames();
			boolean firstCache = true;
			for (int i = 0; i < ehCacheNames.length; ++i) {
				String ehCacheName = ehCacheNames[i];
				Ehcache ehCache = ehCacheManager.getEhcache(ehCacheName);
				if (firstCache) {
					firstCache = false;
				} else {
					result.append("<br />\n");
				}
				// Display the current statistics
				tableBuilder.getXHTMLApplicationData(result, context, ehCache);
				if (EhcacheHelper.hasStatistics) { // No link if the capability is not available (Ehcache < 1.7)!
					if (EhcacheHelper.isStatisticsEnabled(ehCache)) {
						String urlDisableStats = urlPrefixDisableStats + urlEncodeUTF8(ehCache.getGuid());
						result.append(buildActionLink(urlDisableStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.disableStats"), this));//$NON-NLS-1$
						result.append("&nbsp;|&nbsp;");
					} else {
						String urlEnableStats = urlPrefixEnableStats + urlEncodeUTF8(ehCache.getGuid());
						result.append(buildActionLink(urlEnableStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.enableStats"), this));//$NON-NLS-1$
						result.append("&nbsp;|&nbsp;");
					}
				}
				String urlClearStats = urlPrefixClearStats + urlEncodeUTF8(ehCache.getGuid());
				result.append(buildActionLink(urlClearStats, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.clearStatistics"), this));//$NON-NLS-1$
				result.append("&nbsp;|&nbsp;");
				String urlRemoveAll = urlPrefixRemoveAll + urlEncodeUTF8(ehCache.getGuid());
				result.append(buildActionLink(urlRemoveAll, I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.removeAll"),//$NON-NLS-1$
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, cl, "action.removeAll.confirmJS"), this));//$NON-NLS-1$
			}
		}
		result.append('\n');
		return result.toString();
	}

	protected String getUrlPrefix(ServletContext context, CacheManager ehCacheManager, String subAction) {
		String urlPrefix = new StringBuilder().append('?').append(ACTION_PARAMETER_NAME).append('=').append(getActionID())
			.append('&').append(PARAM_EHCACHE_ACTION_NAME).append('=').append(subAction)
			.append('&').append(CONTEXT_KEY).append('=').append(urlEncodeUTF8(Server.getInstance().getApplication(context).getApplicationInfo().getInternalContextPath()))
			.append('&').append(PARAM_CACHE_MANAGER).append('=').append(urlEncodeUTF8(ehCacheManager.getName())).toString();
		return urlPrefix;
	}

	/***********************************************************************/

	/** {@inheritDoc} */
	public String getActionID() {
		return ACTION_ID;
	}

	protected void displayXHTMLApplicationData(HttpServletRequest request, HttpServletResponse response, String context) throws ServletException, IOException {
		// ensure we get a GET
		if (METHOD_POST.equals(request.getMethod())) {
			sendRedirect(request, response);
			return;
		}
		// display a listing of all caches
		String data = getXHTMLApplicationData(getServletContext(context));
		setNoCache(response);
		PrintWriter out = response.getWriter();
		out.print(data);
		out.flush();
		out.close();
	}

	/** {@inheritDoc} */
	@Override
	public void serviceWithContext(HttpServletRequest request, HttpServletResponse response, String context) throws ServletException, IOException {
		String ehcacheAction = request.getParameter(PARAM_EHCACHE_ACTION_NAME);
		if (StringUtils.isBlank(ehcacheAction)) {
			displayXHTMLApplicationData(request, response, context);
			return;
		}
		String cacheManagerName = request.getParameter(PARAM_CACHE_MANAGER);
		if (StringUtils.isBlank(cacheManagerName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, PARAM_CACHE_MANAGER + " parameter is required");
			return;
		} // else
		// get Ehcache to modify
		CacheManager cacheManager = null;
		Iterator<CacheManager> cacheManagers = CacheManager.ALL_CACHE_MANAGERS.iterator();
		while (cacheManagers.hasNext()) {
			CacheManager cacheManagerTest = cacheManagers.next();
			if (cacheManagerName.equals(cacheManagerTest.getName())) {
				cacheManager = cacheManagerTest;
				break;
			}
		}
		if (cacheManager == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can not find CacheManager named " + cacheManagerName);
			return;
		}
		Ehcache cache = null;
		String cacheGUID = request.getParameter(PARAM_CACHE_GUID);
		if (StringUtils.isNotBlank(cacheGUID)) {
			for (String cacheName : cacheManager.getCacheNames()) {
				Ehcache cacheTest = cacheManager.getCache(cacheName);
				if (cacheTest != null && cacheGUID.equals(cacheTest.getGuid())) {
					cache = cacheTest;
					break;
				}
			}
			if (cache == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can not find Cache of GUID " + cacheGUID + " for CacheManager " + cacheManagerName);
				return;
			}
		}
		// Note the cache can be null here, as the PARAM_CACHE_GUID is not mandatory!
		if (EHCACHE_ACTION_REMOVE_ALL.equalsIgnoreCase(ehcacheAction)) {
			//cache.remove("");
			//cache.evictExpiredElements();
			if (cache != null) {
				cache.removeAll();
			} else {
//				// do not allow to remove all cache elements for all CacheManager's caches
//				response.sendError(HttpServletResponse.SC_NOT_FOUND, PARAM_CACHE_GUID + " parameter is required");
//				return;
				cacheManager.clearAll();
			}
		} else if (EHCACHE_ACTION_CLEAR_STATS.equalsIgnoreCase(ehcacheAction)) {
			if (cache != null) {
				cache.clearStatistics();
			} else {
				for (String cacheName : cacheManager.getCacheNames()) {
					Ehcache cache2 = cacheManager.getCache(cacheName);
					if (cache2 != null) {
						cache2.clearStatistics();
					}
				}
			}
		} else if (EHCACHE_ACTION_SET_STATISTICS_ON.equalsIgnoreCase(ehcacheAction)) {
			if (cache != null) {
				EhcacheHelper.setStatisticsEnabled(cache, true);
			} else {
				for (String cacheName : cacheManager.getCacheNames()) {
					Ehcache cache2 = cacheManager.getCache(cacheName);
					if (cache2 != null) {
						EhcacheHelper.setStatisticsEnabled(cache2, true);
					}
				}
			}
		} else if (EHCACHE_ACTION_SET_STATISTICS_OFF.equalsIgnoreCase(ehcacheAction)) {
			if (cache != null) {
				EhcacheHelper.setStatisticsEnabled(cache, false);
			} else {
				for (String cacheName : cacheManager.getCacheNames()) {
					Ehcache cache2 = cacheManager.getCache(cacheName);
					if (cache2 != null) {
						EhcacheHelper.setStatisticsEnabled(cache2, false);
					}
				}
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, ehcacheAction + " value for parameter " + PARAM_EHCACHE_ACTION_NAME + " is unknown");
			return;
		}
		displayXHTMLApplicationData(request, response, context);
	}
}
