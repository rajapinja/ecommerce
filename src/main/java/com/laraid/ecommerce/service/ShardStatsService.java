package com.laraid.ecommerce.service;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ShardStatsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Map<String, Object> getShardDistribution(String collectionName) {
        Document stats = mongoTemplate.executeCommand(new Document("collStats", collectionName));
        return stats;
    }


    public Map<String, Long> getShardCounts(String collectionName) {
        Document stats = mongoTemplate.executeCommand(new Document("collStats", collectionName));

        Map<String, Long> shardCounts = new HashMap<>();
        if (stats.containsKey("shards")) {
            Document shardsDoc = (Document) stats.get("shards");
            for (String shardName : shardsDoc.keySet()) {
                Document shardInfo = (Document) shardsDoc.get(shardName);

                // Handle both Integer and Long safely
                Number countNumber = (Number) shardInfo.get("count");
                shardCounts.put(shardName, countNumber.longValue());
            }
        }
        return shardCounts;
    }

    public Map<String, Object> getShardCountsWithTotal(String collectionName) {
        Document stats = mongoTemplate.executeCommand(new Document("collStats", collectionName));

        Map<String, Object> response = new LinkedHashMap<>();
        long total = 0;

        if (stats.containsKey("shards")) {
            Document shardsDoc = (Document) stats.get("shards");
            for (String shardName : shardsDoc.keySet()) {
                Document shardInfo = (Document) shardsDoc.get(shardName);

                Number countNumber = (Number) shardInfo.get("count");
                long count = countNumber.longValue();

                response.put(shardName, count);
                total += count;
            }
        }

        response.put("totalDocuments", total);
        return response;
    }

}

