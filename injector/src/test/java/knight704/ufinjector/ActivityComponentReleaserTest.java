package knight704.ufinjector;

import android.app.Activity;
import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import knight704.ufinjector.releasers.ActivityComponentReleaser;
import knight704.ufinjector.releasers.ComponentReleaser;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActivityComponentReleaserTest {
    @Mock
    protected Activity mMockActivity;
    @Mock
    protected Application mMockApp;

    private ActivityComponentReleaser mActivityReleaser;

    @Before
    public void setUp() throws Exception {
        when(mMockActivity.getApplication()).thenReturn(mMockApp);
        mActivityReleaser = new ActivityComponentReleaser(mMockActivity);
    }

    @Test
    public void testRegisterReleaserShouldAddActivityLifecycleCallbacks() throws Exception {
        mActivityReleaser.onRegisterReleaser(mock(ComponentReleaser.OnReleaseListener.class));
        verify(mMockApp).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    @Test
    public void testShouldFireOnReleaseWhenActivityStopped() throws Exception {
        ReleaserEnv releaserEnv = registerReleaser();
        releaserEnv.activityCallbacks.onActivityStopped(mMockActivity);

        verify(releaserEnv.releaseListener).onRelease(anyBoolean());
    }

    @Test
    public void testShouldFireOnReleaseOnlyForProvidedActivity() throws Exception {
        ReleaserEnv releaserEnv = registerReleaser();
        Activity anotherActivity = mock(Activity.class);
        releaserEnv.activityCallbacks.onActivityStopped(anotherActivity);

        verify(releaserEnv.releaseListener, times(0)).onRelease(anyBoolean());
    }

    @Test
    public void testConfigChangeShouldFireOnReleaseWithCanRetain() throws Exception {
        when(mMockActivity.isFinishing()).thenReturn(false);
        when(mMockActivity.isChangingConfigurations()).thenReturn(true);
        ReleaserEnv releaserEnv = registerReleaser();
        releaserEnv.activityCallbacks.onActivityStopped(mMockActivity);

        verify(releaserEnv.releaseListener).onRelease(true);
    }

    @Test
    public void testFinishingActivityShouldFireOnReleaseWithCannotRetain() throws Exception {
        when(mMockActivity.isFinishing()).thenReturn(true);
        ReleaserEnv releaserEnv = registerReleaser();
        releaserEnv.activityCallbacks.onActivityStopped(mMockActivity);

        verify(releaserEnv.releaseListener).onRelease(false);
    }

    @Test
    public void testUnregisterReleaserShouldRemoveActivityLifecycleCallbacks() throws Exception {
        mActivityReleaser.onUnregisterReleaser();
        verify(mMockApp).unregisterActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    private ReleaserEnv registerReleaser() {
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> alcCaptor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);
        ComponentReleaser.OnReleaseListener mockListener = mock(ComponentReleaser.OnReleaseListener.class);
        mActivityReleaser.onRegisterReleaser(mockListener);
        verify(mMockApp).registerActivityLifecycleCallbacks(alcCaptor.capture());

        return new ReleaserEnv(alcCaptor.getValue(), mockListener);
    }

    private static class ReleaserEnv {
        public Application.ActivityLifecycleCallbacks activityCallbacks;
        public ComponentReleaser.OnReleaseListener releaseListener;

        public ReleaserEnv(Application.ActivityLifecycleCallbacks activityCallbacks, ComponentReleaser.OnReleaseListener releaseListener) {
            this.activityCallbacks = activityCallbacks;
            this.releaseListener = releaseListener;
        }
    }
}
