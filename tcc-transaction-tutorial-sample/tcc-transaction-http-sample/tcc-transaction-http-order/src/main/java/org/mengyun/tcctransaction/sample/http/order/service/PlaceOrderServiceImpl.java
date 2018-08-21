package org.mengyun.tcctransaction.sample.http.order.service;

import org.mengyun.tcctransaction.CancellingException;
import org.mengyun.tcctransaction.ConfirmingException;
import org.mengyun.tcctransaction.sample.http.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.http.order.domain.entity.Shop;
import org.mengyun.tcctransaction.sample.http.order.domain.repository.ShopRepository;
import org.mengyun.tcctransaction.sample.http.order.domain.service.OrderServiceImpl;
import org.mengyun.tcctransaction.sample.http.order.domain.service.PaymentServiceImpl;
import org.mengyun.tcctransaction.sample.http.order.web.controller.vo.PlaceOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付订单服务实现
 *
 * Created by changming.xie on 4/1/16.
 */
@Service
public class PlaceOrderServiceImpl {

    @Autowired
    ShopRepository shopRepository;

    @Autowired
    OrderServiceImpl orderService;

    @Autowired
    PaymentServiceImpl paymentService;

    public String placeOrder(PlaceOrderRequest request) {
        // 获取商店
        Shop shop = shopRepository.findById(request.getShopId());
        // 创建订单(构造订单对象，插入订单表 【订单状态为草稿】)
        Order order = orderService.createOrder(request.getPayerUserId(), shop.getOwnerUserId(), request.getProductQuantities());
        // 发起支付
        Boolean result = false;
        try {
            paymentService.makePayment(order, request.getRedPacketPayAmount(), order.getTotalAmount().subtract(request.getRedPacketPayAmount()));
        } catch (ConfirmingException confirmingException) {
            // exception throws with the tcc transaction status is CONFIRMING,
            // when tcc transaction is confirming status,
            // the tcc transaction recovery will try to confirm the whole transaction to ensure eventually consistent.
            result = true;
        } catch (CancellingException cancellingException) {
            // exception throws with the tcc transaction status is CANCELLING,
            // when tcc transaction is under CANCELLING status,
            // the tcc transaction recovery will try to cancel the whole transaction to ensure eventually consistent.
        } catch (Throwable e) {
            // other exceptions throws at TRYING stage.
            // you can retry or cancel the operation.
            e.printStackTrace();
        }
        return order.getMerchantOrderNo();
    }

}
