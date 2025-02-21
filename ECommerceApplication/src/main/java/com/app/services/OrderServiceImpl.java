package com.app.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.app.config.AppConstants;
import com.app.entites.Address;
import com.app.entites.Cart;
import com.app.entites.CartItem;
import com.app.entites.Coupon;
import com.app.entites.Order;
import com.app.entites.OrderItem;
import com.app.entites.Payment;
import com.app.entites.Product;
import com.app.entites.Member;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.AddressDTO;
import com.app.payloads.OrderDTO;
import com.app.payloads.OrderItemDTO;
import com.app.payloads.OrderResponse;
import com.app.repositories.AddressRepo;
import com.app.repositories.CartItemRepo;
import com.app.repositories.CartRepo;
import com.app.repositories.CouponRepo;
import com.app.repositories.OrderItemRepo;
import com.app.repositories.OrderRepo;
import com.app.repositories.PaymentRepo;
import com.app.repositories.UserRepo;
import com.app.repositories.MemberRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	public UserRepo userRepo;

	@Autowired
	public CartRepo cartRepo;

	@Autowired
	public OrderRepo orderRepo;

	@Autowired
	private PaymentRepo paymentRepo;

	@Autowired
	public OrderItemRepo orderItemRepo;

	@Autowired
	public CartItemRepo cartItemRepo;

	@Autowired
	private AddressRepo addressRepo;

	@Autowired
	public UserService userService;

	@Autowired
	public CartService cartService;

	@Autowired
	public ModelMapper modelMapper;

	@Autowired
	public CouponRepo couponRepo;

	@Autowired
	public MemberRepo memberRepo;

	@Override
    public OrderDTO placeOrder(String email, Long cartId, String paymentMethod, String coupon, boolean membership) {
        return processOrder(email, cartId, paymentMethod, coupon, membership, null);
    }

    @Override
    public OrderDTO placeOrder(String email, Long cartId, String paymentMethod, String coupon, boolean membership, AddressDTO addressDTO) {
        if (paymentMethod.equalsIgnoreCase("COD") && addressDTO == null) {
            throw new APIException("Address is required for Cash on Delivery (COD).");
        }
        return processOrder(email, cartId, paymentMethod, coupon, membership, addressDTO);
    }

	public OrderDTO processOrder(String email, Long cartId, String paymentMethod, String couponCode, boolean membership, AddressDTO addressDTO) {

		Long userId = userRepo.findUserIdByEmail(email);
		if (userId == null) {
			throw new ResourceNotFoundException("User", "email", email);
		}

		Member member = memberRepo.findByUserId(userId);
		boolean isMember = (member != null);
		double memberBalance = isMember ? member.getBalance() : 0.0;

		if (membership && !isMember) {
			throw new APIException("User is not a member. Please subscribe to membership first.");
		}

		Cart cart = cartRepo.findCartByEmailAndCartId(email, cartId);

		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}

		Order order = new Order();

		order.setEmail(email);
		order.setOrderDate(LocalDate.now());

		double totalAmount = cart.getTotalPrice();
		if (couponCode != null && !couponCode.isEmpty()) {
			Coupon coupon = couponRepo.findByCode(couponCode);

			System.out.println(couponCode);

			if (coupon == null || coupon.isActive()) {
				throw new APIException("Coupon is invalid");
			}

			double discount = coupon.getDiscount();
			totalAmount -= totalAmount * discount / 100;
		}

		if (membership) {
			if (memberBalance > totalAmount) {
				member.setBalance(memberBalance-totalAmount);
				totalAmount = 0.0;
			} else {
				totalAmount -= memberBalance;
				member.setBalance(0.0); 
			}
			memberRepo.save(member);
		} 
		else if (isMember) {
			double cashback = totalAmount * 0.01;
			member.setBalance(memberBalance + cashback);
			memberRepo.save(member);
		}
	
		
		if (addressDTO != null) {
			Address shippingAddress = modelMapper.map(addressDTO, Address.class);
			shippingAddress = addressRepo.save(shippingAddress);
			order.setShippingAddress(shippingAddress);
		}
		
		order.setTotalAmount(totalAmount);
		order.setOrderStatus("Order Accepted !");

		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setPaymentMethod(paymentMethod);

		payment = paymentRepo.save(payment);

		order.setPayment(payment);

		Order savedOrder = orderRepo.save(order);

		List<CartItem> cartItems = cart.getCartItems();

		if (cartItems.size() == 0) {
			throw new APIException("Cart is empty");
		}

		List<OrderItem> orderItems = new ArrayList<>();

		for (CartItem cartItem : cartItems) {
			OrderItem orderItem = new OrderItem();

			orderItem.setProduct(cartItem.getProduct());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setDiscount(cartItem.getDiscount());
			orderItem.setOrderedProductPrice(cartItem.getProductPrice());
			orderItem.setOrder(savedOrder);

			orderItems.add(orderItem);
		}

		orderItems = orderItemRepo.saveAll(orderItems);

		cart.getCartItems().forEach(item -> {
			int quantity = item.getQuantity();

			Product product = item.getProduct();

			cartService.deleteProductFromCart(cartId, item.getProduct().getProductId());

			product.setQuantity(product.getQuantity() - quantity);
		});

		OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

		orderItems.forEach(item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));



		return orderDTO;
	}

	@Override
	public List<OrderDTO> getOrdersByUser(String email) {
		List<Order> orders = orderRepo.findAllByEmail(email);

		List<OrderDTO> orderDTOs = orders.stream().map(order -> modelMapper.map(order, OrderDTO.class))
				.collect(Collectors.toList());

		if (orderDTOs.size() == 0) {
			throw new APIException("No orders placed yet by the user with email: " + email);
		}

		return orderDTOs;
	}

	@Override
	public OrderDTO getOrder(String email, Long orderId) {

		Order order = orderRepo.findOrderByEmailAndOrderId(email, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		return modelMapper.map(order, OrderDTO.class);
	}

	@Override
	public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Order> pageOrders = orderRepo.findAll(pageDetails);

		List<Order> orders = pageOrders.getContent();

		List<OrderDTO> orderDTOs = orders.stream().map(order -> modelMapper.map(order, OrderDTO.class))
				.collect(Collectors.toList());

		if (orderDTOs.size() == 0) {
			throw new APIException("No orders placed yet by the users");
		}

		OrderResponse orderResponse = new OrderResponse();

		orderResponse.setContent(orderDTOs);
		orderResponse.setPageNumber(pageOrders.getNumber());
		orderResponse.setPageSize(pageOrders.getSize());
		orderResponse.setTotalElements(pageOrders.getTotalElements());
		orderResponse.setTotalPages(pageOrders.getTotalPages());
		orderResponse.setLastPage(pageOrders.isLast());

		return orderResponse;
	}

	@Override
	public OrderDTO updateOrder(String email, Long orderId, String orderStatus) {

		Order order = orderRepo.findOrderByEmailAndOrderId(email, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		order.setOrderStatus(orderStatus);

		return modelMapper.map(order, OrderDTO.class);
	}

}