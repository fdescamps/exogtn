/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.webui.application;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Random;

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.application.registry.Application;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.Properties;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.webui.container.UIDashboardLayoutContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortalComponentActionListener.DeleteComponentActionListener;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SAS Author : dang.tung tungcnw@gmail.com May 06,
 * 2008
 */
@ComponentConfig(template = "system:/groovy/portal/webui/application/UIGadget.gtmpl", events = {
   @EventConfig(listeners = UIGadget.SaveUserPrefActionListener.class),
   @EventConfig(listeners = UIGadget.SetNoCacheActionListener.class),
   @EventConfig(listeners = UIGadget.SetDebugActionListener.class),
   @EventConfig(name = "DeleteGadget", confirm="UIGadgetContainerManagement.confirm.DeleteGadget", listeners = UIGadget.DeleteGadgetActionListener.class)
})
/**
 * This class represents user interface gadgets, it using UIGadget.gtmpl for
 * rendering UI in eXo. It mapped to Application model in page or container.
 */
public class UIGadget extends UIWindow<org.exoplatform.portal.pom.spi.gadget.Gadget>
{

   /** . */
   private ApplicationState<org.exoplatform.portal.pom.spi.gadget.Gadget> state;

   /** . */
   private String gadgetId;

   private Properties properties_;

   private JSONObject metadata_;

   private String url_;

   private GadgetRegistryService gadgetRegistryService = null;

   public static final String PREF_KEY = "_pref_gadget_";

   public static final String PREF_NO_CACHE = "_pref_no_cache_";

   public static final String PREF_DEBUG = "_pref_debug_";

   public static final String HOME_VIEW = "home";

   public static final String CANVAS_VIEW = "canvas"; 
   
   public static final String METADATA_GADGETS = "gadgets";
   
   public static final String METADATA_USERPREFS = "userPrefs";
   
   public static final String METADATA_MODULEPREFS = "modulePrefs";
   
   public static final String RPC_RESULT = "result";
   
   public static final String METADATA_USERPREFS_TYPE = "dataType";
   
   public static final String METADATA_USERPREFS_TYPE_HIDDEN = "hidden";
   
   public static final String METADATA_USERPREFS_TYPE_LIST = "list";

   public String view = HOME_VIEW;
   
   public static String SAVE_PREF_FAIL = "UIGadget.savePrefFail";

   /**
    * Initializes a newly created <code>UIGadget</code> object
    * 
    * @throws Exception if can't initialize object
    */
   public UIGadget()
   {
      super();
   }

   public String getId()
   {
      return storageName;
   }

   public String getStandaloneURL()
   {
      PortalRequestContext portalRC = Util.getPortalRequestContext();
      HttpServletRequest request = portalRC.getRequest();
      StringBuilder urlBuilder = new StringBuilder(request.getScheme());
      urlBuilder.append("://").append(request.getServerName()).append(":").append(request.getServerPort());
      urlBuilder.append(request.getContextPath()).append("/standalone/").append(storageId);

      HttpServletResponse response = portalRC.getResponse();
      return response.encodeURL(urlBuilder.toString());
   }

   public ApplicationState<org.exoplatform.portal.pom.spi.gadget.Gadget> getState()
   {
      return state;
   }

   @Override
   public void initApplicationState(Application registryModel)
   {
      ApplicationState<org.exoplatform.portal.pom.spi.gadget.Gadget> appState = new TransientApplicationState<org.exoplatform.portal.pom.spi.gadget.Gadget>(registryModel.getApplicationName());
      setState(appState);
   }

   @Override
   public Class<? extends UIWindowForm> getFormType()
   {
      return UIGadgetForm.class;
   }

