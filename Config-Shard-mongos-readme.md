## Once Docker-compose up for Config Server, Shard Servers, and Mongos please add ConfigRelSet, Each shard1, shar2, enable 
    as shown below

    1.Replica sets / config servers are not initialized → mongos has nothing to talk to.
    2.Shards not added yet → mongos can’t route queries anywhere.
    3.mongos started but configReplSet isn’t initiated → it refuses client connections.

## ReplicaSet Initialize Config Server Replica Set
    docker exec -it ecommerce-configsvr1-1 mongosh --port 27019

    rs.initiate({
        _id: "configReplSet",
        configsvr: true,
        members: [
                { _id: 0, host: "configsvr1:27019" },
                { _id: 1, host: "configsvr2:27020" },
                { _id: 2, host: "configsvr3:27021" }
            ]
        }) 
## Initialize Each Shard Replica Set
    shard1: 
        docker exec -it ecommerce-shard1-1 mongosh --port 27030

        rs.initiate({
            _id: "shard1",
            members: [ { _id: 0, host: "shard1:27030" } ]
        })
    
### Connect to mongos and Add Shards
        sh.addShard("shard1/shard1:27030")

     shard2: 
        docker exec -it ecommerce-shard2-1 mongosh --port 27031

        rs.initiate({
            _id: "shard2",
            members: [ { _id: 0, host: "shard2:27031" } ]
        })

### Connect to mongos and Add Shards
        sh.addShard("shard2/shard2:27031")


    shard3: 
        docker exec -it ecommerce-shard3-1 mongosh --port 27032

        rs.initiate({
            _id: "shard3",
            members: [ { _id: 0, host: "shard3:27032" } ]
        })

### Connect to mongos and Add Shards   
        sh.addShard("shard3/shard3:27032")

## Enable Sharding for Your Database
    sh.enableSharding("ecommerce")
    sh.shardCollection("ecommerce.orders", { orderId: "hashed" })


## Or

## We’ll create an init-mongo.js script that:

    Initiates the configReplSet.
    Initiates each shard replica set.
    Connects through mongos and adds all shards.
    Enables sharding for ecommerce DB and shards orders collection.

### Put this file in your project folder (same level as docker-compose.yml):

    `// init-mongo.js
    
    // Wait a bit for mongod/mongos to be up
    sleep(10000);
    
    // ----------------------
    // Config Server Replica Set
    // ----------------------
    print(">>> Initiating Config Server Replica Set");
    rs.initiate({
    _id: "configReplSet",
    configsvr: true,
    members: [
    { _id: 0, host: "configsvr1:27019" },
    { _id: 1, host: "configsvr2:27020" },
    { _id: 2, host: "configsvr3:27021" }
    ]
    });
    
    // ----------------------
    // Shard1
    // ----------------------
    print(">>> Initiating Shard1 Replica Set");
    conn = new Mongo("shard1:27030");
    db = conn.getDB("admin");
    db.runCommand({
    replSetInitiate: {
    _id: "shard1",
    members: [ { _id: 0, host: "shard1:27030" } ]
    }
    });
    
    // ----------------------
    // Shard2
    // ----------------------
    print(">>> Initiating Shard2 Replica Set");
    conn = new Mongo("shard2:27031");
    db = conn.getDB("admin");
    db.runCommand({
    replSetInitiate: {
    _id: "shard2",
    members: [ { _id: 0, host: "shard2:27031" } ]
    }
    });
    
    // ----------------------
    // Shard3
    // ----------------------
    print(">>> Initiating Shard3 Replica Set");
    conn = new Mongo("shard3:27032");
    db = conn.getDB("admin");
    db.runCommand({
    replSetInitiate: {
    _id: "shard3",
    members: [ { _id: 0, host: "shard3:27032" } ]
    }
    });
    
    // ----------------------
    // Mongos Setup
    // ----------------------
    print(">>> Connecting to Mongos and adding shards");
    conn = new Mongo("mongos:27017");
    db = conn.getDB("admin");
    sh.addShard("shard1/shard1:27030");
    sh.addShard("shard2/shard2:27031");
    sh.addShard("shard3/shard3:27032");
    
    // Enable sharding for ecommerce DB
    print(">>> Enabling sharding for ecommerce DB");
    sh.enableSharding("ecommerce");
    
    // Shard the orders collection
    print(">>> Sharding ecommerce.orders on orderId (hashed)");
    sh.shardCollection("ecommerce.orders", { orderId: "hashed" });
    
    print(">>> Sharding Setup Complete ✅");

### 2. Update docker-compose.yml (Add a one-time init container:)

    mongo-init:
        image: mongo:7
        depends_on:
          - mongos
        volumes:
          - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro
        entrypoint: [ "mongosh", "--host", "mongos:27017", "/docker-entrypoint-initdb.d/init-mongo.js" ]
    
    `
