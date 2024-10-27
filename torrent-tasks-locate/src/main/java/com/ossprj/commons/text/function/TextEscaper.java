package com.ossprj.commons.text.function;

public class TextEscaper {

    public String perform(final String input) {
        return input.replaceAll("\"","\\\\\"");
    }


    public static void main(String[] args) {
        final TextEscaper textEscaper = new TextEscaper();

        System.out.println(textEscaper.perform("This"));
        System.out.println(textEscaper.perform("\"That\"Thing"));


    }
}
