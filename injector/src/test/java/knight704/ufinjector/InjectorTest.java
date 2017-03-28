package knight704.ufinjector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test Injector from ComponentCache point of view, since Injector implements this interface.
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class InjectorTest {
    @Mock
    protected ComponentFactory mMockFactory;
    @Mock
    protected MockComponent mMockComponent;
    private Injector mInjector;

    @Before
    public void setUp() {
        mInjector = new Injector();
        when(mMockFactory.create()).thenReturn(mMockComponent);
    }

    @Test
    public void testFirstGetShouldCreateComponentFromScratch() throws Exception {
        mInjector.getOrCreate(MockComponent.class, mMockFactory);

        verify(mMockFactory, times(1)).create();
        assertThat(mInjector.getComponentGroups().size(), is(1));
    }

    @Test
    public void testNextGetShouldReturnCachedInstance() throws Exception {
        MockComponent comp1 = mInjector.getOrCreate(MockComponent.class, mMockFactory);
        MockComponent comp2 = mInjector.getOrCreate(MockComponent.class, mMockFactory);
        MockComponent comp3 = mInjector.getOrCreate(MockComponent.class, mMockFactory);

        verify(mMockFactory, times(1)).create();
        assertThat(mInjector.getComponentGroups().size(), is(1));
        assertTrue(comp1 == comp2 && comp2 == comp3);
    }

    @Test
    public void testGetByKeyShouldSaveMultipleComponentOfSameType() throws Exception {
        mInjector.getOrCreate(MockComponent.class, "key1", mMockFactory);
        mInjector.getOrCreate(MockComponent.class, "key2", mMockFactory);

        Map<Class, Map<String, Object>> componentGroups = mInjector.getComponentGroups();
        assertThat(componentGroups.size(), is(1));
        Map<String, Object> componentMap = componentGroups.get(MockComponent.class);
        assertThat(componentMap.size(), is(2));
        assertTrue(componentMap.containsKey("key1") && componentMap.containsKey("key2"));
    }

    @Test
    public void testReleaseShouldRemoveComponentFromCache() throws Exception {
        mInjector.getOrCreate(MockComponent.class, mMockFactory);

        assertThat(mInjector.getComponentGroups().get(MockComponent.class).size(), is(1));

        mInjector.release(MockComponent.class);

        assertThat(mInjector.getComponentGroups().get(MockComponent.class).size(), is(0));
    }

    private static class MockComponent {
    }
}
