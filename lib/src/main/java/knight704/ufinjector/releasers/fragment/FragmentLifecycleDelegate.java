package knight704.ufinjector.releasers.fragment;

import android.app.Fragment;

public class FragmentLifecycleDelegate {
    private Fragment mFragment;
    private FragmentLifecycleCallbacks mCallbacks;

    public FragmentLifecycleDelegate(Fragment fragment) {
        mFragment = fragment;
    }

    public void setCallbacks(FragmentLifecycleCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void onStop() {
        if (mCallbacks != null) {
            mCallbacks.onStop(mFragment);
        }
    }

    public interface FragmentLifecycleCallbacks {
        void onStop(Fragment fragment);
    }
}
