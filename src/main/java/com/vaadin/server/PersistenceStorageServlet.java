package com.vaadin.server;

/**
 * The PersistenceStorageServlet extends the VaadinServlet modifying the createServletService method to
 * use the service PersistenceService.
 *
 * @author mrosin
 */
@SuppressWarnings("serial")
public class PersistenceStorageServlet extends VaadinServlet {

    /**
     * The createServletService method that sets the service to PersistenceStorageService
     */
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new PersistenceStorageService(this, deploymentConfiguration);
        service.init();
        return service;
    }

}