### 3. Run Everything
    docker-compose up -d

## Do you also want me to wire this so that mongo-express logs in and shows the sharded ecommerce DB automatically?

### 🔧 Update docker-compose.yml

    Modify your mongo-express service like this:

    mongo-express:
        image: mongo-express
        restart: always
        depends_on:
          - mongos
        ports:
          - "8081:8081"
        environment:
          ME_CONFIG_MONGODB_SERVER: mongos        # point to mongos router
          ME_CONFIG_MONGODB_PORT: 27017
          ME_CONFIG_MONGODB_ENABLE_ADMIN: "true" # admin mode to see all DBs
          ME_CONFIG_BASICAUTH_USERNAME: admin
          ME_CONFIG_BASICAUTH_PASSWORD: admin


### ✅ After setup
    `
    Run:
    docker-compose up -d
    
    Wait a bit (init script will configure config servers, shards, and mongos).
    You’ll see logs like:
    
    >>> Initiating Config Server Replica Set
    >>> Initiating Shard1 Replica Set
    >>> Connecting to Mongos and adding shards
    >>> Sharding Setup Complete ✅`


    Open http://localhost:8081
    in your browser.
    Login with:
    
    Username: admin    
    Password: admin    
    You should see:    
        admin    
        config    
        ecommerce (with orders sharded collection)

## Open shell inside mongos: 
    docker exec -it ecommerce-mongos mongosh
### ⚡ Bonus: Count documents per shard
    db.getSiblingDB("ecommerce").orders.aggregate([
        { $group: { _id: "$$SHARDING$INTERNAL", count: { $sum: 1 } } }
    ])

## 👉 Do you want me to also give you a host-side Node.js or Python script that calls mongos and prints the same table, so you don’t need to log into mongosh every time?

### 1️⃣ Node.js Script
    chunkMap.js

    const { MongoClient } = require("mongodb");
    
    async function printChunkMap() {
    const uri = "mongodb://localhost:27017";  // adjust if mongos is exposed elsewhere
    const client = new MongoClient(uri);
    
    try {
        await client.connect();
    
        const configDb = client.db("config");
        const chunks = await configDb.collection("chunks")
          .find({ ns: "ecommerce.orders" })
          .sort({ "min.customerId": 1 })
          .toArray();
    
        console.log("Chunk distribution for ecommerce.orders");
        console.log("=".repeat(80));
        console.log("| Range (min → max)                           | Shard   |");
        console.log("-".repeat(80));
    
        chunks.forEach(c => {
          const min = JSON.stringify(c.min.customerId);
          const max = JSON.stringify(c.max.customerId);
          console.log(`| ${min} → ${max} | ${c.shard}`);
        });
    
        console.log("=".repeat(80));
    } finally {
        await client.close();
        }
    }
    
    printChunkMap().catch(console.error);

### Output
    Run
    node chunkMap.js

## 2️⃣ Python Script
    chunk_map.py

    from pymongo import MongoClient
    import json
    
    def print_chunk_map():
    client = MongoClient("mongodb://localhost:27017")  # adjust if mongos exposed differently
    config_db = client["config"]

    chunks = config_db["chunks"].find(
        {"ns": "ecommerce.orders"}
    ).sort("min.customerId", 1)

    print("Chunk distribution for ecommerce.orders")
    print("=" * 80)
    print("| Range (min → max)                           | Shard   |")
    print("-" * 80)

    for c in chunks:
        min_val = json.dumps(c["min"]["customerId"])
        max_val = json.dumps(c["max"]["customerId"])
        print(f"| {min_val} → {max_val} | {c['shard']}")

    print("=" * 80)

    if __name__ == "__main__":
    print_chunk_map()

    python chunk_map.py

### Output  ️⃣ Example Output (both versions)
    Chunk distribution for ecommerce.orders
    ================================================================================
    | Range (min → max)                           | Shard   |
    --------------------------------------------------------------------------------
    | "MinKey" → "-5000000000000000000"           | shard1  |
    | "-5000000000000000000" → "0"                | shard2  |
    | "0" → "MaxKey"                              | shard3  |
    ================================================================================


## 1️⃣ Run this in mongosh (connected to mongos)
    use config

    db.chunks.find({ ns: "ecommerce.orders" })
    .sort({ "min.customerId": 1 })
    .pretty()

