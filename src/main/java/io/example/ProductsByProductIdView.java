package io.example;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;

@ViewId("products-by-product-id")
@Table("products_by_product_id")
@Subscribe.EventSourcedEntity(value = ProductEntity.class, ignoreUnknown = true)
public class ProductsByProductIdView extends View<ProductsByProductIdView.ProductViewRow> {
  private static final Logger log = LoggerFactory.getLogger(ProductsByProductIdView.class);

  @GetMapping("/products/by-product-id/{productId}")
  // @Query("""
  // SELECT * AS products
  // FROM products_by_product_id
  // WHERE productId = :productId
  // """)
  @Query("SELECT * AS products FROM products_by_product_id WHERE productId = :productId")
  public Products getProductsByProductId(@PathVariable String productId) {
    return null;
  }

  public UpdateEffect<ProductViewRow> on(ProductEntity.ProductCreatedEvent event) {
    log.info("Event: {}", event);
    return effects().updateState(ProductViewRow.on(event));
  }

  public record ProductViewRow(String productId, String name, String description, double price) {

    static ProductViewRow on(ProductEntity.ProductCreatedEvent event) {
      return new ProductViewRow(event.productId(), event.name(), event.description(), event.price());
    }
  }

  public record Products(Collection<ProductViewRow> products) {}
}
