package model;

public class FAQ {
    private String id;
    private String question;
    private String answer;

    public FAQ(){ }

    public FAQ(String question, String answer){
        this.question = question;
        this.answer = answer;
    }

    public String getId(){ return id; }
    public void setId(String id){ this.id = id; }

    public String getQuestion(){ return question; }
    public void setQuestion(String qn){ this.question = qn; }

    public String getAnswer(){ return answer; }
    public void setAnswer(String answer){ this.answer = answer; }

}