### This will dump all chunk metadata, ordered by shard key ranges.

    2️⃣ Example output (simplified)
    Let’s say you have 3 shards. You might see something like:

    {
        "min" : { "customerId" : MinKey },
        "max" : { "customerId" : NumberLong(-5000000000000000000) },
        "shard" : "shard1"
    }
    {
        "min" : { "customerId" : NumberLong(-5000000000000000000) },
        "max" : { "customerId" : NumberLong(0) },
        "shard" : "shard2"
    }
    {
        "min" : { "customerId" : NumberLong(0) },
        "max" : { "customerId" : MaxKey },
        "shard" : "shard3"
    }

### 3️⃣ Visualized distribution
    Keyspace (hashed customerId)

    ┌──────────────────────────────┬──────────────────────────────┬──────────────────────────────┐
    │ Range: MinKey → -5000...     │ Range: -5000... → 0          │ Range: 0 → MaxKey            │
    │ Shard: shard1                │ Shard: shard2                │ Shard: shard3                │
    └──────────────────────────────┴──────────────────────────────┴──────────────────────────────┘

    Any customerId is hashed → falls into one of these ranges → sent to the owning shard.
    Over time, chunks split further, so instead of 3 big chunks, you might have dozens.

### 4️⃣ To see data ownership in numbers
    sh.status()

    Look for the shardedDataDistribution section:
    "shardedDataDistribution" : [
        {
            "ns" : "ecommerce.orders",
        "shards" : [
            { "shardName" : "shard1", "numOwnedDocuments" : 1000 },
            { "shardName" : "shard2", "numOwnedDocuments" : 980 },
            { "shardName" : "shard3", "numOwnedDocuments" : 1020 }
        ]
        }
        ]

        This shows how many docs each shard currently stores.

    ✅ So, chunks = ranges of hashed key values.
    ✅ Each chunk belongs to one shard.
    ✅ MongoDB automatically splits/moves chunks to keep shards balanced.

## (base) PS C:\_Raja\_SpringBoot\ecommerce> docker exec -it ecommerce-mongos-1 mongosh --port 27017
    [direct: mongos] config> use ecommerce
    switched to db ecommerce
    [direct: mongos] ecommerce> db.orders.countDocuments();
    8

## Bulk inserts 
    (base) PS C:\_Raja\_SpringBoot\ecommerce> docker exec -it ecommerce-mongos-1 mongosh --port 27017
    
    [direct: mongos] ecommerce>
    
    for (let i = 1; i <= 5000; i++) {
        db.orders.insertOne({
        orderId: i,
        customerId: i % 50,        // shard key
        amount: Math.floor(Math.random() * 1000),
        status: "CREATED",
        createdAt: new Date()
        });
    };

    # once inserted
    {
        acknowledged: true,
        insertedId: ObjectId('68cd08660b1b948a1ace72ce')
    }

### [direct: mongos] ecommerce> db.orders.getShardDistribution()
    Shard shard3 at shard3/shard3:27032
    {
        data: '499KiB',
        docs: 5008,
        chunks: 1,
        'estimated data per chunk': '499KiB',
        'estimated docs per chunk': 5008
    }
    ---
    Totals
    {
        data: '499KiB',
        docs: 5008,
        chunks: 1,
        'Shard shard3': [
            '100 % data',
            '100 % docs in cluster',
            '102B avg obj size on shard'
        ]
    }


