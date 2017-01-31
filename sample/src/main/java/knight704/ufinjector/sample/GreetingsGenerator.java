package knight704.ufinjector.sample;

import javax.inject.Inject;

public class GreetingsGenerator {
    private String[] mGreetings = {"Hello", "Salut", "Oi"};
    private int mCurrent;

    @Inject
    public GreetingsGenerator() {
    }

    public String nextGreeting() {
        String result = mGreetings[mCurrent++];
        if (mCurrent >= mGreetings.length) {
            mCurrent = 0;
        }
        return result;
    }
}
