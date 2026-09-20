# Spring MVC Exercises

Hands-on exercises to practice Spring MVC architecture, custom argument resolvers, and standardized exception handling.

## Exercise 1: Implement a Custom `@CurrentUser` Argument Resolver

Implement a custom annotation `@CurrentUser` and a `HandlerMethodArgumentResolver` that extracts the authenticated user ID from an `X-User-Id` HTTP header and injects an `AuthenticatedUser` record into controller methods.

### Requirements
- Create `@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME) @interface CurrentUser {}`.
- Create `record AuthenticatedUser(String userId, String role) {}`.
- Implement `HandlerMethodArgumentResolver` verifying the presence of `@CurrentUser` and reading request headers.
- Register resolver in `WebMvcConfigurer.addArgumentResolvers()`.

??? question "Reveal solution"
    ```java
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CurrentUser {}

    public record AuthenticatedUser(String userId, String role) {}

    @Component
    public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class)
                    && parameter.getParameterType().equals(AuthenticatedUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            String userId = webRequest.getHeader("X-User-Id");
            String role = webRequest.getHeader("X-User-Role");

            if (userId == null || userId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
            }

            return new AuthenticatedUser(userId, role != null ? role : "USER");
        }
    }

    @Configuration
    public class WebMvcConfig implements WebMvcConfigurer {

        private final CurrentUserArgumentResolver currentUserArgumentResolver;

        public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
            this.currentUserArgumentResolver = currentUserArgumentResolver;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(currentUserArgumentResolver);
        }
    }
    ```

---

## Exercise 2: Build a Global RFC 9457 Validation Exception Handler

Implement a centralized `@RestControllerAdvice` method that catches `MethodArgumentNotValidException` and formats each field validation failure into an RFC 9457 `ProblemDetail` with a structured `invalid_params` property map.

### Requirements
- Handle `MethodArgumentNotValidException.class`.
- Return `ProblemDetail` with status `400 Bad Request`.
- Attach custom property `invalid_params` containing a list of objects with `field` and `message`.

??? question "Reveal solution"
    ```java
    @RestControllerAdvice
    public class ValidationExceptionHandler {

        public record ValidationError(String field, String message) {}

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
            ProblemDetail problemDetail =
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST, "One or more request parameters failed validation");
            problemDetail.setTitle("Bad Request");
            problemDetail.setType(URI.create("https://api.example.com/errors/validation-failed"));

            List<ValidationError> errors =
                    ex.getBindingResult().getFieldErrors().stream()
                            .map(err -> new ValidationError(err.getField(), err.getDefaultMessage()))
                            .toList();

            problemDetail.setProperty("invalid_params", errors);
            return problemDetail;
        }
    }
    ```