### Really sharder sh.status()

    [direct: mongos] ecommerce> sh.status()

    shardingVersion
    { _id: 1, clusterId: ObjectId('68cc783e7d3c3b28d35c061c') }
    ---
    shards
    [
        {
            _id: 'shard1',
            host: 'shard1/shard1:27030',
            state: 1,
            topologyTime: Timestamp({ t: 1758231296, i: 5 })
        },
        {
            _id: 'shard2',
            host: 'shard2/shard2:27031',
            state: 1,
            topologyTime: Timestamp({ t: 1758231313, i: 2 })
        },
        {
            _id: 'shard3',
            host: 'shard3/shard3:27032',
            state: 1,
            topologyTime: Timestamp({ t: 1758231327, i: 2 })
        }
    ]
    ---
    active mongoses
    [ { '7.0.24': 1 } ]
    ---
    autosplit
    { 'Currently enabled': 'yes' }
    ---
    balancer
    {
        'Currently enabled': 'yes',
        'Currently running': 'no',
        'Failed balancer rounds in last 5 attempts': 0,
        'Migration Results for the last 24 hours': 'No recent migrations'
    }
    ---
    shardedDataDistribution
    [
        {
            ns: 'config.system.sessions',
            shards: [
            {
                shardName: 'shard1',
                numOrphanedDocs: 0,
                numOwnedDocuments: 5,
                ownedSizeBytes: 495,
                orphanedSizeBytes: 0
            }
    ]
    },
    {
        ns: 'ecommerce.orders',
        shards: [
            {
                shardName: 'shard3',
                numOrphanedDocs: 0,
                numOwnedDocuments: 5008,
                ownedSizeBytes: 510816,
                orphanedSizeBytes: 0
        }
    ]
    }
    ]
    ---
    databases
    [
        {
            database: { _id: 'config', primary: 'config', partitioned: true },
            collections: {
            'config.system.sessions': {
            shardKey: { _id: 1 },
            unique: false,
            balancing: true,
            chunkMetadata: [ { shard: 'shard1', nChunks: 1 } ],
            chunks: [
            { min: { _id: MinKey() }, max: { _id: MaxKey() }, 'on shard': 'shard1', 'last modified': Timestamp({ t: 1, i: 0 }) }
            ],
            tags: []
            }
            }
            },
            {
            database: {
                _id: 'ecommerce',
                primary: 'shard3',
                partitioned: false,
                version: {
                uuid: UUID('bda6c8c1-095f-4c19-ab67-177e92f56138'),
                timestamp: Timestamp({ t: 1758231327, i: 2 }),
                lastMod: 1
            }
    },
    collections: {
        'ecommerce.orders': {
        shardKey: { customerId: 1 },
        unique: false,
        balancing: true,
        chunkMetadata: [ { shard: 'shard3', nChunks: 1 } ],
        chunks: [
        { min: { customerId: MinKey() }, max: { customerId: MaxKey() }, 'on shard': 'shard3', 'last modified': Timestamp({ t: 1, i: 0 }) }
        ],
        tags: []
        }
        }
        },
    {
    database: {
        _id: 'test',
        primary: 'shard2',
        partitioned: false,
        version: {
        uuid: UUID('b6bae7f4-f8da-4c6e-a8e4-b0db5b9e2ca4'),
        timestamp: Timestamp({ t: 1758267465, i: 1 }),
        lastMod: 1
    }
    },
    collections: {}
    }
    ]

### Above all the collections data inserted into shard3 only , we can manually split and spread data across all 3 shards as shown below

    ✅ Options to see real shard distribution now (without waiting for GBs of data)
    1. Pre-split & move chunks    
    Manually create chunks before inserting:
    
    // Enable sharding for ecommerce db
    sh.enableSharding("ecommerce")
    
    // Shard on customerId
    sh.shardCollection("ecommerce.orders", { customerId: 1 })
    
    // Pre-split at ranges
    sh.splitAt("ecommerce.orders", { customerId: 1000 })
    sh.splitAt("ecommerce.orders", { customerId: 2000 })
    
    // Move chunks to different shards
    sh.moveChunk("ecommerce.orders", { customerId: 0 }, "shard1")
    sh.moveChunk("ecommerce.orders", { customerId: 2000 }, "shard2")
    
    
    Now:    
    customerId < 1000 → goes to shard1    
    1000 ≤ customerId < 2000 → stays on shard3    
    customerId ≥ 2000 → goes to shard2

### 2. Insert synthetic test data

    From mongosh:    
    for (let i = 1; i <= 5000; i++) {
        db.orders.insert({ orderId: i, customerId: i, amount: Math.random() * 100 })
    }   
    
    Now getShardDistribution() should show docs across all 3 shards.

### After bulk inserts

    [direct: mongos] ecommerce> db.orders.getShardDistribution()

    Shard shard2 at shard2/shard2:27031
    {
        data: '1.12MiB',
        docs: 12008,
        chunks: 1,
        'estimated data per chunk': '1.12MiB',
        'estimated docs per chunk': 12008
    }
    ---
    Shard shard1 at shard1/shard1:27030
    {
        data: '498KiB',
        docs: 5000,
        chunks: 1,
        'estimated data per chunk': '498KiB',
        'estimated docs per chunk': 5000
    }
    ---
    Shard shard3 at shard3/shard3:27032
    {
        data: '0B',
        docs: 0,
        chunks: 1,
        'estimated data per chunk': '0B',
        'estimated docs per chunk': 0
    }
    ---
    Totals
    {
        data: '1.6MiB',
        docs: 17008,
        chunks: 3,
        'Shard shard2': [
            '69.78 % data',
            '70.6 % docs in cluster',
            '98B avg obj size on shard'
        ],
        'Shard shard1': [
            '30.21 % data',
            '29.39 % docs in cluster',
            '102B avg obj size on shard'
        ],
        'Shard shard3': [ '0 % data', '0 % docs in cluster', '0B avg obj size on shard' ]
    }


