package codes.thischwa.jdvs.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for favicon.ico, just for reducing 404 errors in the logs.
 */
@RestController
public class FaviconController {

  @GetMapping("favicon.ico")
  void returnNoFavicon() {
    // see class comment
  }
}
