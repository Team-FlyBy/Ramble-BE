package com.flyby.ramble.annotation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

    @AliasFor(annotation = ApiResponse.class, attribute = "content")
    Content[] content() default {
            @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Void.class)
            )
    };
}
