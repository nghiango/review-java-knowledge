package lab.restapi.questions;

import org.springframework.http.HttpStatus;

public class Q02HttpStatusCodesOverview {

    public static void main(String[] args) {
        // 2xx Success
        int ok = HttpStatus.OK.value(); // 200
        int created = HttpStatus.CREATED.value(); // 201
        int noContent = HttpStatus.NO_CONTENT.value(); // 204

        // 4xx Client Errors
        int badRequest = HttpStatus.BAD_REQUEST.value(); // 400
        int unauthorized = HttpStatus.UNAUTHORIZED.value(); // 401
        int forbidden = HttpStatus.FORBIDDEN.value(); // 403
        int notFound = HttpStatus.NOT_FOUND.value(); // 404
        int conflict = HttpStatus.CONFLICT.value(); // 409
        int preconditionFailed = HttpStatus.PRECONDITION_FAILED.value(); // 412
        int unprocessableEntity = HttpStatus.UNPROCESSABLE_ENTITY.value(); // 422
        int tooManyRequests = HttpStatus.TOO_MANY_REQUESTS.value(); // 429

        // 5xx Server Errors
        int internalError = HttpStatus.INTERNAL_SERVER_ERROR.value(); // 500
        int serviceUnavailable = HttpStatus.SERVICE_UNAVAILABLE.value(); // 503

        boolean is4xxClientError = HttpStatus.BAD_REQUEST.is4xxClientError(); // true
        boolean is2xxSuccessful = HttpStatus.CREATED.is2xxSuccessful(); // true

        System.out.println(
                "2xx codes: " + ok + ", " + created + ", " + noContent); // 2xx codes: 200, 201, 204
        System.out.println(
                "4xx codes: "
                        + badRequest
                        + ", "
                        + unauthorized
                        + ", "
                        + forbidden
                        + ", "
                        + notFound
                        + ", "
                        + conflict
                        + ", "
                        + preconditionFailed
                        + ", "
                        + unprocessableEntity
                        + ", "
                        + tooManyRequests); // 4xx codes: 400, 401, 403, 404, 409, 412, 422, 429
        System.out.println(
                "5xx codes: " + internalError + ", " + serviceUnavailable); // 5xx codes: 500, 503
        System.out.println("201 is 2xx: " + is2xxSuccessful); // 201 is 2xx: true
        System.out.println("400 is 4xx: " + is4xxClientError); // 400 is 4xx: true
    }
}
