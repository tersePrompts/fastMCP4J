package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to provide enhanced parameter descriptions for MCP tools.
 * This helps LLMs understand how to pass parameters correctly by providing
 * detailed guidance, examples, and constraints.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpParam {
    
    /**
     * Detailed description of what this parameter represents.
     * Should be LLM-friendly and explain the purpose and expected format.
     */
    String description() default "";
    
    /**
     * Example values that demonstrate how to use this parameter.
     * Helps LLMs understand the expected format and structure.
     */
    String[] examples() default {};
    
    /**
     * Constraints or validation rules for this parameter.
     * Examples: "must be positive", "valid email address", "YYYY-MM-DD format"
     */
    String constraints() default "";
    
    /**
     * Whether this parameter is required (true) or optional (false).
     * If false, the parameter can be omitted from the request.
     */
    boolean required() default true;
    
    /**
     * Default value to use when the parameter is optional and not provided.
     * Only meaningful when required() is false.
     */
    String defaultValue() default "";
    
    /**
     * Additional context or hints for the LLM about how to use this parameter.
     * Can include formatting hints, related parameters, or usage tips.
     */
    String hints() default "";
}