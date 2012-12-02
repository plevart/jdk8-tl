package test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@RuntimeAnnotationA
public @interface RuntimeAnnotationB {}
