use config

function printChunkMap(ns) {
  const chunks = db.chunks.find({ ns }).sort({ "min.customerId": 1 }).toArray();

  print("Chunk distribution for:", ns);
  print("=".repeat(70));
  print("| Range (min → max)                      | Shard   |");
  print("-".repeat(70));

  chunks.forEach(c => {
    const min = JSON.stringify(c.min.customerId);
    const max = JSON.stringify(c.max.customerId);
    const shard = c.shard;
    print(`| ${min} → ${max} | ${shard}`);
  });

  print("=".repeat(70));
}

printChunkMap("ecommerce.orders");


///**2️⃣ Example Output
//  Chunk distribution for: ecommerce.orders
//  ======================================================================
//  | Range (min → max)                      | Shard   |
//  ----------------------------------------------------------------------
//  | MinKey → -5000000000000000000          | shard1  |
//  | -5000000000000000000 → 0               | shard2  |
//  | 0 → MaxKey                             | shard3  |
// ======================================================================
// **/