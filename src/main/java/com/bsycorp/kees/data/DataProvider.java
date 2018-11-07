package com.bsycorp.kees.data;

import com.bsycorp.kees.models.SecretTypeEnum;

public interface DataProvider {

    //returns the raw bytes of the generated secret
    byte[] generateRaw(SecretTypeEnum type, String annotationName, int size);

    //returns Public, Private for RSA as raw bytes/binary format
    Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size);

    String generateBase64Encoded(SecretTypeEnum type, String annotationName, int size);

    String[] generatePairedBase64Encoded(SecretTypeEnum type, String annotationName, int size);

}
