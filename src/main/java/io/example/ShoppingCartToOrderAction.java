package io.example;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kalix.javasdk.action.Action;
import kalix.springsdk.KalixClient;
import kalix.springsdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = ShoppingCartEntity.class, ignoreUnknown = true)
public class ShoppingCartToOrderAction extends Action {
  private static final Logger log = LoggerFactory.getLogger(ShoppingCartToOrderAction.class);
  private final KalixClient kalixClient;

  public ShoppingCartToOrderAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<String> on(ShoppingCartEntity.CheckedOutEvent event) {
    log.info("Event: {}", event);

    var path = "/order/%s/create".formatted(event.cartId());
    var command = new OrderEntity.CreateOrderCommand(event.cartId(), mapItems(event.items()));
    var returnType = String.class;
    var deferredCall = kalixClient.post(path, command, returnType);

    return effects().forward(deferredCall);
  }

  private List<OrderEntity.Item> mapItems(List<ShoppingCartEntity.Item> items) {
    return items.stream().map(item -> mapItem(item)).toList();
  }

  private OrderEntity.Item mapItem(ShoppingCartEntity.Item item) {
    return new OrderEntity.Item(item.productId(), item.name(), item.description(), item.quantity());
  }
}
