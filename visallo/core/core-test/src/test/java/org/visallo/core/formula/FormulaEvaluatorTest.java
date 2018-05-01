package org.visallo.core.formula;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.ontology.OntologyRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class FormulaEvaluatorTest {
    private FormulaEvaluator evaluator;
    private FormulaEvaluator.UserContext userContext;
    private Graph graph;
    private Authorizations authorizations;

    @Mock
    private OntologyRepository ontologyRepository;

    @Before
    public void before() throws Exception {
        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations();

        Map<String, String> map = new HashMap<>();
        ConfigurationLoader configurationLoader = new HashMapConfigurationLoader(map);
        Configuration configuration = configurationLoader.createConfiguration();

        Locale locale = Locale.getDefault();
        String timeZone = TimeZone.getDefault().getDisplayName();
        userContext = new FormulaEvaluator.UserContext(locale, timeZone, null);

        final String ontologyJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("ontology.json"), "utf-8");
        final String configurationJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("configuration.json"), "utf-8");
        final String vertexJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("vertex.json"), "utf-8");

        evaluator = new FormulaEvaluator(configuration, ontologyRepository) {
            @Override
            protected String getOntologyJson() {
                return ontologyJson;
            }

            @Override
            protected String getConfigurationJson(Locale locale) {
                return configurationJson;
            }

            @Override
            protected String toJson(Element element, String workspaceId, Authorizations authorizations) {
                if (element != null) {
                    return super.toJson(element, workspaceId, authorizations);
                }
                return vertexJson;
            }
        };
    }

    @After
    public void teardown() {
        evaluator.close();
    }

    @Test
    public void testEvaluatorJson() throws Exception {
        assertTrue(evaluator.getOntologyJson().length() > 0);
        assertTrue(evaluator.getConfigurationJson(Locale.getDefault()).length() > 0);
    }

    @Test
    public void testEvaluateTitleFormula() {
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, userContext, authorizations));
    }

    @Test
    public void testEvaluateSubtitleFormula() {
        assertEquals("Prop C Value", evaluator.evaluateSubtitleFormula(null, userContext, authorizations));
    }

    @Test
    public void testEvaluateTimeFormula() {
        assertEquals("2014-11-20", evaluator.evaluateTimeFormula(null, userContext, authorizations));
    }

    @Test
    public void testDuration() {
        String propertyKey = "pkey";
        String propertyName = "http://visallo.org/dev#duration";

        Element element = graph.prepareVertex("v1", new Visibility(""))
                .addPropertyValue(propertyKey, propertyName, 1234, new Visibility(""))
                .save(authorizations);
        graph.flush();

        assertEquals("20m 34s", evaluator.evaluatePropertyDisplayFormula(element, propertyKey, propertyName, userContext, authorizations));
    }

    @Test
    public void testThreading() throws InterruptedException {
        Thread[] threads = new Thread[4];
        final AtomicInteger threadsReadyCount = new AtomicInteger();
        final Semaphore block = new Semaphore(threads.length);
        block.acquire(threads.length);

        // prime the main thread for evaluation
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, userContext, authorizations));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // prime this thread for evaluation
                        evaluator.evaluateTitleFormula(null, userContext, null);
                        threadsReadyCount.incrementAndGet();
                        block.acquire(); // wait to run the look
                        for (int i = 0; i < 20; i++) {
                            System.out.println(Thread.currentThread().getName() + " - " + i);
                            assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, userContext, authorizations));
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException("Could not run", ex);
                    }
                }
            });
            threads[i].setName(FormulaEvaluatorTest.class.getSimpleName() + "-testThreading-" + i);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        // wait for all threads to be primed
        while (threadsReadyCount.get() < threads.length) {
            Thread.sleep(100);
        }
        block.release(threads.length);

        // wait for threads to finish
        for (Thread thread : threads) {
            synchronized (thread) {
                thread.join();
            }
        }

        // make sure the main threads evaluator isn't broken.
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, userContext, authorizations));
        evaluator.close();
    }
}