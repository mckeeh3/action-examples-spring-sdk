package io.example;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.example.OrderItemEntity.OrderItemCreatedEvent;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;

@ViewId("order-items-by-product-id")
@Table("order_items_by_product_id")
@Subscribe.EventSourcedEntity(value = OrderItemEntity.class)
public class OrderItemsByProductIdView extends View<OrderItemsByProductIdView.OrderItemViewRow> {
  private static final Logger log = LoggerFactory.getLogger(OrderItemsByProductIdView.class);

  @GetMapping("/order-items/by-product-id/{productId}")
  // TODO until run locally query parsing problem is fixed, use the following query
  // @Query("""
  // SELECT * AS orderItems
  // FROM order_items_by_product_id
  // WHERE productId = :productId
  // """)
  @Query("SELECT * AS orderItems FROM order_items_by_product_id WHERE productId = :productId")
  public OrderItems getOrderItemsByProductId(@PathVariable String productId) {
    return null;
  }

  public UpdateEffect<OrderItemViewRow> onChange(OrderItemCreatedEvent event) {
    log.info("Event {}", event);
    return effects().updateState(OrderItemViewRow.on(event));
  }

  public record OrderItemViewRow(String productId, String orderId, String name, String description, int quantity) {

    static OrderItemViewRow on(OrderItemCreatedEvent event) {
      return new OrderItemViewRow(event.productId(), event.orderId(), event.name(), event.description(), event.quantity());
    }
  }

  public record OrderItems(Collection<OrderItemViewRow> orderItems) {}
}
