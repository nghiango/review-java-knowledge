package lab.jpahibernate.questions;

import jakarta.persistence.GenerationType;

public class Q03EntityMappingAnnotations {

    public static void main(String[] args) {
        GenerationType identity = GenerationType.IDENTITY;
        GenerationType sequence = GenerationType.SEQUENCE;
        GenerationType table = GenerationType.TABLE;
        GenerationType auto = GenerationType.AUTO;

        boolean identityUsesDbAutoIncrement = (identity == GenerationType.IDENTITY); // true
        boolean sequenceSupportsPreallocation = (sequence == GenerationType.SEQUENCE); // true
        boolean tableRequiresSeparateLockingTable = (table == GenerationType.TABLE); // true
        boolean autoDelegatesToProviderDefault = (auto == GenerationType.AUTO); // true

        System.out.println(
                "IDENTITY uses DB auto-increment: "
                        + identityUsesDbAutoIncrement); // IDENTITY uses DB auto-increment: true
        System.out.println(
                "SEQUENCE supports preallocation: "
                        + sequenceSupportsPreallocation); // SEQUENCE supports preallocation: true
        System.out.println(
                "TABLE requires lock table: "
                        + tableRequiresSeparateLockingTable); // TABLE requires lock table: true
        System.out.println(
                "AUTO delegates to dialect: "
                        + autoDelegatesToProviderDefault); // AUTO delegates to dialect: true
    }
}
