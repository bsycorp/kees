package com.bsycorp.kees.models;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ResourceParameterTest {

    //SSM Path
    @Test
    public void shouldGiveCorrectSSMPath() throws IOException {
        ResourceParameter parameter = (ResourceParameter) Parameter.construct("resource.bsycorp.com/app.db.main.url", "localModeValue=dmFsdWU=");
        assertEquals("/storage/prefix/app.db.main.url", parameter.getStorageFullPath("/storage/prefix/"));
    }

    @Test
    public void shouldGiveCorrectSSMPathWithStorageKey() throws IOException {
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