package com.laraid.ecommerce.repo;

import com.laraid.ecommerce.domain.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerId(String customerId);

}

