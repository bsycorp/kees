package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.Parameter;
import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocalStorageProviderTest {

    LocalStorageProvider provider = new LocalStorageProvider();

    @Test
    public void shouldRetrieveRawValueForParameter() throws Exception {
        Parameter parameter = Parameter.construct("secret.bsycorp.com/service-b.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        String result = provider.get("local", parameter);
        assertNotNull(result);
    }

    @Test
    public void shouldRetrieveEncodedValueForParameter() throws Exception {
        Parameter parameter = Parameter.construct("secret.bsycorp.com/service-b.v1_public", "kind=DYNAMIC,type=RSA,size=2048");
        String result = new String(provider.get("local", parameter));
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgQI70at0oy5nLrrdJloJPjJMlPkSiPzK+djmCkJkyg5AExEIf2wU/+DoLb8vVJvl6sHq+HTA0ViWoJqbEet8nr6PLI+aSNDhAHgV35RoHDBERSm42dEZswEJ2ZvZhfMuYJLDFER9qO9f+qWUpWR8q5fp8LC2M0ofLcUC7yStBDkzKjyfAqehXG+bHyg90HWZkm8iCZ4TWDndJdB0IBBEP7o3M9wuH8kJiaM1L/i1dl761uoVxyf5ANhec9KvT5L9o49ZuxD8rfjHHa23YvhNFF69MOag+/SwOspVeLZynzeAQ7zioYJXjZrNwvLCdRw3lYF20egTbM2EFOqVGX5alQIDAQAB", result);
    }

    @Test
    public void shouldRetrieveRawValueForResourceParameter() throws Exception {
        Parameter parameter = Parameter.construct("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");
        byte[] result = Base64.getDecoder().decode(provider.get("local", parameter));
        assertEquals("value", new String(result));
    }

    @Test
    public void shouldRetrieveEncodedValueForResourceParameter() throws Exception {
        Parameter parameter = Parameter.construct("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");
        String result = provider.get("local", parameter);
        assertEquals("dmFsdWU=", result);
    }
}