### [direct: mongos] ecommerce> sh.status()
    shardingVersion
    { _id: 1, clusterId: ObjectId('68cc783e7d3c3b28d35c061c') }
    ---
    shards
    [
        {
            _id: 'shard1',
            host: 'shard1/shard1:27030',
            state: 1,
            topologyTime: Timestamp({ t: 1758231296, i: 5 })
        },
        {
            _id: 'shard2',
            host: 'shard2/shard2:27031',
            state: 1,
            topologyTime: Timestamp({ t: 1758231313, i: 2 })
        },
        {
            _id: 'shard3',
            host: 'shard3/shard3:27032',
            state: 1,
            topologyTime: Timestamp({ t: 1758231327, i: 2 })
        }
    ]
    ---
    active mongoses
    [ { '7.0.24': 1 } ]
    ---
    autosplit
    { 'Currently enabled': 'yes' }
    ---
    balancer
    {
        'Currently enabled': 'yes',
        'Currently running': 'no',
        'Failed balancer rounds in last 5 attempts': 0,
        'Migration Results for the last 24 hours': 'No recent migrations'
    }
    ---
    shardedDataDistribution
    [
    {
    ns: 'config.system.sessions',
    shards: [
        {
            shardName: 'shard1',
            numOrphanedDocs: 0,
            numOwnedDocuments: 5,
            ownedSizeBytes: 495,
            orphanedSizeBytes: 0
        }
    ]
    },
    {
    ns: 'ecommerce.orders',
    shards: [
        {
            shardName: 'shard3',
            numOrphanedDocs: 0,
            numOwnedDocuments: 5008,
            ownedSizeBytes: 510816,
            orphanedSizeBytes: 0
        }
    ]
    }
    ]
    ---
    databases
    [
    {
    database: { _id: 'config', primary: 'config', partitioned: true },
    collections: {
        'config.system.sessions': {
        shardKey: { _id: 1 },
        unique: false,
        balancing: true,
        chunkMetadata: [ { shard: 'shard1', nChunks: 1 } ],
        chunks: [
        { min: { _id: MinKey() }, max: { _id: MaxKey() }, 'on shard': 'shard1', 'last modified': Timestamp({ t: 1, i: 0 }) }
        ],
        tags: []
    }
    }
    },
    {
    database: {
    _id: 'ecommerce',
    primary: 'shard3',
    partitioned: false,
    version: {
    uuid: UUID('bda6c8c1-095f-4c19-ab67-177e92f56138'),
    timestamp: Timestamp({ t: 1758231327, i: 2 }),
    lastMod: 1
    }
    },
    collections: {
    'ecommerce.orders': {
    shardKey: { customerId: 1 },
    unique: false,
    balancing: true,
    chunkMetadata: [ { shard: 'shard3', nChunks: 1 } ],
    chunks: [
    { min: { customerId: MinKey() }, max: { customerId: MaxKey() }, 'on shard': 'shard3', 'last modified': Timestamp({ t: 1, i: 0 }) }
    ],
    tags: []
    }
    }
    },
    {
    database: {
    _id: 'test',
    primary: 'shard2',
    partitioned: false,
    version: {
    uuid: UUID('b6bae7f4-f8da-4c6e-a8e4-b0db5b9e2ca4'),
    timestamp: Timestamp({ t: 1758267465, i: 1 }),
    lastMod: 1
    }
    },
    collections: {}
    }
    ]
    [direct: mongos] ecommerce> // Enable sharding for ecommerce db
    ... sh.enableSharding("ecommerce")
    ...
    ... // Shard on customerId
    ... sh.shardCollection("ecommerce.orders", { customerId: 1 })
    ...
    ... // Pre-split at ranges
    ... sh.splitAt("ecommerce.orders", { customerId: 1000 })
    ... sh.splitAt("ecommerce.orders", { customerId: 2000 })
    ...
    ... // Move chunks to different shards
    ... sh.moveChunk("ecommerce.orders", { customerId: 0 }, "shard1")
    ... sh.moveChunk("ecommerce.orders", { customerId: 2000 }, "shard2")
    {
    millis: 498,
    ok: 1,
    '$clusterTime': {
    clusterTime: Timestamp({ t: 1758268213, i: 84 }),
    signature: {
    hash: Binary.createFromBase64('AAAAAAAAAAAAAAAAAAAAAAAAAAA=', 0),
    keyId: Long('0')
    }
    },
    operationTime: Timestamp({ t: 1758268213, i: 84 })
    }
    [direct: mongos] ecommerce> for (let i = 1; i <= 5000; i++) {
    ...         db.orders.insert({ orderId: i, customerId: i, amount: Math.random() * 100 })
    ...     }
    [direct: mongos] ecommerce>
    ... for (let i = 5001; i <= 12000; i++) {
    ...   db.orders.insert({ orderId: i, customerId: i, amount: Math.random() * 100 })
    ... }
    DeprecationWarning: Collection.insert() is deprecated. Use insertOne, insertMany, or bulkWrite.
    {
    acknowledged: true,
    insertedIds: { '0': ObjectId('68cd0c670b1b948a1acea1ae') }
    }
    [direct: mongos] ecommerce> db.orders.getShardDistribution()
    Shard shard2 at shard2/shard2:27031
    {
    data: '1.12MiB',
    docs: 12008,
    chunks: 1,
    'estimated data per chunk': '1.12MiB',
    'estimated docs per chunk': 12008
    }
    ---
    Shard shard1 at shard1/shard1:27030
    {
    data: '498KiB',
    docs: 5000,
    chunks: 1,
    'estimated data per chunk': '498KiB',
    'estimated docs per chunk': 5000
    }
    ---
    Shard shard3 at shard3/shard3:27032
    {
    data: '0B',
    docs: 0,
    chunks: 1,
    'estimated data per chunk': '0B',
    'estimated docs per chunk': 0
    }
    ---
    Totals
    {
        data: '1.6MiB',
        docs: 17008,
        chunks: 3,
        'Shard shard2': [
            '69.78 % data',
            '70.6 % docs in cluster',
            '98B avg obj size on shard'
        ],
    'Shard shard1': [
        '30.21 % data',
        '29.39 % docs in cluster',
        '102B avg obj size on shard'
    ],
    'Shard shard3': [ '0 % data', '0 % docs in cluster', '0B avg obj size on shard' ]
    }
    [direct: mongos] ecommerce> sh.status()
        shardingVersion
        { _id: 1, clusterId: ObjectId('68cc783e7d3c3b28d35c061c') }
        ---
        shards
        [
        {
        _id: 'shard1',
        host: 'shard1/shard1:27030',
        state: 1,
        topologyTime: Timestamp({ t: 1758231296, i: 5 })
        },
        {
        _id: 'shard2',
        host: 'shard2/shard2:27031',
        state: 1,
        topologyTime: Timestamp({ t: 1758231313, i: 2 })
        },
        {
        _id: 'shard3',
        host: 'shard3/shard3:27032',
        state: 1,
        topologyTime: Timestamp({ t: 1758231327, i: 2 })
        }
    ]
    ---
    active mongoses
    [ { '7.0.24': 1 } ]
    ---
    autosplit
    { 'Currently enabled': 'yes' }
    ---
    balancer
    {
    'Currently enabled': 'yes',
    'Currently running': 'no',
    'Failed balancer rounds in last 5 attempts': 0,
    'Migration Results for the last 24 hours': { '2': 'Success' }
    }
    ---
    shardedDataDistribution
    [
    {
    ns: 'config.system.sessions',
    shards: [
    {
    shardName: 'shard1',
    numOrphanedDocs: 0,
    numOwnedDocuments: 7,
    ownedSizeBytes: 693,
    orphanedSizeBytes: 0
    }
    ]
    },
    {
    ns: 'ecommerce.orders',
    shards: [
    {
        shardName: 'shard3',
        numOrphanedDocs: 0,
        numOwnedDocuments: 0,
        ownedSizeBytes: 0,
        orphanedSizeBytes: 0
    },
    {
        shardName: 'shard1',
        numOrphanedDocs: 0,
        numOwnedDocuments: 5000,
        ownedSizeBytes: 510000,
        orphanedSizeBytes: 0
    },
    {
        shardName: 'shard2',
        numOrphanedDocs: 0,
        numOwnedDocuments: 12008,
        ownedSizeBytes: 1176784,
        orphanedSizeBytes: 0
    }
    ]
    }
    ]
    ---
    databases
    [
    {
        database: { _id: 'config', primary: 'config', partitioned: true },
        collections: {
        'config.system.sessions': {
        shardKey: { _id: 1 },
        unique: false,
        balancing: true,
        chunkMetadata: [ { shard: 'shard1', nChunks: 1 } ],
        chunks: [
        { min: { _id: MinKey() }, max: { _id: MaxKey() }, 'on shard': 'shard1', 'last modified': Timestamp({ t: 1, i: 0 }) }
        ],
        tags: []
        }
        }
        },
        {
        database: {
        _id: 'ecommerce',
        primary: 'shard3',
        partitioned: false,
        version: {
        uuid: UUID('bda6c8c1-095f-4c19-ab67-177e92f56138'),
        timestamp: Timestamp({ t: 1758231327, i: 2 }),
        lastMod: 1
    }
    },
    collections: {
        'ecommerce.orders': {
        shardKey: { customerId: 1 },
        unique: false,
        balancing: true,
        chunkMetadata: [
        { shard: 'shard1', nChunks: 1 },
        { shard: 'shard2', nChunks: 1 },
        { shard: 'shard3', nChunks: 1 }
        ],
        chunks: [
        { min: { customerId: MinKey() }, max: { customerId: 1000 }, 'on shard': 'shard1', 'last modified': Timestamp({ t: 2, i: 0 }) },
        { min: { customerId: 1000 }, max: { customerId: 2000 }, 'on shard': 'shard3', 'last modified': Timestamp({ t: 3, i: 1 }) },
        { min: { customerId: 2000 }, max: { customerId: MaxKey() }, 'on shard': 'shard2', 'last modified': Timestamp({ t: 3, i: 0 }) }
        ],
        tags: []
    }
    }
    },
    {
    database: {
        _id: 'test',
        primary: 'shard2',
        partitioned: false,
        version: {
        uuid: UUID('b6bae7f4-f8da-4c6e-a8e4-b0db5b9e2ca4'),
        timestamp: Timestamp({ t: 1758267465, i: 1 }),
        lastMod: 1
    }
    },
    collections: {}
    }
    ]

