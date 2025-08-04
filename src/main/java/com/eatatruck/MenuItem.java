package com.eatatruck;

public record MenuItem(
    String id,
    String name,
    String description,
    double price
) {}
