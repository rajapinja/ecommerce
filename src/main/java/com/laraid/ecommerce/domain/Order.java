package com.laraid.ecommerce.domain;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {
    @Id
    private String id;  // maps to Mongo _id


    //✅ Why Add @Indexed?
    //MongoDB sharding requires the shard key field (customerId) to be indexed.
    //If you shard on { customerId: "hashed" }, this index must exist.
    //Spring Boot will create the index automatically when the app starts if spring.data.mongodb.auto-index-creation=true is set.
    @Indexed
    private String customerId;
    private Double amount;
    private Date orderDate;
    private String orderId;

    // Expose Mongo's _id as orderId
    public String getOrderId() {
        return this.id;
    }
}

