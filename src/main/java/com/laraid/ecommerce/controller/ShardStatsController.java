package com.laraid.ecommerce.controller;

import com.laraid.ecommerce.service.ShardStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shards")
public class ShardStatsController {

    @Autowired
    private ShardStatsService shardStatsService;

    @GetMapping("/distribution/{collection}")
    public ResponseEntity<Map<String, Object>> getShardDistribution(@PathVariable String collection) {
        Map<String, Object> stats = shardStatsService.getShardDistribution(collection);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/counts/{collection}")
    public ResponseEntity<Map<String, Long>> getShardCounts(@PathVariable String collection) {
        Map<String, Long> counts = shardStatsService.getShardCounts(collection);
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/totalShardsCount/{collection}")
    public ResponseEntity<Map<String, Object>> getShardCountsTotal(@PathVariable String collection) {
        Map<String, Object> counts = shardStatsService.getShardCountsWithTotal(collection);
        return ResponseEntity.ok(counts);
    }
}

