package knight704.ufinjector;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import dagger.Component;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class InjectorTest {
    @Mock
    protected MockComponent mMockComponent;
    @Mock
    protected ComponentFactory mMockFactory;

    @Before
    public void setUp() {
        when(mMockFactory.create()).thenReturn(mMockComponent);
    }

    private Injector.InjectRequest mockInjectRequest() {
        Context mockContext = mock(Application.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        return Injector.with(mockContext);
    }

    @After
    public void resetState() {
        Injector.clearComponents();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullComponentClassOrFactoryShouldThrowException() throws Exception {
        mockInjectRequest().build(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotAnnotatedComponentShouldThrowException() throws Exception {
        mockInjectRequest().build(NotComponent.class, new ComponentFactory<NotComponent>() {
            @Override
            public NotComponent create() {
                return new NotComponent();
            }
        });
    }

    @Test
    public void testFirstAccessToInjectorShouldCreateComponentFromScratch() throws Exception {
        MockComponent component = mockInjectRequest().build(MockComponent.class, mMockFactory);

        verify(mMockFactory, times(1)).create();
        assertThat(component, is(mMockComponent));
        assertThat(Injector.getComponentGroups().size(), is(1));
    }

    @Test
    public void testNextAccessToInjectorShouldReturnCachedComponent() throws Exception {
        mockInjectRequest().build(MockComponent.class, mMockFactory);
        mockInjectRequest().build(MockComponent.class, mMockFactory);
        mockInjectRequest().build(MockComponent.class, mMockFactory);

        verify(mMockFactory, times(1)).create();
        assertThat(Injector.getComponentGroups().size(), is(1));
    }

    @Test
    public void testAllowComponentDuplicatesShouldAllowSaveComponentsOfTheSameType() throws Exception {
        mockInjectRequest().allowComponentDuplicates("key1")
                .build(MockComponent.class, mMockFactory);
        mockInjectRequest().allowComponentDuplicates("key2")
                .build(MockComponent.class, mMockFactory);

        Map<Class, Map<String, Object>> componentGroups = Injector.getComponentGroups();
        assertThat(componentGroups.size(), is(1));
        Map<String, Object> componentMap = componentGroups.get(MockComponent.class);
        assertThat(componentMap.size(), is(2));
        assertTrue(componentMap.containsKey("key1") && componentMap.containsKey("key2"));
    }

    @Test
    public void testComponentShouldReleaseAccordingToLifecycle() throws Exception {
        Application mockApp = mock(Application.class);
        when(mockApp.getApplicationContext()).thenReturn(mockApp);
        Injector.InjectRequest injectRequest = Injector.with(mockApp);
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.isFinishing()).thenReturn(true);
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> alcCaptor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);

        injectRequest.bindToLifecycle(mockActivity)
                .allowComponentDuplicates("key")
                .build(MockComponent.class, mMockFactory);

        verify(mockApp).registerActivityLifecycleCallbacks(alcCaptor.capture());
        assertThat(Injector.getComponentGroups().get(MockComponent.class).size(), is(1));
        Application.ActivityLifecycleCallbacks callback = alcCaptor.getValue();

        callback.onActivityStopped(mockActivity);
        assertThat(Injector.getComponentGroups().get(MockComponent.class).size(), is(0));
    }

    private static class NotComponent {
    }

    @Component
    public interface MockComponent {
    }
}