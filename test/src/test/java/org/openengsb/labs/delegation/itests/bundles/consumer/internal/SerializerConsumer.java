package org.openengsb.labs.delegation.itests.bundles.consumer.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class SerializerConsumer implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        ServiceReference reference = context.getServiceReferences(Callable.class.getName(), "(type=pageProvider)")[0];
        @SuppressWarnings("unchecked")
        Callable<Object> provider = (Callable<Object>) context.getService(reference);
        Object orignal = provider.call();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ObjectOutputStream(out).writeObject(orignal);
        byte[] data = out.toByteArray();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object pageObject = ois.readObject();
        if (!pageObject.equals(orignal)) {
            throw new Exception("not equal");
        }
    }

    public void stop(BundleContext context) throws Exception {
    };

}