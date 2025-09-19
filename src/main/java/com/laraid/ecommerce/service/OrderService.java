package com.laraid.ecommerce.service;

import com.laraid.ecommerce.domain.Order;
import com.laraid.ecommerce.dto.OrderDto;
import com.laraid.ecommerce.mapper.OrderMapper;
import com.laraid.ecommerce.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;


    public OrderDto createOrder(Order order) {
        // you could add validations here before saving
        return OrderMapper.toDto(orderRepository.save(order));
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toDto)   // convert each entity to DTO
                .collect(Collectors.toList());
    }

    public Page<OrderDto> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toDto);  // Page<T> → Page<DTO>
    }

    public List<OrderDto> bulkInsert() {
        long count = orderRepository.count(); // current doc count
        List<Order> bulkOrders = new ArrayList<>();

        for (int i = 1; i <= 5000; i++) {
            long next = count + i;
            bulkOrders.add(new Order(
                    null,
                    String.valueOf(next), // customerId
                    100.0 + next,
                    new Date(),
                    String.valueOf(next)
            ));
        }

        return StreamSupport.stream(orderRepository.saveAll(bulkOrders).spliterator(), false)
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }
}

