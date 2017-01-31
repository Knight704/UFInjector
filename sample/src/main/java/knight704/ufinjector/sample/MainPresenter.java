package knight704.ufinjector.sample;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainPresenter {
    private View mView;
    private String mLastMessage;
    private GreetingsGenerator mGenerator;

    @Inject
    public MainPresenter(GreetingsGenerator greetingsGenerator) {
        mGenerator = greetingsGenerator;
    }

    public void attachView(View view) {
        mView = view;
        mView.showHello(mLastMessage != null ? mLastMessage : "Tap to see greeting");
    }

    public void detachView() {
        mView = null;
    }

    public void onClick() {
        mLastMessage = mGenerator.nextGreeting();
        mView.showHello(mLastMessage);
    }

    public interface View {
        void showHello(String message);
    }
}
