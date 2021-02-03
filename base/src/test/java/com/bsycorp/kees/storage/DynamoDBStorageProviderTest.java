package com.bsycorp.kees.storage;

import com.bsycorp.kees.models.SecretParameter;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DynamoDBStorageProviderTest {

    @Ignore("Need AWS SecretCreator creds")
    @Test
    public void testCreatorIntegrationAndPolicy() throws IOException {
        DynamoDBStorageProvider provider = new DynamoDBStorageProvider("test-table");
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/api-key.test" + System.currentTimeMillis() + ".v1_provider", "kind=DYNAMIC,type=RANDOM,size=128");
        boolean exists = provider.exists("test", parameter);
        System.out.println("Exists Parameter: " + parameter.getStorageFullPath("test") + " - " + exists);
        assertEquals(false, exists);

        String getResult = provider.getValueByKey("test", parameter);
        System.out.println("Get Parameter: " + parameter.getStorageFullPath("test") + " - '" + getResult + "'");
        assertEquals(null, getResult);
        
        provider.put("test", parameter, "newer secret value!!!", true);
        System.out.println("PutParameter: " + parameter.getStorageFullPath("test"));

        //create done
        exists = provider.exists("test", parameter);
        System.out.println("Exists 2 Parameter: " + parameter.getStorageFullPath("test") + " - " + exists);
        assertEquals(true, exists);

        getResult = provider.getValueByKey("test", parameter);
        System.out.println("Get 2 Parameter: " + parameter.getStorageFullPath("test") + " - '" + getResult + "'");
        assertEquals(null, getResult);
    }
}