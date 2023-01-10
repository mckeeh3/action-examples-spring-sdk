package io.example;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kalix.javasdk.action.Action;
import kalix.springsdk.KalixClient;
import kalix.springsdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = OrderEntity.class, ignoreUnknown = true)
public class OrderToOrderItemsAction extends Action {
  // slf4j log
  private final Logger log = LoggerFactory.getLogger(OrderToOrderItemsAction.class);
  private final KalixClient kalixClient;

  public OrderToOrderItemsAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<String> on(OrderEntity.OrderCreatedEvent event) {
    log.info("Event: {}", event);

    var results = event.items().stream()
        .map(item -> {
          var command = new OrderItemEntity.CreateOrderItemCommand(item.productId(), event.orderId(), item.name(), item.description(), item.quantity());
          var path = "/order-item/%s/create".formatted(command.entityId());
          var returnType = String.class;
          var deferredCall = kalixClient.post(path, command, returnType);

          return deferredCall.execute();
        }).toList();

    var result = CompletableFuture.allOf(results.toArray(CompletableFuture[]::new))
        .thenApply(__ -> "OK");

    return effects().asyncReply(result);
  }
}
