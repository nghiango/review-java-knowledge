package lab.restapi.questions;

import java.util.Locale;
import org.springframework.http.HttpMethod;

public class Q04RestVsRpc {

    public static void main(String[] args) {
        // RPC Style: Verbs/actions in URI paths, often everything uses POST/GET
        String rpcCreateOrder = "/api/createOrder"; // RPC: action in URI
        String rpcCancelOrder = "/api/cancelOrder?orderId=123"; // RPC: procedural trigger
        String rpcGetUserDetails = "/api/getUserDetails"; // RPC: procedural query

        // REST Style: Resource nouns, standard HTTP verbs for operations
        String restCreateOrder = "POST /api/orders"; // REST: create resource in collection
        String restGetOrder = "GET /api/orders/123"; // REST: retrieve specific resource
        String restCancelOrder =
                "POST /api/orders/123/cancellation"; // REST: state sub-resource or DELETE

        boolean isRestfulVerbInUri =
                rpcCreateOrder
                        .toLowerCase(Locale.ROOT)
                        .contains("create"); // true (RPC anti-pattern)
        boolean isRestStandardVerb = HttpMethod.POST.name().equals("POST"); // true

        System.out.println(
                "RPC URI examples: "
                        + rpcCreateOrder
                        + ", "
                        + rpcCancelOrder
                        + ", "
                        + rpcGetUserDetails); // RPC URI examples: /api/createOrder,
        // /api/cancelOrder?orderId=123, /api/getUserDetails
        System.out.println(
                "REST URI examples: "
                        + restCreateOrder
                        + ", "
                        + restGetOrder
                        + ", "
                        + restCancelOrder); // REST URI examples: POST /api/orders, GET
        // /api/orders/123, POST /api/orders/123/cancellation
        System.out.println(
                "RPC contains verb in URI: "
                        + isRestfulVerbInUri); // RPC contains verb in URI: true
        System.out.println(
                "REST maps creation to standard verb: "
                        + isRestStandardVerb); // REST maps creation to standard verb: true
    }
}
