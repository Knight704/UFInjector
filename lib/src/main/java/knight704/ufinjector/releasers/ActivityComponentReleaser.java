package knight704.ufinjector.releasers;

import android.app.Activity;
import android.app.Application;

import knight704.ufinjector.ActivityLifecycleCallbacksAdapter;

public class ActivityComponentReleaser implements ComponentReleaser {
    private Activity mActivity;
    private Application.ActivityLifecycleCallbacks mActivityCallbacks;

    public ActivityComponentReleaser(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onRegisterReleaser(final OnReleaseListener listener) {
        mActivityCallbacks = new ActivityLifecycleCallbacksAdapter() {
            @Override
            public void onActivityStopped(Activity activity) {
                if (mActivity == activity) {
                    listener.onRelease(!activity.isFinishing() && activity.isChangingConfigurations());
                }
            }
        };

        Application app = mActivity.getApplication();
        app.registerActivityLifecycleCallbacks(mActivityCallbacks);
    }

    @Override
    public void onUnregisterReleaser() {
        Application app = mActivity.getApplication();
        app.unregisterActivityLifecycleCallbacks(mActivityCallbacks);
        mActivity = null;
    }
}
