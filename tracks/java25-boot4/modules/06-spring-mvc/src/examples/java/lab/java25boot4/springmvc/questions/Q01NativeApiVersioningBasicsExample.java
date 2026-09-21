package lab.java25boot4.springmvc.questions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public class Q01NativeApiVersioningBasicsExample {

    @RestController
    @RequestMapping("/api/products")
    static class ProductVersionController {

        @GetMapping(headers = "X-API-Version=1.0")
        public ResponseEntity<String> getProductsV1() {
            return ResponseEntity.ok("v1");
        }

        @GetMapping(headers = "X-API-Version=2.0")
        public ResponseEntity<String> getProductsV2() {
            return ResponseEntity.ok("v2");
        }
    }

    public static void main(String[] args) {
        var controller = new ProductVersionController();
        System.out.println(controller.getProductsV1().getBody()); // v1
        System.out.println(controller.getProductsV2().getBody()); // v2
    }
}