### 3. Force Balancing (if data sticks to one shard)

    If you bulk inserted into an unsharded collection, MongoDB just dropped everything into the primary shard.
    You need to enable sharding before bulk insert.
    
    Steps:    
        use ecommerce
        sh.enableSharding("ecommerce")
        sh.shardCollection("ecommerce.orders", { customerId: "hashed" })    
    
    Then new inserts will be distributed.
    If you already inserted, you can move chunks manually or drop + reinsert after enabling sharding.

### 4. Directly Query by customerId
    Try queries and see if Mongo only hits one shard:
#### [direct: mongos] ecommerce> db.orders.find({ customerId: "1001" }).explain("executionStats")
    {
        queryPlanner: {
        mongosPlannerVersion: 1,
        winningPlan: {
        stage: 'SINGLE_SHARD',
        shards: [
        {
            shardName: 'shard2',
            connectionString: 'shard2/shard2:27031',
            serverInfo: {
                host: '67288bf3055f',
                port: 27031,
                version: '7.0.24',
                gitVersion: '332b0e6c30fdc41a0228dc55657e2e0784b0fe24'
            },
            namespace: 'ecommerce.orders',
            indexFilterSet: false,
            parsedQuery: { customerId: { '$eq': '1001' } },
            queryHash: 'DE6BAE07',
            planCacheKey: '7E620628',
            optimizationTimeMillis: 0,
            maxIndexedOrSolutionsReached: false,
            maxIndexedAndSolutionsReached: false,
            maxScansToExplodeReached: false,
            winningPlan: {
            stage: 'FETCH',
            inputStage: {
            stage: 'IXSCAN',
            keyPattern: { customerId: 1 },
            indexName: 'customerId',
            isMultiKey: false,
            multiKeyPaths: { customerId: [] },
            isUnique: false,
            isSparse: false,
            isPartial: false,
            indexVersion: 2,
            direction: 'forward',
            indexBounds: { customerId: [ '["1001", "1001"]' ] }
        }
        },
        rejectedPlans: []
        }
        ]
        }
        },
        executionStats: {
            nReturned: 1,
            executionTimeMillis: 10,
            totalKeysExamined: 1,
            totalDocsExamined: 1,
            executionStages: {
            stage: 'SINGLE_SHARD',
            nReturned: 1,
            executionTimeMillis: 10,
            totalKeysExamined: 1,
            totalDocsExamined: 1,
            totalChildMillis: Long('1'),
        shards: [
            {
                shardName: 'shard2',
                executionSuccess: true,
                nReturned: 1,
                executionTimeMillis: 1,
                totalKeysExamined: 1,
                totalDocsExamined: 1,
                executionStages: {
                stage: 'FETCH',
                nReturned: 1,
                executionTimeMillisEstimate: 0,
                works: 2,
                advanced: 1,
                needTime: 0,
                needYield: 0,
                saveState: 0,
                restoreState: 0,
                isEOF: 1,
                docsExamined: 1,
                alreadyHasObj: 0,
                inputStage: {
                stage: 'IXSCAN',
                nReturned: 1,
                executionTimeMillisEstimate: 0,
                works: 2,
                advanced: 1,
                needTime: 0,
                needYield: 0,
                saveState: 0,
                restoreState: 0,
                isEOF: 1,
                keyPattern: { customerId: 1 },
                indexName: 'customerId',
                isMultiKey: false,
                multiKeyPaths: { customerId: [] },
                isUnique: false,
                isSparse: false,
                isPartial: false,
                indexVersion: 2,
                direction: 'forward',
                indexBounds: { customerId: [ '["1001", "1001"]' ] },
                keysExamined: 1,
                seeks: 1,
                dupsTested: 0,
                dupsDropped: 0
            }
            }
            }
        ]
        }
        },
        serverInfo: {
            host: 'b09cdcc9ba94',
            port: 27017,
            version: '7.0.24',
            gitVersion: '332b0e6c30fdc41a0228dc55657e2e0784b0fe24'
            },
            serverParameters: {
            internalQueryFacetBufferSizeBytes: 104857600,
            internalQueryFacetMaxOutputDocSizeBytes: 104857600,
            internalLookupStageIntermediateDocumentMaxSizeBytes: 104857600,
            internalDocumentSourceGroupMaxMemoryBytes: 104857600,
            internalQueryMaxBlockingSortMemoryUsageBytes: 104857600,
            internalQueryProhibitBlockingMergeOnMongoS: 0,
            internalQueryMaxAddToSetBytes: 104857600,
            internalDocumentSourceSetWindowFieldsMaxMemoryBytes: 104857600,
            internalQueryFrameworkControl: 'forceClassicEngine'
        },
        command: {
            find: 'orders',
            filter: { customerId: '1001' },
            lsid: { id: UUID('01f0e506-a52a-429e-9129-481cb2d75786') },
            '$clusterTime': {
            clusterTime: Timestamp({ t: 1758269706, i: 2 }),
            signature: {
            hash: Binary.createFromBase64('AAAAAAAAAAAAAAAAAAAAAAAAAAA=', 0),
            keyId: 0
        }
        },
        '$db': 'ecommerce'
        },
        ok: 1,
        '$clusterTime': {
            clusterTime: Timestamp({ t: 1758269718, i: 2 }),
            signature: {
            hash: Binary.createFromBase64('AAAAAAAAAAAAAAAAAAAAAAAAAAA=', 0),
            keyId: Long('0')
        }
        },
        operationTime: Timestamp({ t: 1758269716, i: 1 })
    }


