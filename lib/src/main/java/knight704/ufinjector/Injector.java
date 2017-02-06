package knight704.ufinjector;

import android.app.Activity;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

import knight704.ufinjector.releasers.ActivityComponentReleaser;
import knight704.ufinjector.releasers.ComponentReleaser;
import knight704.ufinjector.releasers.fragment.FragmentComponentReleaser;
import knight704.ufinjector.releasers.fragment.FragmentLifecycleDelegate;

/**
 * Created by Knight704.
 * This class is responsible for creating dagger components via convenient builder-style and keeping them in map-cache.
 */
public class Injector implements ComponentCache {
    private static Injector sInstance = new Injector();
    private Map<Class, Map<String, Object>> mComponentGroups = new HashMap<>();

    @VisibleForTesting
    Injector() {
    }

    @VisibleForTesting
    Map<Class, Map<String, Object>> getComponentGroups() {
        return mComponentGroups;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOrCreate(Class<T> componentClass, String key, ComponentFactory<T> componentFactory) {
        Map<String, Object> componentMap = mComponentGroups.get(componentClass);
        if (componentMap == null) {
            componentMap = new HashMap<>();
            mComponentGroups.put(componentClass, componentMap);
        }
        T component = (T) componentMap.get(key);
        if (component == null) {
            component = componentFactory.create();
            componentMap.put(key, component);
        }
        return component;
    }

    @Override
    public <T> T getOrCreate(Class<T> componentClass, ComponentFactory<T> componentFactory) {
        return getOrCreate(componentClass, componentClass.getName(), componentFactory);
    }

    @Override
    public void release(Class componentClass, String key) {
        Map<String, Object> componentMap = mComponentGroups.get(componentClass);
        if (componentMap != null) {
            componentMap.remove(key);
        }
    }

    @Override
    public void release(Class componentClass) {
        release(componentClass, componentClass.getName());
    }

    public static InjectRequest with(Activity activity) {
        return with(new ActivityComponentReleaser(activity));
    }

    public static InjectRequest with(FragmentLifecycleDelegate fragmentDelegate) {
        return with(new FragmentComponentReleaser(fragmentDelegate));
    }

    public static InjectRequest with(ComponentReleaser releaser) {
        return new InjectRequest(sInstance, releaser);
    }
}
