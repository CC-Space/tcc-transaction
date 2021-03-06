package org.mengyun.tcctransaction.sample.http.order.domain.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.sample.http.capital.api.dto.CapitalTradeOrderDto;
import org.mengyun.tcctransaction.sample.http.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.http.order.domain.repository.OrderRepository;
import org.mengyun.tcctransaction.sample.http.order.infrastructure.serviceproxy.TradeOrderServiceProxy;
import org.mengyun.tcctransaction.sample.http.redpacket.api.dto.RedPacketTradeOrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Created by changming.xie on 4/1/16.
 */
@Service
public class PaymentServiceImpl {

    @Autowired
    TradeOrderServiceProxy tradeOrderServiceProxy;

    @Autowired
    OrderRepository orderRepository;

    /**
     * 本业务报错调用 cancelMakePayment，把订单状态设置为支付失败
     *
     * @param order
     * @param redPacketPayAmount
     * @param capitalPayAmount
     */
    @Compensable(confirmMethod = "confirmMakePayment", cancelMethod = "cancelMakePayment")
    @Transactional
    public void makePayment(Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {
        System.out.println("order try make payment called.time seq:" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 更新订单状态为支付中
        order.pay(redPacketPayAmount, capitalPayAmount);
        orderRepository.updateOrder(order);
        // 资金账户余额支付订单
        String result = tradeOrderServiceProxy.record(null, buildCapitalTradeOrderDto(order));
        // 红包账户余额支付订单
        String result2 = tradeOrderServiceProxy.record(null, buildRedPacketTradeOrderDto(order));
    }

    public void confirmMakePayment(Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {
        // 调试用
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("order confirm make payment called. time seq:" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 更新订单状态为支付成功
        order.confirm();
        orderRepository.updateOrder(order);
    }

    public void cancelMakePayment(Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {
        // 调试用
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("order cancel make payment called.time seq:" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 更新订单状态为支付失败
        order.cancelPayment();
        orderRepository.updateOrder(order);
    }

    private CapitalTradeOrderDto buildCapitalTradeOrderDto(Order order) {
        CapitalTradeOrderDto tradeOrderDto = new CapitalTradeOrderDto();
        tradeOrderDto.setAmount(order.getCapitalPayAmount());
        tradeOrderDto.setMerchantOrderNo(order.getMerchantOrderNo());
        tradeOrderDto.setSelfUserId(order.getPayerUserId());
        tradeOrderDto.setOppositeUserId(order.getPayeeUserId());
        tradeOrderDto.setOrderTitle(String.format("order no:%s", order.getMerchantOrderNo()));
        return tradeOrderDto;
    }

    private RedPacketTradeOrderDto buildRedPacketTradeOrderDto(Order order) {
        RedPacketTradeOrderDto tradeOrderDto = new RedPacketTradeOrderDto();
        tradeOrderDto.setAmount(order.getRedPacketPayAmount());
        tradeOrderDto.setMerchantOrderNo(order.getMerchantOrderNo());
        tradeOrderDto.setSelfUserId(order.getPayerUserId());
        tradeOrderDto.setOppositeUserId(order.getPayeeUserId());
        tradeOrderDto.setOrderTitle(String.format("order no:%s", order.getMerchantOrderNo()));
        return tradeOrderDto;
    }

}
