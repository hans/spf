package edu.cornell.cs.nlp.spf.data.situated.labeled;

import java.util.Objects;

public class CLEVRAnswer {

    private static final CLEVRAnswer TRUE = new CLEVRAnswer(true);
    private static final CLEVRAnswer FALSE = new CLEVRAnswer(false);

    private final Object answer;

    public CLEVRAnswer(Object answer) {
        this.answer = answer;
    }

    public static CLEVRAnswer valueOf(String expr) {
        if (expr.equals("yes") || expr.equals("true")) {
            return TRUE;
        } else if (expr.equals("no") || expr.equals("false")) {
            return FALSE;
        }

        try {
            int ret = Integer.valueOf(expr);
            return new CLEVRAnswer(ret);
        } catch (NumberFormatException e) {
            return new CLEVRAnswer(expr);
        }
    }

    public boolean hasSameType(Object other) {
        return this.answer.getClass() == other.getClass();
    }

    @Override
    public String toString() {
        return String.format("CLEVRAnswer<%s>", answer == null ? null : answer.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CLEVRAnswer that = (CLEVRAnswer) o;
        return Objects.equals(answer, that.answer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer);
    }
}
