package io.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityType("order-item")
@EntityKey("orderItemId")
@RequestMapping("/order-item/{orderItemId}")
public class OrderItemEntity extends EventSourcedEntity<OrderItemEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(OrderItemEntity.class);
  private final String entityId;

  @Override
  public State emptyState() {
    return State.empty();
  }

  public OrderItemEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody CreateOrderItemCommand command) {
    log.info("EntityId: {}\nState: {}\nCommand: {}", entityId, currentState(), command);
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping
  public Effect<State> get() {
    log.info("EntityId: {}\nState: {}", entityId, currentState());
    return effects().reply(currentState());
  }

  @EventHandler
  public State handle(OrderItemCreatedEvent event) {
    log.info("EntityId: {}\nState: {}\nEvent: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(String productId, String orderId, String name, String description, int quantity) {
    static State empty() {
      return new State("", "", "", "", 0);
    }

    OrderItemCreatedEvent eventFor(CreateOrderItemCommand command) {
      return new OrderItemCreatedEvent(command.productId(), command.orderId(), command.name(), command.description(), command.quantity());
    }

    State on(OrderItemCreatedEvent event) {
      return new State(event.productId(), event.orderId(), event.name(), event.description(), event.quantity());
    }
  }

  public record CreateOrderItemCommand(String productId, String orderId, String name, String description, int quantity) {
    String entityId() {
      return "%s_%s".formatted(productId, orderId);
    }
  }

  public record OrderItemCreatedEvent(String productId, String orderId, String name, String description, int quantity) {}
}
