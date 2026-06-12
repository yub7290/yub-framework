package com.yub.framework.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 密码强度校验注解
 * <p>校验规则：大写字母、小写字母、数字、特殊字符四种类型至少满足三种</p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-12
 * @Description: 密码强度校验注解
 * @Version: 1.0.0
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface PasswordStrength {

    String message() default "密码必须包含大写字母、小写字母、数字、特殊字符中至少三种";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
