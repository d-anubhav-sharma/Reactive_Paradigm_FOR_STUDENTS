package com.user.info.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.user.info.entities.Order;
import com.user.info.entities.Product;
import com.user.info.entities.UserInfoEntity;
import com.user.info.respositories.UserInfoRepository;
import com.user.info.services.ProductService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	@Autowired
	private WebClient webClient;

	@Autowired
	UserInfoRepository userInfoRepository;

	@Value("${searchOrderByPhoneUrl}")
	private String searchOrderByPhoneUrl;

	@Value("${searchProductByProductCode}")
	private String searchProductByProductCode;

	@Override
	public Mono<Product> findProductByUserId(String userId) {
		return findProductWithHighestScore(
				Flux.from(findPhoneNumberByUserId(userId)).flatMap(phoneNumber -> findOrdersByPhoneNumber(phoneNumber))
						.flatMap(order -> findProductsByProductCode(order.getProductCode())));
	}

	@Override
	public Mono<String> findPhoneNumberByUserId(String userId) {
		return userInfoRepository.findById(userId).map(user -> user.getPhone());
	}

	@Override
	public Flux<Order> findOrdersByPhoneNumber(String phoneNumber) {
		return webClient.get().uri(searchOrderByPhoneUrl + "?phoneNumber=" + phoneNumber)
				.exchangeToFlux(response -> response.bodyToFlux(Order.class));
	}

	@Override
	public Flux<Order> findOrdersByPhoneNumber(Flux<String> phoneNumberFlux) {
		return phoneNumberFlux
				.flatMap(phoneNumber -> webClient.get().uri(searchOrderByPhoneUrl + "?phoneNumber=" + phoneNumber)
						.exchangeToFlux(response -> response.bodyToFlux(Order.class)));
	}

	@Override
	public Flux<Product> findProductsByProductCode(String productCode) {
		return webClient.get().uri(searchProductByProductCode + "?productCode=" + productCode)
				.exchangeToFlux(response -> response.bodyToFlux(Product.class));
	}

	@Override
	public Mono<Product> findProductWithHighestScore(Flux<Product> productFlux) {
		return productFlux
				.reduceWith(() -> new Product("", "", "", Double.MIN_VALUE),
						(maxProduct, product) -> maxProduct.getScore() > product.getScore() ? maxProduct : product);
	}

	@Override
	public Flux<Product> findAllProducts() {
		return findAllOrders().flatMap(order -> findProductsByProductCode(order.getProductCode()));
	}

	@Override
	public Flux<UserInfoEntity> findAllUsers() {
		return userInfoRepository.findAll();
	}

	@Override
	public Flux<Order> findAllOrders() {
		return findAllUsers().map(user -> user.getPhone()).flatMap(phone -> findOrdersByPhoneNumber(phone));
	}

	@Override
	public Flux<Order> findOrdersByUserId(String userId) {
		return findOrdersByPhoneNumber(Flux.from(findPhoneNumberByUserId(userId)));
	}

}
