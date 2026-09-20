package lab.jpahibernate.questions;

import jakarta.persistence.FetchType;

public class Q06FetchTypeDefaults {

    public static void main(String[] args) {
        // JPA Specification Defaults:
        // @ManyToOne: EAGER
        FetchType manyToOneDefault = FetchType.EAGER;
        // @OneToOne: EAGER
        FetchType oneToOneDefault = FetchType.EAGER;
        // @OneToMany: LAZY
        FetchType oneToManyDefault = FetchType.LAZY;
        // @ManyToMany: LAZY
        FetchType manyToManyDefault = FetchType.LAZY;

        boolean isManyToOneDefaultEager = (manyToOneDefault == FetchType.EAGER); // true
        boolean isOneToOneDefaultEager = (oneToOneDefault == FetchType.EAGER); // true
        boolean isOneToManyDefaultLazy = (oneToManyDefault == FetchType.LAZY); // true
        boolean isManyToManyDefaultLazy = (manyToManyDefault == FetchType.LAZY); // true

        System.out.println(
                "@ManyToOne default is EAGER: "
                        + isManyToOneDefaultEager); // @ManyToOne default is EAGER: true
        System.out.println(
                "@OneToOne default is EAGER: "
                        + isOneToOneDefaultEager); // @OneToOne default is EAGER: true
        System.out.println(
                "@OneToMany default is LAZY: "
                        + isOneToManyDefaultLazy); // @OneToMany default is LAZY: true
        System.out.println(
                "@ManyToMany default is LAZY: "
                        + isManyToManyDefaultLazy); // @ManyToMany default is LAZY: true
    }
}
