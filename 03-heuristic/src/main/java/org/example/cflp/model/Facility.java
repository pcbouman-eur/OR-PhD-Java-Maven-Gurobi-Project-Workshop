package org.example.cflp.model;

/** A candidate facility location with a capacity and a fixed opening cost. */
public record Facility(double x, double y, double capacity, double fixedCost) {
}
