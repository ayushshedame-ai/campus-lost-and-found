package com.campus.lostfound.util;

import com.campus.lostfound.exception.InvalidInputException;

/**
 * Functional interface: converts raw text typed by the user into a validated
 * value of type T, or throws InvalidInputException.
 * Works with lambdas and method references, e.g. {@code Validator::parseDate}.
 */
@FunctionalInterface
public interface InputParser<T> {

    T parse(String raw) throws InvalidInputException;
}
