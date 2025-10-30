package com.scrim_pds.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// Anotacion para resolver el usuario autenticado a traves del token
// e inyectarlo como argumento en un metodo del controlador.

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}