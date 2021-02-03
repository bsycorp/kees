package com.bsycorp.kees.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import java.io.IOException;

public class ResourceParameterTest {

    @Test
    public void shouldGiveCorrectPath() throws IOException {
        ResourceParameter parameter = (ResourceParameter) Parameter.construct("resource.bsycorp.com/app.db.main.url", "localModeValue=dmFsdWU=");
        assertEquals("/storage/prefix/app.db.main.url", parameter.getStorageFullPath("/storage/prefix/"));
    }

    @Test
    public void shouldGiveCorrectPathWithStorageKey() throws IOException {
        ResourceParameter parameter = new ResourceParameter("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");
        assertEquals("/storage/smth/db.url", parameter.getStorageFullPath("/storage/smth/"));
    }

    //RESOURCE
    @Test
    public void shouldParseResourceWithStorageKeyAnnotation() throws Exception {
        ResourceParameter parameter = new ResourceParameter("resource.bsycorp.com/app.db.main.url", "storageKey=db.url,localModeValue=dmFsdWU=");

        assertEquals("dmFsdWU=", new String(parameter.getLocalValue()));
        assertEquals("db.url", parameter.getStorageKey());
    }

}