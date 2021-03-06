package org.mengyun.tcctransaction.sample.http.order.web.controller;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mengyun.tcctransaction.sample.http.order.domain.entity.Product;
import org.mengyun.tcctransaction.sample.http.order.domain.repository.ProductRepository;
import org.mengyun.tcctransaction.sample.http.order.domain.service.AccountServiceImpl;
import org.mengyun.tcctransaction.sample.http.order.domain.service.OrderServiceImpl;
import org.mengyun.tcctransaction.sample.http.order.service.PlaceOrderServiceImpl;
import org.mengyun.tcctransaction.sample.http.order.web.controller.vo.PlaceOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * Created by changming.xie on 4/1/16.
 */
@Controller
@RequestMapping("")
public class OrderController {

    @Autowired
    PlaceOrderServiceImpl placeOrderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    AccountServiceImpl accountService;

    @Autowired
    OrderServiceImpl orderService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("/index");
        return mv;
    }

    @RequestMapping(value = "/user/{userId}/shop/{shopId}", method = RequestMethod.GET)
    public ModelAndView getProductsInShop(@PathVariable long userId,
                                          @PathVariable long shopId) {
        List<Product> products = productRepository.findByShopId(shopId);

        ModelAndView mv = new ModelAndView("/shop");

        mv.addObject("products", products);
        mv.addObject("userId", userId);
        mv.addObject("shopId", shopId);

        return mv;
    }

    @RequestMapping(value = "/user/{userId}/shop/{shopId}/product/{productId}/confirm", method = RequestMethod.GET)
    public ModelAndView productDetail(@PathVariable long userId,
                                      @PathVariable long shopId,
                                      @PathVariable long productId) {

        ModelAndView mv = new ModelAndView("product_detail");

        mv.addObject("capitalAmount", accountService.getCapitalAccountByUserId(userId));
        mv.addObject("redPacketAmount", accountService.getRedPacketAccountByUserId(userId));

        mv.addObject("product", productRepository.findById(productId));

        mv.addObject("userId", userId);
        mv.addObject("shopId", shopId);

        return mv;
    }

    @RequestMapping(value = "/placeorder", method = RequestMethod.POST)
    public ModelAndView placeOrder(@RequestParam String redPacketPayAmount,
                                   @RequestParam long shopId,
                                   @RequestParam long payerUserId,
                                   @RequestParam long productId) {
        //构造订单请求对象
        PlaceOrderRequest request = buildRequest(redPacketPayAmount, shopId, payerUserId, productId);

        // 下单并支付订单 （业务主体体现，分布式事务在这里）（真想不懂老芋为啥封装好了又把他拆开了）
        String merchantOrderNo = placeOrderService.placeOrder(request);

        // 查询订单状态
        String status = orderService.getOrderStatusByMerchantOrderNo(merchantOrderNo);

        // 返回支付结果提示
        return getModelAndView(payerUserId, productId, status);
    }

    private ModelAndView getModelAndView(@RequestParam long payerUserId, @RequestParam long productId, String status) {
        String payResultTip = null;
        if ("CONFIRMED".equals(status)) {
            payResultTip = "支付成功";
        } else if ("PAY_FAILED".equals(status)) {
            payResultTip = "支付失败";
        }

        ModelAndView mv = new ModelAndView("pay_success");
        mv.addObject("payResult", payResultTip);
        // 商品信息
        mv.addObject("product", productRepository.findById(productId));
        // 资金账户金额 和 红包账户金额
        mv.addObject("capitalAmount", accountService.getCapitalAccountByUserId(payerUserId));
        mv.addObject("redPacketAmount", accountService.getRedPacketAccountByUserId(payerUserId));
        return mv;
    }


    private PlaceOrderRequest buildRequest(String redPacketPayAmount, long shopId, long payerUserId, long productId) {
        BigDecimal redPacketPayAmountInBigDecimal = new BigDecimal(redPacketPayAmount);
        if (redPacketPayAmountInBigDecimal.compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidParameterException("invalid red packet amount :" + redPacketPayAmount);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setPayerUserId(payerUserId);
        request.setShopId(shopId);
        request.setRedPacketPayAmount(new BigDecimal(redPacketPayAmount));
        //不可变的ImmutablePair对象,即:左元素不可变、右元素不可变 可以理解为不可变的Map
        request.getProductQuantities().add(new ImmutablePair<Long, Integer>(productId, 1));
        return request;
    }
}
