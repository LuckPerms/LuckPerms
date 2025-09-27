package me.lucko.luckperms.common.util;

import java.util.Optional;

public class InetParser {
    public static class Address {
        public String address;
        public Optional<String> port;

        @Override
        public String toString() {
            return port.map(p -> address + ":" + p).orElse(address);
        }
    }

    public static Address parseAddress(String input) {
        System.out.println("Input: " + input);
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Empty address passed");
        }

        Address result = new Address();

        if (input.startsWith("[")) {
            // [addr]:port
            int end = input.indexOf("]");

            if (end == -1) {
                throw new IllegalArgumentException("Invalid IPv6 address: terminator ']' is missing");
            }

            result.address = String.format("[%s]", input.substring(1, end));

            if (end + 1 < input.length() && input.charAt(end + 1) == ':') {
                result.port = Optional.of(input.substring(end + 2));
            } else {
                result.port = Optional.empty();
            }
        } else {
            // IPv4/hostname
            int colon = input.lastIndexOf(':');

            if (colon != -1) {
                result.address = input.substring(0, colon);
                result.port = Optional.of(input.substring(colon + 1));
            } else {
                result.address = input;
                result.port = Optional.empty();
            }
        }

        System.out.println("Output parsed: " + result);

        return result;
    }
}
