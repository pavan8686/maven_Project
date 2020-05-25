/**
 * QI Analyze is a Java EE framework for identifying and analyzing Quality Indicators
 * Copyright (C) 2010  QIAnalyze LLC http://www.qianalyze.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package IBM;

import generated.CurrentObservation;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXB;
import javax.xml.transform.stream.StreamSource;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.definition.KnowledgePackage;
import org.drools.definitions.impl.KnowledgePackageImp;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.qianalyze.api.indicator.Indicator;
import org.qianalyze.api.indicator.IndicatorResult;
import org.qianalyze.api.module.IModuleInspector;
import org.qianalyze.api.module.Module;
import org.qianalyze.api.module.ModuleResponse;
import IBM.module.Sailing;

/**
 * This QiAnalyze Module is a entry point for the QiAnalyze system. It is this object that is called from the QiAnalyze
 * Web Service. It must implement the IModuleInspector and Serializable interfaces.
 * 
 * @author Matthew Demerath
 * 
 */
@Stateless
@Remote(IModuleInspector.class)
public class QiModule implements IModuleInspector, Serializable {

    private static final long serialVersionUID = -5012094493515326219L;

    // Our connecton to the QiAnalyze Database
    @PersistenceContext(unitName = "pavanPU")
    private EntityManager entityManager;

    /**
     * This method is called from the QiAnalyze Web Service.
     * 
     * @return {@link}ModuleResponse
     */
    public ModuleResponse inspect(Module module, String xmlPayload) {

        // Create a response object to send back to the QiAnalyze Web Service.
        ModuleResponse response = new ModuleResponse();

        // Unmarshal the XMLPayload to a java object we can work with.
        // This object was created from the xsd located in the src/main/resources/xsd directory.
        // HyperJaxb created this object as an entity bean.
        CurrentObservation currentObservation = JAXB.unmarshal(new StreamSource(new StringReader(xmlPayload)),
                CurrentObservation.class);

        // check to see if the user would like to save this data model to the database.
        if (module.isPersist()) {
            entityManager.persist(currentObservation);
        }

        // Each Module can have multiple Indicators.
        // Loop through the collection and call the Inspect on each.
        for (Indicator indicator : module.getIndicators()) {

            // Create a result for this Indicator.
            IndicatorResult ir = null;

            KnowledgeRuntimeLogger droolsLogger = null;

            // Use the name that was passed in to figure out what Indicator to call.
            try {
                // Check to see if the Indicator they want to call is a POJO
                if (indicator.getName().equalsIgnoreCase("sailing")) {
                    Sailing sailing = new Sailing();

                    // Pass the data we unmashalled from the XMLPayload to the Indicator.
                    ir = sailing.inspect(currentObservation);
                } else {

                    // you can use the JBoss Drools engine to inspect the unmarshalled data.
                    // if we didn't fiid the name of a POJO Indicator see if there is a drools
                    // rule file with the name.

                    // Create factHandle to interact with.
                    org.drools.runtime.rule.FactHandle irHandle = null;

                    // Try to load the Drools Rule file from the name passed in.
                    // The rules files are located in the /src/main/drools directory.
                    KnowledgeBase kbase = loadKnowledgePackages(indicator.getName());

                    // Create a new Drools session
                    StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

                    // Create a logger for Drools
                    droolsLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession, "DroolsLog");

                    // Insert into the session the data we wish to inspect.
                    ksession.insert(currentObservation);

                    // create a new result that the Drools engine can manipulate.
                    ir = new IndicatorResult();

                    // Insert the result into the session.
                    irHandle = ksession.insert(ir);

                    // Fire all the rules in the rules engine.
                    ksession.fireAllRules();

                }

            } catch (Throwable t) {
                t.printStackTrace();
                ir.setResult("Error loading indicator");
            } finally {
                if (droolsLogger != null) {
                    // don't forget to close resources.
                    droolsLogger.close();
                }
            }

            // set the name of the indicator result from what they passed in.
            ir.setName(indicator.getName());

            // Add this Indicator's result to the Module's reponse.
            response.getIndicatorResults().add(ir);
        }

        // Return the Module's response to the calling QiAnalyze application.
        return response;
    }

    /**
     * 
     * @param rule name of the indicator that is also the name of the drools rule file
     * @return The KnowledgeBase which is the comipled version of the rules expressed in the drools rule file.
     * @throws IOException thrown when there is an exception in loading or reading the drools rule file.
     * @throws ClassNotFoundException The two most common reasons this exception is thrown are:
     *         1) Missing an import statement in the drools rule file.
     *         2) The imported java class is not in the path.
     */
    private KnowledgeBase loadKnowledgePackages(String rule) throws IOException, ClassNotFoundException {

        int hash = this.getClass().getClassLoader().hashCode();
        ObjectInputStream in;
        InputStream is = this.getClass().getResourceAsStream(rule);
        in = new ObjectInputStream(is);
        org.drools.rule.Package pkg;
        pkg = (org.drools.rule.Package) in.readObject();
        in.close();
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        ArrayList<KnowledgePackage> kpgs = new ArrayList<KnowledgePackage>();
        KnowledgePackage kpg = new KnowledgePackageImp(pkg);
        kpgs.add(kpg);
        kbase.addKnowledgePackages(kpgs);

        return kbase;

    }
}
