package com.bsycorp.kees.storage;

import com.bsycorp.kees.Utils;
import com.bsycorp.kees.models.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class DynamoDBStorageProvider implements StorageProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBStorageProvider.class);

    public final String  tableName;
    private final DynamoDbClient client = Utils.getDDBClient();

    public DynamoDBStorageProvider(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Setting DDB item for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put("secretName", AttributeValue.builder().s(ssmPath).build());
            values.put("secretValue", AttributeValue.builder().s(value).build());
            client.putItem(
                    PutItemRequest.builder()
                            .tableName(tableName)
                            .item(values)
                            //do this to mimic default SSM behaviour where it won't overwrite
                            .conditionExpression("attribute_not_exists(secretName)")
                            .build()
            );
            //success!
            LOG.info("Set DDB parameter and value for key: {}", ssmPath);

        } catch (ConditionalCheckFailedException ce){
            //item already exists
            LOG.warn("Item already exists for key, ignoring new value: {}", ssmPath);

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when setting item for key: " + ssmPath, e);
        }

    }

    @Override
    public String get(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Looking up DDB value for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> itemKey = new HashMap<>();
            itemKey.put("secretName", AttributeValue.builder().s(ssmPath).build());
            GetItemResponse result = client.getItem(GetItemRequest.builder().tableName(tableName).key(itemKey).build());
            if (!result.hasItem()){
                LOG.warn("Couldn't find item for key: {}", ssmPath);
                return null;
            }
            return result.item().get("secretValue").s();

        } catch (ResourceNotFoundException e) {
            LOG.warn("Couldn't find item for key: {}", ssmPath);
            return null;

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return null;
        }
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Checking DDB value exists for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":name", AttributeValue.builder().s(ssmPath).build());

            QueryResponse result = client.query(
                    QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("secretName =:name")
                    .expressionAttributeValues(eav)
                    .projectionExpression("secretName")
                    .build()
            );
            return result.count().intValue() > 0;

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return false;
        }
    }
}
