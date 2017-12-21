package edu.cornell.cs.nlp.spf.data.situated.labeled;

import java.util.Objects;

public class CLEVRAnswer {

    private final Object answer;

    public CLEVRAnswer(Object answer) {
        this.answer = answer;
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
