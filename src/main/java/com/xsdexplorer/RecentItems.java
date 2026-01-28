package com.xsdexplorer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * A simple data structure to store recent items (e.g. recent file in a menu or
 * recent search text in a search dialog).
 * 
 * @author djemili
 */
public class RecentItems
{
    public interface RecentItemsObserver
    {
        void onRecentItemChange(RecentItems src);
    }
    
    public final static String RECENT_ITEM_STRING = "recent.item.";
    
    private int                       m_maxItems;
    private Preferences               m_prefNode;

    //private List<String>              m_items            = new ArrayList<String>();
    private List<RecentItemsObserver> m_observers        = new ArrayList<RecentItemsObserver>();
    private ArrayDeque<String> m_items = new ArrayDeque<>();

    
    public RecentItems(int maxItems, Preferences prefNode)
    {
        m_maxItems = maxItems;
        m_prefNode = prefNode;
        
        loadFromPreferences();
    }
    
    public void push(String item)
    {
        m_items.remove(item);
        m_items.addFirst(item);
        
        if (m_items.size() > m_maxItems)
        {
            m_items.removeLast();
        }
        
        update();
    }
    
    public void remove(Object item)
    {
        m_items.remove(item);
        update();
    }
    
    
    public List<String> getItems()
    {
        return m_items.stream().toList();
    }
    
    public int size()
    {
        return m_items.size();
    }
    
    public void addObserver(RecentItemsObserver observer)
    {
        m_observers.add(observer);
    }
    
    public void removeObserver(RecentItemsObserver observer)
    {
        m_observers.remove(observer);
    }
    
    private void update()
    {
        for (RecentItemsObserver observer : m_observers)
        {
            observer.onRecentItemChange(this);
        }
        
        storeToPreferences();
    }
    
    private void loadFromPreferences()
    {
        // load recent files from properties
        for (int i = 0; i < m_maxItems; i++)
        {
            String val = m_prefNode.get(RECENT_ITEM_STRING+i, ""); 

            if (!val.equals("")) 
            {
                m_items.add(val);
            }
            else
            {
                break;
            }
        }
    }
    
    private void storeToPreferences()
    {
    	int i = 0;
    	for (String s : m_items) {
            m_prefNode.put(RECENT_ITEM_STRING+i, s);
            i++;
    	}
    	for ( ; i < m_maxItems; ++i) {
    		m_prefNode.remove(RECENT_ITEM_STRING+i);
    	}
    }
}