### ✅ Summary    
    Use db.orders.getShardDistribution() to check balance.    
    If all records on 1 shard → probably didn’t shard the collection before inserts.    
    Best practice: sh.enableSharding() + sh.shardCollection() before bulk inserts.

### GET http://localhost:9395/api/v1/orders/orders?page=1&size=20

    {
    "page": 1,
    "content": [
        {
            "orderId": "68cd235708b6a27d1dc4626d",
            "customerId": "66906",
            "amount": 67006.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46269",
            "customerId": "66902",
            "amount": 67002.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4626a",
            "customerId": "66903",
            "amount": 67003.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46268",
            "customerId": "66901",
            "amount": 67001.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4626b",
            "customerId": "66904",
            "amount": 67004.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46267",
            "customerId": "66900",
            "amount": 67000.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46266",
            "customerId": "66899",
            "amount": 66999.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4627b",
            "customerId": "66920",
            "amount": 67020.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4627c",
            "customerId": "66921",
            "amount": 67021.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4627d",
            "customerId": "66922",
            "amount": 67022.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46275",
            "customerId": "66914",
            "amount": 67014.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46276",
            "customerId": "66915",
            "amount": 67015.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4627e",
            "customerId": "66923",
            "amount": 67023.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46263",
            "customerId": "66896",
            "amount": 66996.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46281",
            "customerId": "66926",
            "amount": 67026.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46280",
            "customerId": "66925",
            "amount": 67025.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc4626c",
            "customerId": "66905",
            "amount": 67005.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46265",
            "customerId": "66898",
            "amount": 66998.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46264",
            "customerId": "66897",
            "amount": 66997.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        },
        {
            "orderId": "68cd235708b6a27d1dc46262",
            "customerId": "66895",
            "amount": 66995.0,
            "orderDate": "2025-09-19T09:33:11.362+00:00"
        }
    ],
    "size": 20,
    "totalElements": 67008,
    "totalPages": 3351,
    "last": false
}

### GET http://localhost:9395/api/v1/shards/totalShardsCount/orders
    {
        "shard2": 42008,
        "shard1": 5000,
        "shard3": 55008,
        "totalDocuments": 102016
}   
