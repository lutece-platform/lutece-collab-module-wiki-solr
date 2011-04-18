/*
 * Copyright (c) 2002-2008, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.wiki.modules.solr.search;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.demo.html.HTMLParser;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.wiki.business.Topic;
import fr.paris.lutece.plugins.wiki.business.TopicHome;
import fr.paris.lutece.plugins.wiki.business.TopicVersion;
import fr.paris.lutece.plugins.wiki.business.TopicVersionHome;
import fr.paris.lutece.plugins.wiki.service.parser.LuteceWikiParser;
import fr.paris.lutece.portal.service.content.XPageAppService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;


/**
 * The Wiki indexer for Solr search platform
 *
 */
public class SolrWikiIndexer implements SolrIndexer
{
    private static final String PROPERTY_DESCRIPTION = "wiki-solr.indexer.description";
    private static final String PROPERTY_NAME = "wiki-solr.indexer.name";
    private static final String PROPERTY_VERSION = "wiki-solr.indexer.version";
    private static final String PROPERTY_INDEXER_ENABLE = "wiki-solr.indexer.enable";
    private static final String PARAMETER_PAGE_NAME = "page_name";
    private static final String PARAMETER_ACTION_VIEW = "view";
    private static final String PLUGIN_NAME = "wiki";
    public static final String PROPERTY_INDEXER_NAME = "wiki.indexer.name";
    private static final String PROPERTY_PAGE_PATH_LABEL = "wiki.pagePathLabel";
    public static final String SHORT_NAME_TOPIC = "wis";
    public static final String SHORT_NAME_TOPIC_CONTENT = "wic";
    private static final String PARAMETER_ACTION = "action";

    // Site name
    private static final String PROPERTY_SITE = "lutece.name";
    private static final String PROPERTY_PROD_URL = "lutece.prod.url";
    private String _strSite;
    private String _strProdUrl;

    public SolrWikiIndexer(  )
    {
        super(  );
        _strSite = AppPropertiesService.getProperty( PROPERTY_SITE );
        _strProdUrl = AppPropertiesService.getProperty( PROPERTY_PROD_URL );

        if ( !_strProdUrl.endsWith( "/" ) )
        {
            _strProdUrl = _strProdUrl + "/";
        }
    }

    public String getDescription(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    public String getName(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    public String getVersion(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }

    public boolean isEnable(  )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    public List<Field> getAdditionalFields(  )
    {
        return new ArrayList<Field>(  );
    }

    public Map<String, SolrItem> index(  )
    {
        Map<String, SolrItem> items = new HashMap<String, SolrItem>(  );

        Plugin plugin = PluginService.getPlugin( PLUGIN_NAME );

        for ( Topic topic : TopicHome.getTopicsList( plugin ) )
        {
            try
            {
                String strPortalUrl = AppPathService.getPortalUrl(  );

                if ( topic != null )
                {
                    UrlItem urlSubject = new UrlItem( _strProdUrl + strPortalUrl );
                    urlSubject.addParameter( XPageAppService.PARAM_XPAGE_APP,
                        AppPropertiesService.getProperty( PROPERTY_PAGE_PATH_LABEL ) );
                    urlSubject.addParameter( PARAMETER_PAGE_NAME, topic.getPageName(  ) );
                    urlSubject.addParameter( PARAMETER_ACTION, PARAMETER_ACTION_VIEW );

                    SolrItem docSubject = getDocument( topic, urlSubject.getUrl(  ), plugin );
                    items.put( getLog( docSubject ), docSubject );
                }
            }
            catch ( IOException e )
            {
                AppLogService.error( e );
            }
        }

        return items;
    }

    public List<SolrItem> getDocuments( String strDocument )
        throws IOException, InterruptedException, SiteMessageException
    {
        List<SolrItem> listDocs = new ArrayList<SolrItem>(  );
        String strPortalUrl = AppPathService.getPortalUrl(  );
        Plugin plugin = PluginService.getPlugin( PLUGIN_NAME );

        Topic topic = TopicHome.findByPrimaryKey( Integer.parseInt( strDocument ), plugin );

        if ( topic != null )
        {
            UrlItem urlSubject = new UrlItem( strPortalUrl );
            urlSubject.addParameter( XPageAppService.PARAM_XPAGE_APP,
                AppPropertiesService.getProperty( PROPERTY_PAGE_PATH_LABEL ) );
            urlSubject.addParameter( PARAMETER_PAGE_NAME, topic.getPageName(  ) );
            urlSubject.addParameter( PARAMETER_ACTION, PARAMETER_ACTION_VIEW );

            SolrItem docSubject = getDocument( topic, urlSubject.getUrl(  ), plugin );
            listDocs.add( docSubject );
        }

        return listDocs;
    }

    /**
     * Builds a {@link SolrItem} which will be used by Solr during the indexing of the topic
     * @param topic The topic to index
     * @param strUrl The url of the topic
     * @param plugin The plugin
     * @return The {@link SolrItem}
     * @throws IOException
     */
    private SolrItem getDocument( Topic topic, String strUrl, Plugin plugin )
        throws IOException
    {
        // make a new, empty item
        SolrItem item = new SolrItem(  );

        //Setting the Url field
        item.setUrl( strUrl );

        //Setting the Title field
        item.setTitle( topic.getPageName(  ) );

        //Setting the Uid field
        String strIdSubject = String.valueOf( topic.getPageName(  ) );
        item.setUid( strIdSubject + "_" + SHORT_NAME_TOPIC );

        //Setting the Content field
        TopicVersion latestTopicVersion = TopicVersionHome.findLastVersion( topic.getIdTopic(  ), plugin );
        String strWikiContent = "";

        if ( ( latestTopicVersion != null ) && ( latestTopicVersion.getWikiContent(  ) != null ) &&
                !latestTopicVersion.getWikiContent(  ).equals( "" ) )
        {
            strWikiContent = latestTopicVersion.getWikiContent(  );

            // Setting the Date field
            item.setDate( new Date( latestTopicVersion.getDateEdition(  ).getTime(  ) ) );
        }

        String strWikiResult = new LuteceWikiParser( strWikiContent ).toString(  );
        StringReader readerPage = new StringReader( strWikiResult );
        HTMLParser parser = new HTMLParser( readerPage );

        Reader reader = parser.getReader(  );
        int c;
        StringBuffer sb = new StringBuffer(  );

        while ( ( c = reader.read(  ) ) != -1 )
        {
            sb.append( String.valueOf( (char) c ) );
        }

        reader.close(  );

        item.setContent( sb.toString(  ) );

        //Setting the Title field
        item.setTitle( topic.getPageName(  ) );

        //Setting the Site field
        item.setSite( _strSite );

        // Setting the Type field
        item.setType( PLUGIN_NAME );

        // return the item
        return item;
    }

    /**
     * Generate the log line for the specified {@link SolrItem}
     * @param item The {@link SolrItem}
     * @return The string representing the log line
     */
    private String getLog( SolrItem item )
    {
        StringBuilder sbLogs = new StringBuilder(  );
        sbLogs.append( "indexing " );
        sbLogs.append( item.getType(  ) );
        sbLogs.append( " id : " );
        sbLogs.append( item.getUid(  ) );
        sbLogs.append( " Title : " );
        sbLogs.append( item.getTitle(  ) );
        sbLogs.append( "<br/>" );

        return sbLogs.toString(  );
    }
}
