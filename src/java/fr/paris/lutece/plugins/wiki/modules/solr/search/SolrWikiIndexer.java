/*
 * Copyright (c) 2002-2011, Mairie de Paris
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
import java.util.List;

import org.apache.lucene.demo.html.HTMLParser;

import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.search.solr.util.SolrConstants;
import fr.paris.lutece.plugins.wiki.business.Topic;
import fr.paris.lutece.plugins.wiki.business.TopicHome;
import fr.paris.lutece.plugins.wiki.business.TopicVersion;
import fr.paris.lutece.plugins.wiki.business.TopicVersionHome;
import fr.paris.lutece.plugins.wiki.service.parser.LuteceWikiParser;
import fr.paris.lutece.portal.service.content.XPageAppService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
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

    private static final String TOPIC_INDEXATION_ERROR = "[SolrWikiIndexer] An error occured during the indexation of the topic number ";
    
    // Indexed resource type name
    public static final String CONSTANT_TYPE_RESOURCE = "WIKI_TOPIC";

    // Site name
    private static final List<String> LIST_RESSOURCES_NAME = new ArrayList<String>(  );

    public SolrWikiIndexer(  )
    {
        super(  );

        LIST_RESSOURCES_NAME.add( CONSTANT_TYPE_RESOURCE );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    /**
     * {@inheritDoc}
     */
    public String getName(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnable(  )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    /**
     * {@inheritDoc}
     */
    public List<Field> getAdditionalFields(  )
    {
        return new ArrayList<Field>(  );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> indexDocuments(  )
    {
        Plugin plugin = PluginService.getPlugin( PLUGIN_NAME );

        List<String> lstErrors = new ArrayList<String>(  );
        
        for ( Topic topic : TopicHome.getTopicsList( plugin ) )
        {
            if ( topic != null )
            {
            	try
            	{
            		UrlItem urlSubject = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
            		urlSubject.addParameter( XPageAppService.PARAM_XPAGE_APP,
            				AppPropertiesService.getProperty( PROPERTY_PAGE_PATH_LABEL ) );
            		urlSubject.addParameter( PARAMETER_PAGE_NAME, topic.getPageName(  ) );
            		urlSubject.addParameter( PARAMETER_ACTION, PARAMETER_ACTION_VIEW );

            		SolrItem docSubject = getDocument( topic, urlSubject.getUrl(  ), plugin );

            		if ( docSubject != null )
            		{
            			SolrIndexerService.write( docSubject );
            		}
            	}
            	catch ( IOException e )
            	{
            		lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
            		AppLogService.error( TOPIC_INDEXATION_ERROR + topic.getIdTopic(  ), e );
            	}
            }
        }
        
        return lstErrors;
    }

    /**
     * {@inheritDoc}
     */
    public List<SolrItem> getDocuments( String strDocument )
    {
        List<SolrItem> listDocs = new ArrayList<SolrItem>(  );
        Plugin plugin = PluginService.getPlugin( PLUGIN_NAME );

        Topic topic = TopicHome.findByPrimaryKey( Integer.parseInt( strDocument ), plugin );

        if ( topic != null )
        {
            UrlItem urlSubject = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
            urlSubject.addParameter( XPageAppService.PARAM_XPAGE_APP,
                AppPropertiesService.getProperty( PROPERTY_PAGE_PATH_LABEL ) );
            urlSubject.addParameter( PARAMETER_PAGE_NAME, topic.getPageName(  ) );
            urlSubject.addParameter( PARAMETER_ACTION, PARAMETER_ACTION_VIEW );

            SolrItem docSubject;

            try
            {
                docSubject = getDocument( topic, urlSubject.getUrl(  ), plugin );
                listDocs.add( docSubject );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
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
        item.setUid( getResourceUid( strIdSubject, CONSTANT_TYPE_RESOURCE ) );

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
        item.setSite( SolrIndexerService.getWebAppName(  ) );

        // Setting the Type field
        item.setType( PLUGIN_NAME );

        // return the item
        return item;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceUid( String strResourceId, String strResourceType )
    {
        StringBuffer sb = new StringBuffer( strResourceId );
        sb.append( SolrConstants.CONSTANT_UNDERSCORE ).append( SHORT_NAME_TOPIC );

        return null;
    }

    /**
    * {@inheritDoc}
    */
    public List<String> getResourcesName(  )
    {
        return LIST_RESSOURCES_NAME;
    }
}
