package edu.cornell.cs.nlp.spf.data.situated.labeled;

public class CLEVRExample {

    private final CLEVRScene scene;
    private final String question;
    private final String questionLf;
    private final String answer;

    public CLEVRExample(CLEVRScene scene, String question, String questionLf, String answer) {
        this.scene = scene;
        this.question = question;
        this.questionLf = questionLf;
        this.answer = answer;
    }
}
