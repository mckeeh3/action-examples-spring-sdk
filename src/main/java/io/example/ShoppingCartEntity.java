package io.example;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("cartId")
@EntityType("shopping-cart")
@RequestMapping("/shopping-cart/{cartId}")
public class ShoppingCartEntity extends EventSourcedEntity<ShoppingCartEntity.State> {
  private static final Logger log = LoggerFactory.getLogger(ShoppingCartEntity.class);
  private final String entityId;

  public ShoppingCartEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.empty();
  }

  @PutMapping("/add-item")
  public Effect<String> addItem(@RequestBody AddItemCommand command) {
    log.info("EntityId: {}\nState: {}\nCommand: {}", entityId, currentState(), command);
    if (currentState().checkedOut) {
      return effects().error("Cannot add item to checked out cart");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");

  }

  @PutMapping("/checkout")
  public Effect<String> checkout(@RequestBody CheckoutCommand command) {
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
  public State handle(ItemAddedEvent event) {
    log.info("EntityId: {}\nState: {}\nEvent: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State handle(CheckedOutEvent event) {
    log.info("EntityId: {}\nState: {}\nEvent: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(
      String cartId,
      boolean checkedOut,
      List<Item> items) {

    static State empty() {
      return new State("", false, List.of());
    }

    ItemAddedEvent eventFor(AddItemCommand command) {
      return new ItemAddedEvent(command.cartId(), command.item());
    }

    CheckedOutEvent eventFor(CheckoutCommand command) {
      return new CheckedOutEvent(command.cartId(), items);
    }

    State on(ItemAddedEvent event) {
      var newItems = new ArrayList<Item>(items.stream().filter(i -> !i.productId().equals(event.item().productId())).toList());
      newItems.add(event.item());
      return new State(event.cartId, checkedOut, newItems);
    }

    State on(CheckedOutEvent event) {
      return new State(cartId, true, items);
    }
  }

  public record AddItemCommand(String cartId, Item item) {}

  public record ItemAddedEvent(String cartId, Item item) {}

  public record CheckoutCommand(String cartId) {}

  public record CheckedOutEvent(String cartId, List<Item> items) {}

  public record Item(String productId, String name, String description, int quantity) {}
}
