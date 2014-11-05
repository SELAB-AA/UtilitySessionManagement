package com.vaadin.server;

import com.vaadin.server.communication.*;

import java.util.ArrayList;
import java.util.List;


/**
 * The PersistenceStorageService extending the VaadinServletService modifying the session management
 * to achieve session persistence to different medias.
 * <p>
 * NOTE: Modified: Only code to add the custom UIInitHandler included.
 *
 * @author mrosin
 */
@SuppressWarnings("serial")
public class PersistenceStorageService extends VaadinServletService {


    public PersistenceStorageService(VaadinServlet servlet,
                                     DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        super(servlet, deploymentConfiguration);
    }

    /**
     * The method for creating RequestHandlers. Private methods from VaadinService
     * and VaadinServletService (merged) but overridden to use custom UIInitHandler.
     */
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        ArrayList<RequestHandler> handlers = new ArrayList<RequestHandler>();
        handlers.add(new SessionRequestHandler());
        handlers.add(new PublishedFileHandler());
        handlers.add(new HeartbeatHandler());
        handlers.add(new FileUploadHandler());
        handlers.add(new UidlRequestHandler());
        handlers.add(new UnsupportedBrowserHandler());
        handlers.add(new ConnectorResourceHandler());

        List<RequestHandler> handlers_from_servletService = handlers;
        handlers_from_servletService.add(0, new ServletBootstrapHandler());
        handlers_from_servletService.add(new PersistenceUIInitHandler());
        if (ensurePushAvailable()) {
            handlers_from_servletService.add(new PushRequestHandler(this));
        }
        return handlers;
    }

}
