package knight704.ufinjector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import dagger.Component;
import knight704.ufinjector.releasers.ComponentReleaser;

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
    protected ComponentReleaser mMockReleaser;
    @Mock
    protected MockComponent mMockComponent;
    @Mock
    protected ComponentFactory mMockFactory;

    private InjectRequest prepareRequest() {
        return new InjectRequest(mMockComponentCache, mMockReleaser);
    }

    @Before
    public void setUp() {
        when(mMockFactory.create()).thenReturn(mMockComponent);
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
    public void testComponentShouldRelease() throws Exception {
        ArgumentCaptor<ComponentReleaser.OnReleaseListener> releaseListenerCaptor = ArgumentCaptor.forClass(ComponentReleaser.OnReleaseListener.class);

        prepareRequest().build(MockComponent.class, mMockFactory);

        dispatchOnRelease(releaseListenerCaptor, false);
        verify(mMockComponentCache).release(MockComponent.class);
    }

    @Test
    public void testRetainOnConfigChangeShouldNotRelease() throws Exception {
        ArgumentCaptor<ComponentReleaser.OnReleaseListener> releaseListenerCaptor = ArgumentCaptor.forClass(ComponentReleaser.OnReleaseListener.class);

        prepareRequest()
                .retainOnConfigChange(true)
                .build(MockComponent.class, mMockFactory);

        dispatchOnRelease(releaseListenerCaptor, true);
        verify(mMockComponentCache, times(0)).release(MockComponent.class);
    }

    @Test
    public void testRetainOnConfigChangeShouldReleaseIfCantRetain() throws Exception {
        ArgumentCaptor<ComponentReleaser.OnReleaseListener> releaseListenerCaptor = ArgumentCaptor.forClass(ComponentReleaser.OnReleaseListener.class);

        prepareRequest()
                .retainOnConfigChange(true)
                .build(MockComponent.class, mMockFactory);

        dispatchOnRelease(releaseListenerCaptor, false);
        verify(mMockComponentCache).release(MockComponent.class);
    }

    private void dispatchOnRelease(ArgumentCaptor<ComponentReleaser.OnReleaseListener> captor, boolean canRelease) {
        verify(mMockReleaser).onRegisterReleaser(captor.capture());
        captor.getValue().onRelease(canRelease);
        verify(mMockReleaser).onUnregisterReleaser();
    }

    private static class NotComponent {
    }

    @Component
    private static class MockComponent {
    }
}
