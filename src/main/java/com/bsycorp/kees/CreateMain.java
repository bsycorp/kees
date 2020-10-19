package com.bsycorp.kees;

import static com.bsycorp.kees.Utils.getAnnotationDomain;

import com.bsycorp.kees.data.DataProvider;
import com.bsycorp.kees.data.RandomDataProvider;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.ResourceParameter;
import com.bsycorp.kees.models.SecretKindEnum;
import com.bsycorp.kees.models.SecretParameter;
import com.bsycorp.kees.models.SecretTypeEnum;
import com.bsycorp.kees.storage.DynamoDBStorageProvider;
import com.bsycorp.kees.storage.StorageProvider;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateMain {

    private static Logger LOG = LoggerFactory.getLogger(CreateMain.class);

    private KubernetesClient client;

    private StorageProvider storageProvider;
    private DataProvider dataProvider = new RandomDataProvider();
    private AnnotationParser annotationParser = new AnnotationParser();
    private AtomicInteger exceptionCounter = new AtomicInteger();
    private AtomicInteger eventCounter = new AtomicInteger();
    private Watch watch = null;
    final CountDownLatch watcherLatch = new CountDownLatch(1);

    private Map<String, String> expiringCache = new PassiveExpiringMap<>(60000);

    public static void main(String... argv) throws Exception {
        CreateMain main = new CreateMain();

        String envLabel = Utils.getEnvironment().get("ENV_LABEL");
        if (envLabel == null) {
            throw new Exception("ENV_LABEL is required");
        }

        try {
            main.setStorageProvider(new DynamoDBStorageProvider(Utils.getTableName(envLabel)));
            main.run();
        } catch (Exception e) {
            LOG.error("Error in execution", e);
            throw e;
        }
    }

    public void run() throws Exception {
        LOG.info("Starting creator process..");

        //watch pods
        if (client == null) {
            LOG.info("Creating Kubernetes client..");
            client = new DefaultKubernetesClient();
        }

        watch = client.pods().inAnyNamespace().watch(new Watcher<Pod>() {

            @Override
            public synchronized void eventReceived(Action action, Pod resource) {
                //only try to create
                if(action == Action.ADDED || action == Action.MODIFIED) {
                    try {
                        LOG.info("Checking pod: {} in namespace: {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                        //get annotations from pod definition
                        Map<String, String> annotations = resource.getMetadata().getAnnotations();
                        if(annotations == null){
                            //default to empty map to avoid NPE
                            annotations = Collections.EMPTY_MAP;
                        }

                        //check if pod is running local mode
                        if (isLocalMode(annotations)) {
                            //local mode
                            LOG.info("Pod {} in namespace: {} is in local mode, skipping.", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                            return;
                        }

                        //otherwise is a valid pod, parse and create missing!
                        List<Parameter> parameters = annotationParser.parseMap(annotations);
                        String storagePrefix = annotations.get("init." + getAnnotationDomain() + "/storage-prefix");

                        //if we have some matching annotations
                        if (parameters.size() > 0 && storagePrefix != null) {
                            LOG.info("Found {} matching annotations to process..", parameters.size());
                            for (Parameter parameter : parameters) {
                                try {
                                    //if we have processed this param recently then skip!
                                    if (expiringCache.containsKey(parameter.getParameterNameWithField())) {
                                        LOG.info("Skipping parameter {} as already exists in processed cache..", parameter.getParameterName());
                                        continue;
                                    }

                                    //check provider, see if it already exists, if nothing then go create..
                                    if (!storageProvider.exists(storagePrefix, parameter)) {
                                        if (parameter instanceof SecretParameter) {
                                            //only create values for dynamic values
                                            if (((SecretParameter) parameter).getKind() == SecretKindEnum.DYNAMIC) {
                                                LOG.info("Found parameter {} with no matching value, creating..", parameter.getParameterName());
                                                SecretParameter secretParameter = (SecretParameter) parameter;

                                                //need special behaviour for RSA as it has two params generated from one call. breaks abstracts and is annoying.
                                                if (secretParameter.getType() == SecretTypeEnum.RSA) {
                                                    parameter.overrideFieldName("public");
                                                    String[] encodedValues = dataProvider.generatePairedBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId());
                                                    storageProvider.put(storagePrefix, parameter, encodedValues[0]);
                                                    parameter.overrideFieldName("private");
                                                    storageProvider.put(storagePrefix, parameter, encodedValues[1]);
                                                } else if (secretParameter.getType() == SecretTypeEnum.GPG) {
                                                    parameter.overrideFieldName("public");
                                                    String[] encodedValues = dataProvider.generatePairedBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId());
                                                    storageProvider.put(storagePrefix, parameter, encodedValues[0]);
                                                    parameter.overrideFieldName("private");
                                                    storageProvider.put(storagePrefix, parameter, encodedValues[1]);
                                                    parameter.overrideFieldName("password");
                                                    storageProvider.put(storagePrefix, parameter, encodedValues[2]);
                                                //TODO extract this out to another secret type
                                                } else if (secretParameter.getType() == SecretTypeEnum.RANDOM && secretParameter.getParameterName().startsWith("api-key")) {
                                                    String encodedValue = dataProvider.generateBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize());
                                                    parameter.overrideFieldName("consumer");
                                                    storageProvider.put(storagePrefix, parameter, encodedValue);
                                                    parameter.overrideFieldName("provider");
                                                    storageProvider.put(storagePrefix, parameter, encodedValue);


                                                } else {
                                                    //otherwise treat as normal value
                                                    storageProvider.put(storagePrefix, parameter, dataProvider.generateBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize()));
                                                }

                                                LOG.info("Created value for {}", parameter.getParameterName());
                                                expiringCache.put(parameter.getParameterNameWithField(), "success");
                                            }

                                        } else if (parameter instanceof ResourceParameter) {
                                            //ignore as resources can't be generated

                                        } else {
                                            throw new RuntimeException("Unsupported parameter");
                                        }

                                    }

                                } catch (Exception e) {
                                    LOG.error("Error creating value for annotation: " + parameter.getFullAnnotationName(), e);
                                }
                            }
                        }

                    } catch (Exception e) {
                        exceptionCounter.incrementAndGet();
                        LOG.error("Error processing event received", e);
                        throw e;
                    } finally {
                        eventCounter.incrementAndGet();
                    }
                }

            }

            @Override
            public void onClose(KubernetesClientException cause) {
                LOG.error("Pod watcher closed, terminating..", cause);
                //decrement latch to mark watcher closed.
                watcherLatch.countDown();
            }

        });

        //blocking while watcher is connected
        watcherLatch.await();
    }

    public void shutdown() {
        watch.close();
        watcherLatch.countDown();
    }

    public void setClient(KubernetesClient client) {
        this.client = client;
    }

    public void setStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public AtomicInteger getExceptionCounter() {
        return exceptionCounter;
    }

    public AtomicInteger getEventCounter() {
        return eventCounter;
    }

    boolean isLocalMode(Map properties) {
        //if local mode exists and its true
        return properties != null
                && properties.containsKey("init." + getAnnotationDomain() + "/local-mode")
                && ((String) properties.get("init."  + getAnnotationDomain() + "/local-mode")).contains("true");
    }
}
