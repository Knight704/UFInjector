package knight704.ufinjector.releasers.fragment;

import android.app.Activity;
import android.app.Fragment;

import knight704.ufinjector.releasers.ComponentReleaser;

public class FragmentComponentReleaser implements ComponentReleaser, FragmentLifecycleDelegate.FragmentLifecycleCallbacks {
    private FragmentLifecycleDelegate mLifecycleDelegate;
    private OnReleaseListener mReleaseListener;

    public FragmentComponentReleaser(FragmentLifecycleDelegate lifecycleDelegate) {
        mLifecycleDelegate = lifecycleDelegate;
    }

    @Override
    public void onRegisterReleaser(OnReleaseListener listener) {
        mReleaseListener = listener;
        mLifecycleDelegate.setCallbacks(this);
    }

    @Override
    public void onUnregisterReleaser() {
        mLifecycleDelegate.setCallbacks(null);
        mLifecycleDelegate = null;
        mReleaseListener = null;
    }

    @Override
    public void onStop(Fragment fragment) {
        Activity activity = fragment.getActivity();
        mReleaseListener.onRelease(!activity.isFinishing() && !fragment.isRemoving());
    }
}
