package Register;

public class Quest {
    private String text;
    private questHandle handle;
    private boolean positiveNeed;
    private boolean negativeNeed;

    public Quest(String body, questHandle handle, boolean positive, boolean negative) {
        text = body;
        this.handle = handle;
        positiveNeed = positive;
        negativeNeed = negative;
    }

    public boolean isNegativeNeed() {
        return negativeNeed;
    }

    public boolean isPositiveNeed() {
        return positiveNeed;
    }

    public questHandle getHandle() {
        return handle;
    }

    public String getText() {
        return text;
    }
}
