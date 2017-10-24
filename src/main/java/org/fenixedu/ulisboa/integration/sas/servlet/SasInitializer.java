package org.fenixedu.ulisboa.integration.sas.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.integration.sas.domain.SchoolLevelTypeMapping;
import org.fenixedu.ulisboa.integration.sas.domain.SocialServicesConfiguration;
import org.fenixedu.ulisboa.integration.sas.service.DailyEnrolmentsIndexing;
import org.fenixedu.ulisboa.integration.sas.service.sicabe.SicabeExternalService;
import org.fenixedu.ulisboa.integration.sas.webservices.ActiveStudentsWebService;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@WebListener
public class SasInitializer implements ServletContextListener {

    @Override
    @Atomic(mode = TxMode.SPECULATIVE_READ)
    public void contextInitialized(ServletContextEvent event) {
        //Bootstrap SocialServicesConfiguration Singleton
        if (Bennu.getInstance().getSocialServicesConfiguration() == null) {
            SocialServicesConfiguration socialServicesConfiguration = new SocialServicesConfiguration();
        }
        DailyEnrolmentsIndexing.bindToSignals();
        SchoolLevelTypeMapping.registerEvents();
        // Load the class so the update thread starts
        ActiveStudentsWebService.class.getName();
        
        SicabeExternalService.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}