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
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmFuN37R3egbUcXtNkYiGrc/kYGYClIDCnkER1gRKEJ8xDaUdYJDA0DjiN1S7pX4GEUURDBDWD5fHyDtX38tgAMR1fnUt9EGLxOXCSgDCYpGNcd/toMnsKwkgvEDr9DupA/RlH5VUl+HytzcgWKNj1oG0k6qOyzXSrBpiWe693CX+F2etChU7cBBPxGPKCYSgw2laLNoLiFUZVgeakkhL3A7vMYuTQSK3w3iavYFe1x1Mwq16HA8yPU7qaHyvtZAm/ld5u3qM6nABFikTZhlSIirQa4WNL5+e7jcapKEWmo+PcTr3Thr1pi+8OXXuv4QwjaYOCQFxC1CH8zyjIViRJQIDAQAB", result);
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