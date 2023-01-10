package io.example;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("orderId")
@EntityType("order")
@RequestMapping("/order/{orderId}")
public class OrderEntity extends EventSourcedEntity<OrderEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(OrderEntity.class);
  private final String entityId;

  public OrderEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody CreateOrderCommand command) {
    log.info("EntityId: {}\nState: {}\nCommand: {}", entityId, currentState(), command);
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public State handle(OrderCreatedEvent event) {
    log.info("EntityId: {}\nState: {}\nEvent: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(String orderId, List<Item> items) {
    static State empty() {
      return new State("", List.of());
    }

    public State on(OrderCreatedEvent event) {
      return new State(event.orderId, event.items);
    }

    public OrderCreatedEvent eventFor(CreateOrderCommand command) {
      return new OrderCreatedEvent(command.orderId, command.items);
    }
  }

  public record Item(String productId, String name, String description, int quantity) {}

  public record CreateOrderCommand(String orderId, List<Item> items) {}

  public record OrderCreatedEvent(String orderId, List<Item> items) {}
}
