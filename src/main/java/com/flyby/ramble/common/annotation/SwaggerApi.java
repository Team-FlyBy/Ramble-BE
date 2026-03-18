package com.flyby.ramble.common.annotation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger API annotation.
 * <p>
 * Swagger의 {@link Operation}와 {@link ApiResponse} annotations을 조합한 Custom Annotation
 * <p>
 * {@code summary}, {@code description}, {@code responseCode}, {@code responseDescription} and {@code content}
 * <p>
 * Usage:
 * <pre>
 *  SwaggerApi(
 *      summary = "Summary of the API",
 *      description = "Description of the API",
 *      responseCode = "200", // 200이면 생략 가능
 *      responseDescription = "Successful operation" // 생략 가능
 *  )
 *  public ResponseEntity getUser(@PathVariable Long id) {
 *       // ...
 *  }
 * </pre>
 * <p>
 *     <b>NOTE:</b> {@code responseCode} and {@code responseDescription} are optional.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation
@ApiResponse
public @interface SwaggerApi {
    @AliasFor(annotation = Operation.class, attribute = "summary")
    String summary() default "Default Summary";

    @AliasFor(annotation = Operation.class, attribute = "description")
    String description() default "Default Description";

    @AliasFor(annotation = ApiResponse.class, attribute = "responseCode")
    String responseCode() default "200";

    @AliasFor(annotation = ApiResponse.class, attribute = "description")
    String responseDescription() default "OK";

}
