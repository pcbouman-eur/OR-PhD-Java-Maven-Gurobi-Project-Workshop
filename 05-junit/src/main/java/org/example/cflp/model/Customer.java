package org.example.cflp.model;

/** A customer with a demand (weight) that must be served by exactly one facility. */
public record Customer(double x, double y, double demand) {
}
