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


// Run
// node chunkMap.js

//3️⃣ Example Output (both versions)
//  Chunk distribution for ecommerce.orders
//  ================================================================================
//  | Range (min → max)                           | Shard   |
//  --------------------------------------------------------------------------------
//  | "MinKey" → "-5000000000000000000"           | shard1  |
//  | "-5000000000000000000" → "0"                | shard2  |
//  | "0" → "MaxKey"                              | shard3  |
//  ================================================================================