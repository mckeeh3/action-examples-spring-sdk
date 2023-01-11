package io.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.example.ProductsByProductIdView.Products;
import kalix.javasdk.action.Action;
import kalix.springsdk.KalixClient;

@RequestMapping("/http-to-shopping-cart")
public class HttpToShoppingCartAction extends Action {
  private static final Logger log = LoggerFactory.getLogger(HttpToShoppingCartAction.class);
  private final KalixClient kalixClient;

  public HttpToShoppingCartAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  @PutMapping("/{cartId}/add-item")
  public Effect<String> addItem(@PathVariable String cartId, @RequestBody AddCartItemCommand commandIn) {
    log.info("CartId: {}\ncommandIn: {}", cartId, commandIn);

    try {
      return effects().asyncReply(queryForProduct(cartId, commandIn));
    } catch (Exception e) {
      return effects().error(e.getMessage());
    }
  }

  private CompletionStage<String> queryForProduct(String cartId, AddCartItemCommand commandIn) {
    var path = "/products/by-product-id/%s".formatted(commandIn.productId());
    var returnType = ProductsByProductIdView.Products.class;

    return kalixClient.get(path, returnType)
        .execute()
        .thenCompose(queryResponse -> addProductDetailsToCart(cartId, commandIn, queryResponse));
  }

  private CompletionStage<String> addProductDetailsToCart(String cartId, AddCartItemCommand commandIn, Products queryResponse) {
    log.info("Query response: {}", queryResponse);
    var path = "/shopping-cart/%s/add-item".formatted(cartId);
    var products = new ArrayList<ProductsByProductIdView.ProductViewRow>(queryResponse.products());
    if (products.size() == 0) {
      throw new RuntimeException("Product not found: %s".formatted(commandIn.productId()));
    }
    var product = products.get(0);
    var item = new ShoppingCartEntity.Item(commandIn.productId, product.name(), product.description(), commandIn.quantity());
    var commandOut = new ShoppingCartEntity.AddItemCommand(cartId, item);
    var returnType = String.class;
    var deferredCall = kalixClient.put(path, commandOut, returnType);

    return deferredCall.execute();
  }

  @PutMapping("/{cartId}/checkout")
  public Effect<String> checkout(@PathVariable String cartId) {
    log.info("Checkout: {}", cartId);

    var path = "/shopping-cart/%s/checkout".formatted(cartId);
    var commandOut = new ShoppingCartEntity.CheckoutCommand(cartId);
    var returnType = String.class;
    var deferredCall = kalixClient.put(path, commandOut, returnType);

    return effects().forward(deferredCall);
  }

  @GetMapping("/{cartId}")
  public Effect<Cart> get(@PathVariable String cartId) {
    log.info("Get: {}", cartId);

    var path = "/shopping-cart/%s".formatted(cartId);
    var returnType = ShoppingCartEntity.State.class;
    var result = kalixClient.get(path, returnType).execute();

    // CompletionStage<Effect<Cart>> effect = result.handle((state, error) -> {
    var effect = result.handle((state, error) -> {
      if (error == null) {
        log.info("Cart: {}", state);
        return effects().reply(Cart.stateToCart(state));
      } else {
        log.error("Error: {}", error);
        // return effects().error("Get cart failed: %s".formatted(error));
        return effects().<Cart>error("Get cart failed: %s".formatted(error));
      }
    });

    return effects().asyncEffect(effect);
  }

  public record AddCartItemCommand(String productId, int quantity) {}

  public record Cart(String id, boolean checkedOut, List<Item> items) {
    static Cart stateToCart(ShoppingCartEntity.State state) {
      return new Cart(state.cartId(), state.checkedOut(), Item.itemsToItems(state.items()));
    }
  }

  public record Item(String productId, String name, String description, int quantity) {
    static List<Item> itemsToItems(List<ShoppingCartEntity.Item> items) {
      return items.stream().map(item -> itemToItem(item)).toList();
    }

    static Item itemToItem(ShoppingCartEntity.Item item) {
      return new Item(item.productId(), item.name(), item.description(), item.quantity());
    }
  }
}
