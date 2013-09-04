/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.SizeOf;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author peter
 */
public class SpaceTest {

    // no annotations

    public static class Unannotated {
    }

    // typical annotations (JPA-like)

    @Target(TYPE) @Retention(RUNTIME)
    public @interface Entity {
        String name() default "";
    }

    @Target(TYPE) @Retention(RUNTIME)
    public @interface Table {
        String name() default "";
        String catalog() default "";
        String schema() default "";
        UniqueConstraint[] uniqueConstraints() default {};
    }

    @Target({}) @Retention(RUNTIME)
    public @interface UniqueConstraint {
        String[] columnNames();
    }

    @Entity(name = "EntityName")
    @Table(
        name = "table_name",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"code"})
        }
    )
    public static class JpaEntity {
    }

    // inherited annotations

    @Target(TYPE) @Retention(RUNTIME)
    public @interface PlainAnn {
        String value();
    }

    @Target(TYPE) @Retention(RUNTIME) @Inherited
    public @interface IngeritableAnn {
        String value();
    }

    @PlainAnn("super")
    @IngeritableAnn("super")
    public static class AnnotatedSuper {
    }

    @PlainAnn("sub")
    public static class AnnotatedSub {
    }


    static void doTest(Class<?> clazz, SizeOf sizeOf) {
        System.out.println("\n" + clazz.getName() + ":");
        long preSize = sizeOf.deepSizeOf(clazz);
        System.out.println("   pre: " +  preSize + " bytes");

        clazz.getAnnotations();
        long postSize = sizeOf.deepSizeOf(clazz);
        System.out.println("  post: " + postSize + " (" + preSize + "+" + (postSize - preSize) + ") bytes");
    }

    public static void main(String[] args) {

        SizeOf sizeOf = new SizeOf(args.length > 0 ? SizeOf.Visitor.STDOUT : SizeOf.Visitor.NULL, Collections.emptyMap());

        doTest(Unannotated.class, sizeOf);
        doTest(JpaEntity.class, sizeOf);
        doTest(AnnotatedSuper.class, sizeOf);
        doTest(AnnotatedSub.class, sizeOf);
    }
}
