package io.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import kalix.javasdk.action.Action;

@RequestMapping("/http-echo")
public class HttpEchoAction extends Action {
  // slf4j logger
  private static final Logger log = LoggerFactory.getLogger(HttpEchoAction.class);

  @GetMapping("/{msg}")
  public Effect<String> echo(@PathVariable String msg) {
    log.info("Echoing request");
    return effects().reply("Echo: %s".formatted(msg));
  }

  @GetMapping("/error")
  public Effect<String> error() {
    log.info("Throwing error");
    return effects().error("Error");
  }
}
