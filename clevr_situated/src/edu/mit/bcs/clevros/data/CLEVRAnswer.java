package edu.mit.bcs.clevros.data;

import org.json.simple.JSONObject;

import java.util.Objects;

public class CLEVRAnswer {

    private static final CLEVRAnswer TRUE = new CLEVRAnswer(true);
    private static final CLEVRAnswer FALSE = new CLEVRAnswer(false);

    private final Object answer;

    public CLEVRAnswer(Object answer) {
        this.answer = answer;
    }

    public static CLEVRAnswer valueOf(Object expr, CLEVRScene scene) {
        if (expr instanceof String) {
            String exprStr = (String) expr;

            if (exprStr.equals("yes") || exprStr.equals("true")) {
                return TRUE;
            } else if (exprStr.equals("no") || exprStr.equals("false")) {
                return FALSE;
            }

            try {
                int ret = Integer.valueOf(exprStr);
                return new CLEVRAnswer(ret);
            } catch (NumberFormatException e) {
                return new CLEVRAnswer(exprStr);
            }
        } else if (expr instanceof JSONObject) {
            JSONObject exprObj = (JSONObject) expr;
            String typeString = (String) exprObj.get("type");
            if (typeString.equals("object")) {
                int idx = ((Long) exprObj.get("index")).intValue();
                return new CLEVRAnswer(scene.getObjects().get(idx));
            }
        }

        throw new IllegalArgumentException("unknown answer type " + expr.toString());
    }

    public boolean hasSameType(Object other) {
        return this.answer.getClass() == other.getClass();
    }

    public Object getAnswer() {
        return answer;
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
