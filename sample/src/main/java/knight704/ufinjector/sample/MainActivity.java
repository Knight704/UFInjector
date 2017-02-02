package knight704.ufinjector.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Component;
import knight704.ufinjector.ComponentFactory;
import knight704.ufinjector.Injector;

public class MainActivity extends AppCompatActivity implements MainPresenter.View {
    @Inject
    protected MainPresenter mPresenter;
    private TextView mGreeting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGreeting = new TextView(this);
        mGreeting.setGravity(Gravity.CENTER);
        mGreeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.onClick();
            }
        });
        setContentView(mGreeting);
        Injector.with(this)
                .retainOnConfigChange(true)
                .build(MainComponent.class, new ComponentFactory<MainComponent>() {
                    @Override
                    public MainComponent create() {
                        return DaggerMainActivity_MainComponent.create();
                    }
                })
                .inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPresenter.attachView(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPresenter.detachView();
    }

    @Override
    public void showHello(String message) {
        mGreeting.setText(message);
    }

    @Singleton
    @Component
    public interface MainComponent {
        void inject(MainActivity activity);
    }
}
