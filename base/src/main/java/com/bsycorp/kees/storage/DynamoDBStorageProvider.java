package com.bsycorp.kees.storage;

import com.bsycorp.kees.Utils;
import com.bsycorp.kees.models.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
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
    public void put(String storagePrefix, Parameter key, String value, Boolean ignorePutFailure) {
        String itemPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Setting DDB item for key: {}", itemPath);

        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put("secretName", AttributeValue.builder().s(itemPath).build());
            values.put("secretValue", AttributeValue.builder().s(value).build());
            client.putItem(
                    PutItemRequest.builder()
                            .tableName(tableName)
                            .item(values)
                            .conditionExpression("attribute_not_exists(secretName)")
                            .build()
            );
            //success!
            LOG.info("Set DDB parameter and value for key: {}", itemPath);

        } catch (ConditionalCheckFailedException ce) {
            //item already exists
            if (ignorePutFailure) {
                LOG.warn("Item already exists for key, ignoring new value: {}", itemPath);
            } else {
                LOG.error("Error putting value for key: {}", itemPath);
                throw new RuntimeException("Failed to put key and not ignoring failures");
            }

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when setting item for key: " + itemPath, e);
        }

    }

    @Override
    public void delete(String storagePrefix, Parameter key, String expectedValue) {
        String itemPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Deleting DDB item for key: {} with expected value: {}", itemPath, expectedValue);

        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put("secretName", AttributeValue.builder().s(itemPath).build());

            Map<String, AttributeValue> conditions = new HashMap<>();
            conditions.put(":value", AttributeValue.builder().s(expectedValue).build());
            client.deleteItem(
                    DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(values)
                            .conditionExpression("secretValue = :value")
                            .expressionAttributeValues(conditions)
                            .build()
            );
            //success!
            LOG.info("Deleted DDB parameter and value for key: {}", itemPath);

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error deleting item for key: " + itemPath, e);
        }
    }

    @Override
    public String getValueByKey(String storagePrefix, Parameter key) {
        String itemPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Looking up DDB value for key: {}", itemPath);

        try {
            Map<String, AttributeValue> itemKey = new HashMap<>();
            itemKey.put("secretName", AttributeValue.builder().s(itemPath).build());
            GetItemResponse result = client.getItem(GetItemRequest.builder().tableName(tableName).key(itemKey).build());
            if (!result.hasItem()){
                LOG.warn("Couldn't find item for key: {}", itemPath);
                return null;
            }
            return result.item().get("secretValue").s();

        } catch (ResourceNotFoundException e) {
            LOG.warn("Couldn't find item for key: {}", itemPath);
            return null;

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + itemPath, e);
            return null;
        }
    }

    @Override
    public String getKeyByParameterAndValue(String storagePrefix, Parameter parameter, String value) {
        LOG.info("Looking up DDB keys for value: {}", value);

        try {
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":storagePrefix", AttributeValue.builder().s(parameter.getStorageFullPath(storagePrefix)).build());
            expressionValues.put(":value", AttributeValue.builder().s(value).build());
            ScanResponse result = client.scan(
                    ScanRequest.builder()
                            .tableName(tableName)
                            .filterExpression("begins_with(secretName, :storagePrefix) and secretValue = :value")
                            .expressionAttributeValues(expressionValues)
                            .build()
            );
            if (!result.hasItems()){
                LOG.warn("Couldn't find item for value: {}", value);
                return null;
            }
            return result.items().stream().map(i -> i.get("secretName").s()).findFirst().orElseGet(() -> null);

        } catch (ResourceNotFoundException e) {
            LOG.warn("Couldn't find item for value: {}", value);
            throw new RuntimeException(e);

        } catch (DynamoDbException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with value: " + value, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String itemPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Checking DDB value exists for key: {}", itemPath);

        try {
            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":name", AttributeValue.builder().s(itemPath).build());

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
            LOG.error("Error when looking up parameter with key: " + itemPath, e);
            return false;
        }
    }
}
