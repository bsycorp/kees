package com.bsycorp.kees;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bsycorp.kees.storage.InMemoryStorageProvider;
import com.bsycorp.kees.storage.SSMStorageProvider;
import com.bsycorp.kees.storage.StorageProvider;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateMainTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(false);

    private static ExecutorService executorService = Executors.newFixedThreadPool(1);

    private CreateMain createMain;
    StorageProvider storageProvider;

    @Before
    public void setUp() throws Exception {
        createMain = new CreateMain();
        createMain.setClient(server.getClient());

        storageProvider = mock(SSMStorageProvider.class);
        createMain.setStorageProvider(storageProvider);
        reset(storageProvider);
    }

    @After
    public void tearDown() throws Exception {
        createMain.shutdown();
    }

    @Test(timeout = 10000)
    public void shouldFindNoMatchingPods() throws Exception {

        final boolean[] hadException = {false};

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("pod1")
                .withResourceVersion("1")
                .endMetadata()
                .build();

        server.expect()
                .withPath("/api/v1/pods?watch=true")
                .andUpgradeToWebSocket().open().waitFor(500).andEmit(new WatchEvent(pod, "ADDED")).done().once();

        executorService.submit(() -> {
            try {
                createMain.run();
            } catch (Exception e) {
                e.printStackTrace();
                hadException[0] = true;
            }
        });

        //wait for it to process 1 event
        while(createMain.getEventCounter().get() < 1){
            Thread.sleep(500);
        }

        verify(storageProvider, times(0)).put(any(), any(), any());
        assertEquals(false, hadException[0]);

    }

    @Test(timeout = 10000)
    public void shouldFindOneMatchingPod() throws Exception {
        InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        createMain.setStorageProvider(storageProvider);
        final boolean[] hadException = {false};

        Map<String, String> annotations = new HashMap<>();
        annotations.put("init.bsycorp.com/local-mode", "false");
        annotations.put("init.bsycorp.com/storage-prefix", "local");
        annotations.put("secret.bsycorp.com/service-key.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.thing.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.thing.v1_private", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.key", "kind=DYNAMIC,type=RANDOM,size=288");
        annotations.put("secret.bsycorp.com/app.db", "kind=REFERENCE,type=PASSWORD,localModeValue=cGFzc3dvcmQ=");
        annotations.put("secret.bsycorp.com/api-key.service-c.v1_consumer", "kind=DYNAMIC,type=RANDOM,size=128");
        annotations.put("secret.bsycorp.com/api-key.service-c.v1_provider", "kind=DYNAMIC,type=RANDOM,size=128");
        annotations.put("secret.bsycorp.com/service-d.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("pod1")
                .withResourceVersion("1")
                .withAnnotations(annotations)
                .endMetadata()
                .build();

        server.expect()
                .withPath("/api/v1/pods?watch=true")
                .andUpgradeToWebSocket().open().waitFor(500).andEmit(new WatchEvent(pod, "ADDED")).done().once();

        executorService.submit(() -> {
            try {
                createMain.run();
            } catch (Exception e) {
                e.printStackTrace();
                hadException[0] = true;
            }
        });

        //wait for it to process 1 event
        while(createMain.getEventCounter().get() < 1){
            Thread.sleep(500);
        }

        //verify we create 12 values and had no exceptions
        assertTrue(storageProvider.getStore().containsKey("local/service-key.v1_private"));
        assertTrue(storageProvider.getStore().containsKey("local/service-d.v1_private"));
        assertTrue(storageProvider.getStore().containsKey("local/service-key.v1_public"));
        assertTrue(storageProvider.getStore().containsKey("local/service-d.v1_public"));
        assertTrue(storageProvider.getStore().containsKey("local/common.key"));
        assertTrue(storageProvider.getStore().containsKey("local/api-key.service-c.v1_consumer"));
        assertTrue(storageProvider.getStore().containsKey("local/api-key.service-c.v1_provider"));
        assertTrue(storageProvider.getStore().containsKey("local/common.thing.v1_private"));
        assertTrue(storageProvider.getStore().containsKey("local/common.thing.v1_public"));
        assertEquals(
                storageProvider.getStore().containsKey("local/api-key.service-c.v1_consumer"),
                storageProvider.getStore().containsKey("local/api-key.service-c.v1_provider")
        );
        assertEquals(9, storageProvider.getStore().size());
        assertEquals(false, hadException[0]);
        assertEquals(0, createMain.getExceptionCounter().get());

    }

    @Test(timeout = 10000)
    public void shouldFindOneModifiedMatchingPod() throws Exception {
        SSMStorageProvider storageProvider = mock(SSMStorageProvider.class);
        createMain.setStorageProvider(storageProvider);
        final boolean[] hadException = {false};

        Map<String, String> annotations = new HashMap<>();
        annotations.put("init.bsycorp.com/local-mode", "false");
        annotations.put("init.bsycorp.com/storage-prefix", "/bsycorp/testbed");
        annotations.put("secret.bsycorp.com/service-key.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.thing.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.thing.v1_private", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("secret.bsycorp.com/common.key", "kind=DYNAMIC,type=RANDOM,size=288");
        annotations.put("secret.bsycorp.com/app.db", "kind=REFERENCE,type=PASSWORD,localModeValue=cGFzc3dvcmQ=");
        annotations.put("secret.bsycorp.com/service-c.api-key", "kind=DYNAMIC,type=RANDOM,size=128");
        annotations.put("secret.bsycorp.com/service-d.api-key", "kind=DYNAMIC,type=RANDOM,size=128");
        annotations.put("secret.bsycorp.com/service-e.api-key", "kind=DYNAMIC,type=RANDOM,size=128");
        annotations.put("secret.bsycorp.com/service-d.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("pod1")
                .withResourceVersion("1")
                .withAnnotations(annotations)
                .endMetadata()
                .build();

        server.expect()
                .withPath("/api/v1/pods?watch=true")
                .andUpgradeToWebSocket().open().waitFor(500).andEmit(new WatchEvent(pod, "MODIFIED")).done().once();

        executorService.submit(() -> {
            try {
                createMain.run();
            } catch (Exception e) {
                e.printStackTrace();
                hadException[0] = true;
            }
        });

        //wait for it to process 1 event
        while(createMain.getEventCounter().get() < 1){
            Thread.sleep(500);
        }

        //verify we create 12 values and had no exceptions
        verify(storageProvider, times(12)).put(eq("/bsycorp/testbed"), any(), any());
        assertEquals(false, hadException[0]);
        assertEquals(0, createMain.getExceptionCounter().get());

    }

    @Test(timeout = 10000)
    public void shouldFindOneMatchingPodInLocalMode() throws Exception {
        SSMStorageProvider storageProvider = mock(SSMStorageProvider.class);
        createMain.setStorageProvider(storageProvider);
        final boolean[] hadException = {false};

        Map<String, String> annotations = new HashMap<>();
        annotations.put("init.bsycorp.com/local-mode", "true");
        annotations.put("init.bsycorp.com/storage-prefix", "local");
        annotations.put("secret.bsycorp.com/service-d.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        annotations.put("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("pod1")
                .withResourceVersion("1")
                .withAnnotations(annotations)
                .endMetadata()
                .build();

        server.expect()
                .withPath("/api/v1/pods?watch=true")
                .andUpgradeToWebSocket().open().waitFor(500).andEmit(new WatchEvent(pod, "ADDED")).done().once();

        executorService.submit(() -> {
            try {
                createMain.run();
            } catch (Exception e) {
                e.printStackTrace();
                hadException[0] = true;
            }
        });

        //wait for it to process 1 event
        while(createMain.getEventCounter().get() < 1){
            Thread.sleep(500);
        }

        //verify we create 12 values and had no exceptions
        verify(storageProvider, times(0)).put(any(), any(), any());
        assertEquals(false, hadException[0]);
        assertEquals(0, createMain.getExceptionCounter().get());

    }
}