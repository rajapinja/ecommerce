package com.laraid.ecommerce.controller;

import com.laraid.ecommerce.domain.Order;
import com.laraid.ecommerce.dto.OrderDto;
import com.laraid.ecommerce.dto.PagedResponse;
import com.laraid.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody Order order) {
        OrderDto savedOrder = orderService.createOrder(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getallOrders(){
        List<OrderDto> listOrders = orderService.getAllOrders();
        return new ResponseEntity<>(listOrders, HttpStatus.FOUND);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<OrderDto>> bulkInserts() {
        List<OrderDto> savedOrders = orderService.bulkInsert();
        return new ResponseEntity<>(savedOrders, HttpStatus.CREATED);
    }

    @GetMapping("/orders")
    public ResponseEntity<PagedResponse<OrderDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderDto> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(new PagedResponse<>(orders));
    }
}

