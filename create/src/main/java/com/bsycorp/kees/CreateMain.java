package com.bsycorp.kees;

import static com.bsycorp.kees.Utils.getAnnotationDomain;
import static com.bsycorp.kees.Utils.setupProxyProperties;

import com.bsycorp.kees.data.DataProvider;
import com.bsycorp.kees.data.RandomDataProvider;
import com.bsycorp.kees.models.LeaseParameter;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.ResolvedLeaseParameter;
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
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateMain {

    private static final Logger LOG = LoggerFactory.getLogger(CreateMain.class);

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
        setupProxyProperties();
        CreateMain main = new CreateMain();

        //need this to find the correct dynamo table for this env, could be replaced by something more explicit
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
                if(action == Action.ADDED || action == Action.MODIFIED || action == Action.DELETED) {
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
                                    if (expiringCache.containsKey(parameter.getParameterNameWithField() + action.name())) {
                                        LOG.info("Skipping parameter {} as already exists in processed cache..", parameter.getParameterName());
                                        continue;
                                    }

                                    if (parameter instanceof LeaseParameter) {
                                        handleLeaseParameter((LeaseParameter) parameter, resource, action, storagePrefix);

                                    //check provider, see if it already exists, if nothing then go create..
                                    } else if (!storageProvider.exists(storagePrefix, parameter) && action != Action.DELETED) {
                                        if (parameter instanceof SecretParameter) {
                                            handleSecretParameter((SecretParameter) parameter, resource, action, storagePrefix);

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

    private void handleSecretParameter(SecretParameter parameter, Pod resource, Watcher.Action action, String storagePrefix) {
        //only create values for dynamic values
        if (((SecretParameter) parameter).getKind() == SecretKindEnum.DYNAMIC) {
            LOG.info("Found parameter {} with no matching value, creating..", parameter.getParameterName());
            SecretParameter secretParameter = (SecretParameter) parameter;

            //need special behaviour for RSA as it has two params generated from one call. breaks abstracts and is annoying.
            if (secretParameter.getType() == SecretTypeEnum.RSA) {
                parameter.overrideFieldName("public");
                String[] encodedValues = dataProvider.generatePairedBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId());
                storageProvider.put(storagePrefix, parameter, encodedValues[0], true);
                parameter.overrideFieldName("private");
                storageProvider.put(storagePrefix, parameter, encodedValues[1], true);
            } else if (secretParameter.getType() == SecretTypeEnum.GPG) {
                parameter.overrideFieldName("public");
                String[] encodedValues = dataProvider.generatePairedBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize(), secretParameter.getUserId());
                storageProvider.put(storagePrefix, parameter, encodedValues[0], true);
                parameter.overrideFieldName("private");
                storageProvider.put(storagePrefix, parameter, encodedValues[1], true);
                parameter.overrideFieldName("password");
                storageProvider.put(storagePrefix, parameter, encodedValues[2], true);
                //TODO extract this out to another secret type
            } else if (secretParameter.getType() == SecretTypeEnum.RANDOM && secretParameter.getParameterName().startsWith("api-key")) {
                String encodedValue = dataProvider.generateBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize());
                parameter.overrideFieldName("consumer");
                storageProvider.put(storagePrefix, parameter, encodedValue, true);
                parameter.overrideFieldName("provider");
                storageProvider.put(storagePrefix, parameter, encodedValue, true);


            } else {
                //otherwise treat as normal value
                storageProvider.put(storagePrefix, parameter, dataProvider.generateBase64Encoded(secretParameter.getType(), parameter.getParameterName(), secretParameter.getSize()), true);
            }

            LOG.info("Created value for {}", parameter.getParameterName());
            expiringCache.put(parameter.getParameterNameWithField() + action.name(), "success");
        }
    }

    protected void handleLeaseParameter(LeaseParameter parameter, Pod resource, Watcher.Action action, String storagePrefix) throws IOException {
        if (action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED) {
            //take lease storage prefix and find a lease index within the range, keep leased items in memory to make leasing cheaper
            //make sure we havent already given this pod a lease, if so use that value
            LeaseParameter leaseParameter = parameter;
            String podName = resource.getMetadata().getName();
            String storageKeyPrefix = leaseParameter.getStorageKeyPrefix();

            String leaseKey = storageProvider.getKeyByParameterAndValue(storagePrefix, parameter, podName);
            if (leaseKey == null) {
                boolean assignedLease = false;
                //generate valid local value for lease
                for (int index = leaseParameter.getRangeStart(); index <= leaseParameter.getRangeEnd(); index++) {
                    //it would be more efficient to try and maintain a list of what has been previously issued, but that risks omitting valie numbers
                    //since the range is probably small, just brute force lookups against dynamo until we get one that succeeds
                    String potentialLeaseKey = storageKeyPrefix + "." + index;
                    //reserve that value in dynamo against pod
                    leaseKey = potentialLeaseKey;
                    try {
                        storageProvider.put(storagePrefix, new ResolvedLeaseParameter(
                                parameter,
                                leaseKey.substring(storageKeyPrefix.length() + 1)
                        ), podName, false);
                        LOG.info("Created lease {} for pod {}", leaseKey, podName);
                        expiringCache.put(parameter.getParameterNameWithField() + action.name(), "success");
                        assignedLease = true;
                        break;
                    } catch (Exception e) {
                        //error putting key, assume its conflicting / already taken, try again
                    }
                }

                if (!assignedLease) {
                    LOG.error("Couldn't create valid lease for prefix, ran out of valid leases: {}", storageKeyPrefix);
                }

            } else {
                LOG.info("Re-used lease {} for pod {}", leaseKey, podName);
                expiringCache.put(parameter.getParameterNameWithField() + action.name(), "success");
            }

            //also have housekeeping control loop to keep local cache in sync and clean up missing / deleted pods

        } else if (action == Watcher.Action.DELETED) {
            String podName = resource.getMetadata().getName();

            String fullLeaseKey = storageProvider.getKeyByParameterAndValue(storagePrefix, parameter, podName);
            if (fullLeaseKey != null && !fullLeaseKey.isEmpty()) {
                String leaseValue = fullLeaseKey.substring((storagePrefix + "/leases/" + parameter.getStorageKeyPrefix()).length() + 1);
                storageProvider.delete(storagePrefix, new ResolvedLeaseParameter(parameter, leaseValue), podName);
                LOG.info("Deleted lease for pod: {} with key: {}", podName, fullLeaseKey);
            } else {
                LOG.debug("Didn't delete lease for pod: {} with key: {}", podName, fullLeaseKey);
            }
        }
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
