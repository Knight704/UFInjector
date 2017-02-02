package knight704.ufinjector;

import android.app.Activity;
import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import dagger.Component;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class InjectRequestTest {
    @Mock
    protected ComponentCache mMockComponentCache;
    @Mock
    protected Application mMockApp;
    @Mock
    protected Activity mMockActivity;
    @Mock
    protected MockComponent mMockComponent;
    @Mock
    protected ComponentFactory mMockFactory;

    private InjectRequest prepareRequest() {
        return new InjectRequest(mMockComponentCache, mMockActivity);
    }

    @Before
    public void setUp() {
        when(mMockFactory.create()).thenReturn(mMockComponent);
        when(mMockActivity.getApplication()).thenReturn(mMockApp);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullComponentClassOrFactoryShouldThrowException() throws Exception {
        prepareRequest().build(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotAnnotatedComponentShouldThrowException() throws Exception {
        prepareRequest().build(NotComponent.class, new ComponentFactory<NotComponent>() {
            @Override
            public NotComponent create() {
                return new NotComponent();
            }
        });
    }

    @Test
    public void testBuildShouldAddressCacheToGetComponent() throws Exception {
        prepareRequest().build(MockComponent.class, mMockFactory);
        verify(mMockComponentCache).getOrCreate(MockComponent.class, mMockFactory);
    }

    @Test
    public void testMultipleBuildsToSameComponentShouldReturnSameInstance() throws Exception {
        MockComponent comp1 = prepareRequest().build(MockComponent.class, mMockFactory);
        MockComponent comp2 = prepareRequest().build(MockComponent.class, mMockFactory);
        MockComponent comp3 = prepareRequest().build(MockComponent.class, mMockFactory);

        verify(mMockComponentCache, times(3)).getOrCreate(MockComponent.class, mMockFactory);
        assertTrue(comp1 == comp2 && comp2 == comp3);
    }

    @Test
    public void testAllowComponentDuplicatesShouldGetComponentByKey() throws Exception {
        prepareRequest().allowComponentDuplicates("key1")
                .build(MockComponent.class, mMockFactory);
        prepareRequest().allowComponentDuplicates("key2")
                .build(MockComponent.class, mMockFactory);

        verify(mMockComponentCache).getOrCreate(MockComponent.class, "key1", mMockFactory);
        verify(mMockComponentCache).getOrCreate(MockComponent.class, "key2", mMockFactory);
    }

    @Test
    public void testComponentShouldReleaseAccordingToActivityLifecycle() throws Exception {
        when(mMockActivity.isFinishing()).thenReturn(true);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> alcCaptor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);

        prepareRequest().build(MockComponent.class, mMockFactory);

        verify(mMockApp).registerActivityLifecycleCallbacks(alcCaptor.capture());
        alcCaptor.getValue().onActivityStopped(mMockActivity);
        verify(mMockComponentCache).release(MockComponent.class);
    }

    @Test
    public void testRetainOnConfigChangeShouldNotReleaseOnActivityConfigChange() throws Exception {
        when(mMockActivity.isChangingConfigurations()).thenReturn(true);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> alcCaptor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);

        prepareRequest()
                .retainOnConfigChange(true)
                .build(MockComponent.class, mMockFactory);

        verify(mMockApp).registerActivityLifecycleCallbacks(alcCaptor.capture());
        alcCaptor.getValue().onActivityStopped(mMockActivity);
        verify(mMockComponentCache, times(0)).release(MockComponent.class);
    }

    @Test
    public void testRetainOnConfigChangeShouldReleaseFinishingActivity() throws Exception {
        when(mMockActivity.isFinishing()).thenReturn(true);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> alcCaptor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);

        prepareRequest()
                .retainOnConfigChange(true)
                .build(MockComponent.class, mMockFactory);

        verify(mMockApp).registerActivityLifecycleCallbacks(alcCaptor.capture());
        alcCaptor.getValue().onActivityStopped(mMockActivity);
        verify(mMockComponentCache).release(MockComponent.class);
    }

    private static class NotComponent {
    }

    @Component
    private static class MockComponent {
    }
}
