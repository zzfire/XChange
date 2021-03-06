package com.xeiam.xchange.btcmarkets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xeiam.xchange.btcmarkets.dto.account.BTCMarketsBalance;
import com.xeiam.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import com.xeiam.xchange.btcmarkets.dto.marketdata.BTCMarketsTicker;
import com.xeiam.xchange.btcmarkets.dto.trade.BTCMarketsOrder;
import com.xeiam.xchange.btcmarkets.dto.trade.BTCMarketsOrders;
import com.xeiam.xchange.btcmarkets.dto.trade.BTCMarketsUserTrade;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.dto.marketdata.Trades.TradeSortType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.OpenOrders;
import com.xeiam.xchange.dto.trade.UserTrade;
import com.xeiam.xchange.dto.trade.UserTrades;
import com.xeiam.xchange.dto.trade.Wallet;

public final class BTCMarketsAdapters {

  public static final Comparator<LimitOrder> ASK_COMPARATOR = new Comparator<LimitOrder>() {
    @Override
    public int compare(LimitOrder o1, LimitOrder o2) {
      return o1.getLimitPrice().compareTo(o2.getLimitPrice());
    }
  };
  public static final Comparator<LimitOrder> BID_COMPARATOR = new Comparator<LimitOrder>() {
    @Override
    public int compare(LimitOrder o1, LimitOrder o2) {
      return o2.getLimitPrice().compareTo(o1.getLimitPrice());
    }
  };

  private BTCMarketsAdapters() {
  }

  public static AccountInfo adaptAccountInfo(List<BTCMarketsBalance> balances, String userName) {
    Map<String, Wallet> wallets = new HashMap<>();
    for (BTCMarketsBalance blc : balances) {
      final String currency = blc.getCurrency();
      wallets.put(currency, new Wallet(currency, blc.getBalance(), blc.getAvailable()));
    }
    return new AccountInfo(userName, wallets);
  }

  public static OrderBook adaptOrderBook(BTCMarketsOrderBook btcmarketsOrderBook, CurrencyPair currencyPair) {
    List<LimitOrder> asks = createOrders(Order.OrderType.ASK, btcmarketsOrderBook.getAsks(), currencyPair);
    List<LimitOrder> bids = createOrders(Order.OrderType.BID, btcmarketsOrderBook.getBids(), currencyPair);
    Collections.sort(bids, BID_COMPARATOR);
    Collections.sort(asks, ASK_COMPARATOR);
    return new OrderBook(btcmarketsOrderBook.getTimestamp(), asks, bids);
  }

  public static List<LimitOrder> createOrders(
      Order.OrderType orderType,
      List<BigDecimal[]> orders, CurrencyPair currencyPair
  ) {
    List<LimitOrder> limitOrders = new ArrayList<>();
    for (BigDecimal[] o : orders) {
      limitOrders.add(new LimitOrder(orderType, o[1], currencyPair, null, null, o[0]));
    }
    return limitOrders;
  }

  public static LimitOrder adaptOrder(BTCMarketsOrder o) {
    return new LimitOrder(adaptOrderType(o.getOrderSide()), o.getVolume(), new CurrencyPair(o.getInstrument(), o.getCurrency()), Long.toString(o.getId()), o.getCreationTime(), o.getPrice());
  }

  public static UserTrades adaptTradeHistory(List<BTCMarketsUserTrade> btcmarketsUserTrades, CurrencyPair currencyPair) {
    List<UserTrade> trades = new ArrayList<>();
    for (BTCMarketsUserTrade btcmarketsUserTrade : btcmarketsUserTrades) {
      trades.add(adaptTrade(btcmarketsUserTrade, currencyPair));
    }

    return new UserTrades(trades, TradeSortType.SortByID);
  }

  public static UserTrade adaptTrade(BTCMarketsUserTrade trade, CurrencyPair currencyPair) {
    final Order.OrderType type = adaptOrderType(trade.getSide());
    final String tradeId = Long.toString(trade.getId());
    final Integer orderId = null; //trade.getOrderId();
    String feeCurrency = currencyPair.counterSymbol;
    return new UserTrade(type, trade.getVolume(), currencyPair,
        trade.getPrice().abs(), trade.getCreationTime(), tradeId,
        String.valueOf(orderId), trade.getFee(),
        feeCurrency);
  }

  public static Order.OrderType adaptOrderType(BTCMarketsOrder.Side orderType) {
    return orderType.equals(BTCMarketsOrder.Side.Bid) ? Order.OrderType.BID : Order.OrderType.ASK;
  }

  public static OpenOrders adaptOpenOrders(BTCMarketsOrders openOrders) {
    List<LimitOrder> limitOrders = new ArrayList<>();
    for (BTCMarketsOrder btcmarketsOrder : openOrders.getOrders()) {
      limitOrders.add(adaptOrder(btcmarketsOrder));
    }
    return new OpenOrders(limitOrders);
  }

  public static Ticker adaptTicker(CurrencyPair currencyPair, BTCMarketsTicker t) {
    return new Ticker.Builder().currencyPair(currencyPair).last(t.getLastPrice()).bid(t.getBestBid()).ask(t.getBestAsk())
        .timestamp(t.getTimestamp()).build();
  }
}
