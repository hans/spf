package edu.cornell.cs.nlp.spf.data.situated;

public class CLEVRAnswer {

    private final Object answer;

    public CLEVRAnswer(Object answer) {
        this.answer = answer;
    }

    public boolean equals(Object other) {
        return other != null && hasSameType(other)
                && this.answer.equals(other);
    }

    public boolean hasSameType(Object other) {
        return this.answer.getClass() == other.getClass();
    }

}
