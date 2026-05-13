package com.visco.backend.services;

import org.springframework.stereotype.Service;

import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StatsService {

	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final SupplierRepository supplierRepository;
	private final PurchaseOrderRepository orderRepository;
	private final GoodReceiptRepository goodreceiptRepository;

	public StatsService(UserRepository userRepository, ProductRepository productRepository,
			SupplierRepository supplierRepository, PurchaseOrderRepository orderRepository,
			GoodReceiptRepository goodreceiptRepository) {
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.supplierRepository = supplierRepository;
		this.orderRepository = orderRepository;
		this.goodreceiptRepository = goodreceiptRepository;
	}

	public Long countProducts() {
		log.debug("Counting products");
		return productRepository.count();
	}

	public Long countSuppliers() {
		log.debug("Counting suppliers");
		return supplierRepository.count();
	}

	public Long countUsers() {
		log.debug("Counting users");
		return userRepository.count();
	}

	public Long countOrders() {
		log.debug("Counting orders");
		return orderRepository.count();
	}

	public Long countGoodReceipts() {
		log.debug("Counting good receipts");
		return goodreceiptRepository.count();
	}

}