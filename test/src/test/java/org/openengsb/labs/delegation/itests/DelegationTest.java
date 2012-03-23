/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.labs.delegation.itests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.delegation.itests.bundles.consumer.internal.SerializerConsumer;
import org.openengsb.labs.delegation.itests.bundles.consumer.internal.TestConsumer;
import org.openengsb.labs.delegation.itests.bundles.provider.ChildBean;
import org.openengsb.labs.delegation.itests.bundles.provider.TestBean;
import org.openengsb.labs.delegation.itests.bundles.provider.TestService;
import org.openengsb.labs.delegation.itests.bundles.provider.internal.TestProvider;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DelegationTest {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return options(
            mavenBundle().groupId("org.ops4j.pax.tinybundles").artifactId("tinybundles").versionAsInProject(),
            mavenBundle().groupId("org.openengsb.labs.delegation").artifactId("org.openengsb.labs.delegation.service")
                .versionAsInProject(),
            junitBundles(),
            felix());
    }

    @Test
    public void loadPrivateClassFromProvider_consumerShouldLoadClass() throws Exception {
        TinyBundle providerTinyBundle = createProviderBundle();
        Bundle providerBundle =
            bundleContext.installBundle("test://testlocation/test.provider.jar", providerTinyBundle.build());
        providerBundle.start();

        TinyBundle tinyBundle = createConsumerBundle();
        Bundle consumerBundle =
            bundleContext.installBundle("test://testlocation/test.consumer.jar", tinyBundle.build());
        consumerBundle.start();

        @SuppressWarnings("unchecked")
        Callable<Method> resultTask = getOsgiService(Callable.class, "(resultProvider=test)");
        Method method = resultTask.call();

        assertThat(method.getName(), is("doSomething"));
        assertThat(method.getParameterTypes(), equalTo(new Class<?>[0]));
    }

    @Test
    public void testSerializeBeanOfPrivateType() throws Exception {
        TinyBundle providerTinyBundle = createProviderBundle();
        Bundle providerBundle =
            bundleContext.installBundle("test://testlocation/test.provider.jar", providerTinyBundle.build());
        providerBundle.start();

        TinyBundle consumerTinyBundle = createConsumerBundle();
        consumerTinyBundle.add(SerializerConsumer.class).set(Constants.BUNDLE_ACTIVATOR,
            SerializerConsumer.class.getName());
        Bundle consumerBundle =
            bundleContext.installBundle("test://testlocation/test.consumer.jar", consumerTinyBundle.build());
        consumerBundle.start();
    }

    private TinyBundle createProviderBundle() {
        TinyBundle providerTinyBundle =
            bundle()
                .add(TestProvider.class)
                .add(TestProvider.PageProvider.class)
                .add(TestService.class)
                .add(TestBean.class)
                .add(ChildBean.class)
                .set(Constants.BUNDLE_ACTIVATOR, TestProvider.class.getName())
                .set(Constants.BUNDLE_SYMBOLICNAME, "test.provider")
                .set(Constants.BUNDLE_VERSION, "1.0.0")
                .set(Constants.IMPORT_PACKAGE,
                    "org.osgi.framework, org.slf4j, org.openengsb.labs.delegation.service");
        return providerTinyBundle;
    }

    private TinyBundle createConsumerBundle() {
        TinyBundle tinyBundle =
            bundle()
                .add(TestConsumer.class)
                .add(TestConsumer.ResultTask.class)
                .set(Constants.BUNDLE_ACTIVATOR, TestConsumer.class.getName())
                .set(Constants.BUNDLE_SYMBOLICNAME, "test.consumer")
                .set(Constants.BUNDLE_VERSION, "1.0.0")
                .set(Constants.IMPORT_PACKAGE,
                    "org.osgi.framework, org.slf4j, org.openengsb.labs.delegation.service");
        return tinyBundle;
    }

    @SuppressWarnings("unchecked")
    private <T> T getOsgiService(Class<T> serviceClass, String filter) throws InvalidSyntaxException {
        ServiceReference[] references = bundleContext.getServiceReferences(serviceClass.getName(), filter);
        assertThat("no service found for Class " + serviceClass + " and filter " + filter, references, not(nullValue()));
        return (T) bundleContext.getService(references[0]);
    }
}
