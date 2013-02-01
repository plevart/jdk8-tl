package test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@RuntimeAnnotationB
public @interface RuntimeAnnotationA {
}
