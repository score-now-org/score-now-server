package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BetsScorePair(int home, int away) {

    private static final Pattern SCORE = Pattern.compile("([0-9]+)\\s*-\\s*([0-9]+)");

    public static BetsScorePair parse(String ss) {
        if (ss == null) {
            return null;
        }

        Matcher matcher = SCORE.matcher(ss.strip());
        if (!matcher.matches()) {
            return null;
        }

        try {
            return new BetsScorePair(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
