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

@EntityKey("productId")
@EntityType("product")
@RequestMapping("/product/{productId}")
public class ProductEntity extends EventSourcedEntity<ProductEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(ProductEntity.class);
  private final String entityId;

  public ProductEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody CreateProductCommand command) {
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
  public State handle(ProductCreatedEvent event) {
    log.info("EntityId: {}\nState: {}\nEvent: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(String productId, String name, String description, double price) {
    static State empty() {
      return new State("", "", "", 0.0);
    }

    public State on(ProductCreatedEvent event) {
      return new State(event.productId, event.name, event.description, event.price);
    }

    public ProductCreatedEvent eventFor(CreateProductCommand command) {
      return new ProductCreatedEvent(command.productId, command.name, command.description, command.price);
    }
  }

  public record CreateProductCommand(String productId, String name, String description, int price) {}

  public record ProductCreatedEvent(String productId, String name, String description, int price) {}
}