   public void setState(ApplicationState<org.exoplatform.portal.pom.spi.gadget.Gadget> state)
   {
      if (state != null)
      {
         try
         {
            DataStorage ds = getApplicationComponent(DataStorage.class);
            String gadgetId = ds.getId(state);

            //
            this.gadgetId = gadgetId;
            this.state = state;
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
      else
      {
         this.gadgetId = null;
         this.state = null;
      }

   }

   /**
    * Gets name of gadget application
    * 
    * @return the string represents name of gadget application
    */
   public String getApplicationName()
   {
      return gadgetId;
   }

   /**
    * Gets Properties of gadget application such as locationX, locationY in
    * desktop page
    * 
    * @return all properties of gadget application
    * @see org.exoplatform.portal.config.model.Application
    * @see org.exoplatform.portal.config.model.Properties
    */
   public Properties getProperties()
   {
      if (properties_ == null)
         properties_ = new Properties();
      return properties_;
   }

   /**
    * Sets Properties of gadget application such as locationX, locationY in
    * desktop page
    * 
    * @param properties Properties that is the properties of gadget application
    * @see org.exoplatform.portal.config.model.Properties
    * @see org.exoplatform.portal.config.model.Application
    */
   public void setProperties(Properties properties)
   {
      this.properties_ = properties;
   }

   @Deprecated
   public String getMetadata()
   {
      try
      {
         if (metadata_ == null)
         {
            String strMetadata = GadgetUtil.fetchGagdetRpcMetadata(getUrl());
            metadata_ = new JSONObject(strMetadata);
         }
         JSONObject obj = metadata_.getJSONArray(METADATA_GADGETS).getJSONObject(0);
         String token = GadgetUtil.createToken(this.getUrl(), new Random().nextLong());
         obj.put("secureToken", token);
         return metadata_.toString();
      }
      catch (JSONException e)
      {
         return null;
      }
   }
   
   public String getRpcMetadata()
   {
      try
      {
         if (metadata_ == null)
         {
        	String gadgetUrl = getUrl();
            String strMetadata = GadgetUtil.fetchGagdetRpcMetadata(gadgetUrl);
            metadata_ = new JSONArray(strMetadata).getJSONObject(0).getJSONObject(UIGadget.RPC_RESULT).getJSONObject(gadgetUrl);
         }
         String token = GadgetUtil.createToken(this.getUrl(), new Random().nextLong());
         metadata_.put("secureToken", token);
         return metadata_.toString();
      }
      catch (JSONException e)
      {
         return null;
      }
   }
   
   /**
    * Check if content of gadget has <UserPref>? (Content is parsed from gadget specification in .xml file)
    * @return boolean
    */
   public boolean isSettingUserPref()
   {
      try
      {
         if(metadata_ != null)
         {
            JSONObject userPrefs = metadata_.getJSONObject(METADATA_USERPREFS);
            JSONArray names = userPrefs.names();
            int count = names.length();
            if(count > 0)
            {
               for(int i = 0; i < count; i++)
               {
                  JSONObject o = (JSONObject) userPrefs.get(names.get(i).toString());
                  if(!(o.get(METADATA_USERPREFS_TYPE).equals(METADATA_USERPREFS_TYPE_HIDDEN) || 
                        o.get(METADATA_USERPREFS_TYPE).equals(METADATA_USERPREFS_TYPE_LIST)))
                     return true;
               }
               return false;
            }
         }
         return false;
      }
      catch (Exception e)
      {
         return false;
      }
   }

   @Override
   public boolean isRendered()
   {
      try
      {
         DataStorage service = getApplicationComponent(DataStorage.class);
         service.load(state, ApplicationType.GADGET);
         if (getApplication() == null)
         {
            throw new Exception();
         }
      }
      catch (Exception e)
      {
         return false;
      }
      return super.isRendered();
   }

   public boolean isLossData()
   {
      try
      {
         DataStorage service = getApplicationComponent(DataStorage.class);
         service.load(state, ApplicationType.GADGET);
         if (getApplication() == null)
         {
            throw new Exception();
         }
      }
      catch (Exception e)
      {
         return true;
      }
      return false;
   }

   /**
    * Gets GadgetApplication by GadgedRegistryService
    * 
    * @return Gadget Application
    * @throws Exception
    */
   private Gadget getApplication()
   {
      try
      {
         GadgetRegistryService gadgetService = getApplicationComponent(GadgetRegistryService.class);
         return gadgetService.getGadget(gadgetId);
      }
      catch (Exception ex)
      {
         return null;
      }
   }

   /**
    * Gets Url of gadget application, it saved before by GadgetRegistryService
    * 
    * @return url of gadget application, such as
    *         "http://www.google.com/ig/modules/horoscope.xml"
    */
   public String getUrl()
   {
      if (url_ == null)
      {
         Gadget gadget = getApplication();
         url_ = GadgetUtil.reproduceUrl(gadget.getUrl(), gadget.isLocal());
      }
      return url_;
   }

   private GadgetRegistryService getGadgetRegistryService()
   {
      if (gadgetRegistryService == null)
         gadgetRegistryService =
            (GadgetRegistryService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(
               GadgetRegistryService.class);
      return gadgetRegistryService;
   }

   public boolean isNoCache()
   {
      if(PropertyManager.isDevelopping())
         return true;
      return false;
   }

   public void setNoCache(boolean value)
   {
   }

   public boolean isDebug()
   {
      if(PropertyManager.isDevelopping())
         return true;
      return false;
   }

   public void setDebug(boolean value)
   {
   }

   public boolean isGadgetDeveloper()
   {
      return getGadgetRegistryService().isGadgetDeveloper(Util.getPortalRequestContext().getRemoteUser());
   }

   public String getView()
   {
      if (view != null)
         return view;
      return HOME_VIEW;
   }

   public void setView(String view)
   {
      this.view = view;
   }

   public boolean showCloseButton()
   {
      return getAncestorOfType(UIDashboardLayoutContainer.class) != null;
   }
   
   /**
    * Gets user preference of gadget application
    * 
    * @return the string represents user preference of gadget application
    * @throws Exception when can't convert object to string
    */
   public String getUserPref() throws Exception
   {
      DataStorage service = getApplicationComponent(DataStorage.class);
      org.exoplatform.portal.pom.spi.gadget.Gadget pp = service.load(state, ApplicationType.GADGET);
      return pp != null ? pp.getUserPref() : null;
   }

   public void addUserPref(String addedUserPref) throws Exception
   {
      DataStorage service = getApplicationComponent(DataStorage.class);
      org.exoplatform.portal.pom.spi.gadget.Gadget gadget = new org.exoplatform.portal.pom.spi.gadget.Gadget();

      //
      gadget.addUserPref(addedUserPref);

      //
      state = service.save(state, gadget);

      // WARNING :
      // This is used to force a state save and it should not be copied else where to make things
      // convenient as this could lead to a severe performance degradation
      ModelDataStorage mds = getApplicationComponent(ModelDataStorage.class);
      mds.save();
   }

   /**
    * Initializes a newly created <code>SaveUserPrefActionListener</code>
    * object
    */
   static public class SaveUserPrefActionListener extends EventListener<UIGadget>
   {
      public void execute(Event<UIGadget> event) throws Exception
      {
         UIGadget uiGadget = event.getSource();
         WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
         
         //
         try
         {
            uiGadget.addUserPref(event.getRequestContext().getRequestParameter("userPref"));
            Util.getPortalRequestContext().setResponseComplete(true);
         } 
         catch(Exception e)
         {                        
            UIPortletApplication uiPortlet = uiGadget.getAncestorOfType(UIPortletApplication.class);
            context.addUIComponentToUpdateByAjax(uiPortlet);
            context.setAttribute(UIGadget.SAVE_PREF_FAIL, true);
            throw new MessageException(new ApplicationMessage("UIDashboard.msg.ApplicationNotExisted", null, ApplicationMessage.ERROR));
         }

         //
         if (uiGadget.isLossData())
         {
            /*
            UIPortalApplication uiApp = Util.getUIPortalApplication();
            uiApp.addMessage(new ApplicationMessage("UIDashboard.msg.ApplicationNotExisted", null));
            PortalRequestContext pcontext = Util.getPortalRequestContext();
            pcontext.addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            */
            return;
         }

         //event.getRequestContext().setResponseComplete(true);
      }
   }

   static public class SetNoCacheActionListener extends EventListener<UIGadget>
   {
      public void execute(Event<UIGadget> event) throws Exception
      {
         /*
          * String noCache =
          * event.getRequestContext().getRequestParameter("nocache") ; UIGadget
          * uiGadget = event.getSource() ;
          * uiGadget.setNoCache(noCache.equals("1"));
          */
         event.getRequestContext().setResponseComplete(true);
      }
   }

   static public class SetDebugActionListener extends EventListener<UIGadget>
   {
      public void execute(Event<UIGadget> event) throws Exception
      {
         /*
          * String debug = event.getRequestContext().getRequestParameter("debug") ;
          * UIGadget uiGadget = event.getSource() ;
          * uiGadget.setDebug(debug.equals("1"));
          */
         event.getRequestContext().setResponseComplete(true);
      }
   }

   public static class DeleteGadgetActionListener extends DeleteComponentActionListener
   {

      @Override
      public void execute(Event<UIComponent> event) throws Exception
      {
         UIComponent uiGadget = event.getSource();
         UIDashboardLayoutContainer container = uiGadget.getAncestorOfType(UIDashboardLayoutContainer.class);
         if (container != null && !container.canEdit())
         {
            return;
         }
         
         WebuiRequestContext context = event.getRequestContext();
         super.execute(event);
         
         UIPortalApplication uiApp = Util.getUIPortalApplication();
         if (container != null)
         {
            if (uiApp.getModeState() == UIPortalApplication.NORMAL_MODE)
            {
               // String objectId = context.getRequestParameter(OBJECTID);
               
               // if (uiDashboard.getMaximizedGadget() != null &&
               // uiDashboard.getMaximizedGadget().getId().equals(objectId))
               // {
               // uiDashboard.setMaximizedGadget(null);
               // }
               
               // Save
               DataStorage storage = uiGadget.getApplicationComponent(DataStorage.class);
               try
               {
                  storage.saveContainer((Container)PortalDataMapper.buildModelObject(uiGadget.getParent()));
               }
               catch (Exception ex)
               {
                  context.getUIApplication().addMessage(
                     new ApplicationMessage("UIDashboard.msg.StaleData", null, ApplicationMessage.ERROR));
                  context.addUIComponentToUpdateByAjax(container);
               }         
            }
            
            if (container.findFirstComponentOfType(UIWindow.class) == null)
            {
               context.getJavascriptManager().addCustomizedOnLoadScript("eXo.webui.UIDashboard.toogleState(" + container.getId() + ");");
            }            
         }
      }
   }
}

