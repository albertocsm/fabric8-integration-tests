/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.pires.example.tests;

import com.github.pires.example.dal.UserService;
import com.github.pires.example.dal.entities.JSON;
import com.github.pires.example.dal.entities.User;
import io.fabric8.api.ServiceLocator;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Constants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class UserServiceIntegrationTest extends FabricTestSupport
{

    public static final String USER_MANAGER_URL = "http://localhost:8181/cxf/demo/user";

    /**
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe)
    {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "com.github.pires.example.tests,*,org.apache.felix.service.*;status=provisional");
        probe.setHeader(Constants.BUNDLE_SYMBOLICNAME, "com.github.pires.example.tests");
        return probe;
    }

    @Configuration
    public Option[] config()
    {
        return combine(
                fabricDistributionConfiguration(),
                CoreOptions.wrappedBundle(CoreOptions.mavenBundle().groupId("commons-httpclient").artifactId("commons-httpclient"))
        );
    }

    @Before
    public void setUp() throws Exception
    {
        System.err.println(executeCommand("fabric:create -n root"));

        waitForFabricCommands();

        System.err.println(executeCommand("features:addUrl mvn:com.github.pires.example/feature-persistence/0.1-SNAPSHOT/xml/features", 10000, false));

        System.err.println(executeCommand("features:install persistence-aries-hibernate", 300000, false));
        System.err.println("persistence-aries-hibernate done");

        System.err.println(executeCommand("features:install persistence-rest", 300000, false));
        System.err.println("persistence-rest done");

        //System.err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/datasource-hsqldb/0.1-SNAPSHOT"));
        System.err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/datasource-postgresdb/0.1-SNAPSHOT"));
        System.err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/dal/0.1-SNAPSHOT"));
        System.err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/dal-impl/0.1-SNAPSHOT"));
        System.err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/rest/0.1-SNAPSHOT"));


        System.err.println("setUp all done");
    }

    @After
    public void tearDown() throws InterruptedException
    {
        Thread.sleep(5000);
    }

    @Test
    public void shouldCreateAndRetriveUserWithDAL() throws Exception
    {
        try
        {
            UserService proxy = ServiceLocator.awaitService(this.bundleContext, UserService.class);
            assertNotNull(proxy);

            User user = new User();
            user.setName("alberto");
            final String properties =
                    "{"
                            + "\"num1\":{"
                            + "\"type\":\"number\", "
                            + "\"value\":1, "
                            + "\"mandatory\":false}, "
                            + "\"string1\":{"
                            + "\"type\":\"string\", "
                            + "\"value\":\"teste\", "
                            + "\"mandatory\":false}"
                            + "}";

            user.setProperties(new JSON(properties));
            proxy.create(user);

            List<User> result = proxy.findAll();
            assertTrue(result.size() == 1);

            User returnedUser = result.get(0);
            System.err.println(returnedUser.getName());
            System.err.println(returnedUser.getProperties().toString());

            assertTrue(returnedUser.getName().contains("alberto"));
            assertTrue(returnedUser.getProperties().toString().contains("{\"string1\":{\"") && returnedUser.getProperties().toString().contains("\"num1\":{"));
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            org.junit.Assert.fail("test failed");
        }
    }

    @Test
    public void shouldCreateAndRetriveUserWithREST() throws Exception
    {
        String payload =
                "{ " +
                        "\"name\": \"manel joao\", " +
                        "\"properties\": {" +
                            "\"value\": {" +

                                "\"num1\":{"+
                                    "\"type\":\"number\", "+
                                    "\"value\":\"1\", "+
                                    "\"mandatory\":\"false\"}, "+

                                "\"string1\":{"+
                                    "\"type\":\"string\", "+
                                    "\"value\":\"teste\", "+
                                    "\"mandatory\":\"false\"}"
                        + "}}}";
        try
        {
            //create user
            PutMethod put = new PutMethod(USER_MANAGER_URL);
            put.addRequestHeader("Accept", "application/json");
            RequestEntity entity = new StringRequestEntity(payload, "application/json", "UTF-8");
            put.setRequestEntity(entity);

            HttpClient httpclient = new HttpClient();
            int result = 0;
            try
            {
                result = httpclient.executeMethod(put);
                //System.err.println(result);
                assertTrue(result == 204);
            }
            catch (Exception e)
            {
                org.junit.Assert.fail("Unable to create user");
            }
            finally
            {
                put.releaseConnection();
            }

            //get user
            URL url = new URL(USER_MANAGER_URL);
            InputStream in = null;
            try
            {
                in = url.openStream();
            }
            catch (Exception e)
            {
                org.junit.Assert.fail("Connection error");
            }

            String res = getStringFromInputStream(in);
            System.err.println(res);

            assertTrue(res.contains("\"name\":\"manel joao\"") && res.contains("\"string1\":{") && res.contains("\"num1\":{"));
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            org.junit.Assert.fail("test failed");
        }
    }

    private static String getStringFromInputStream(InputStream in) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = in.read()) != -1)
        {
            bos.write(c);
        }
        in.close();
        bos.close();
        return bos.toString();
    }
}
