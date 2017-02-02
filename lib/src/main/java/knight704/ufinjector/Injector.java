package knight704.ufinjector;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Knight704.
 * This class is responsible for creating dagger components via convenient builder-style and keeping them in map-cache.
 */
public class Injector {
    private static Injector sInstance = new Injector();
    private Map<Class, Map<String, Object>> mComponentGroups = new HashMap<>();

    private Injector() {
    }

    @SuppressWarnings("unchecked")
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

    public <T> T getOrCreate(Class<T> componentClass, ComponentFactory<T> componentFactory) {
        return getOrCreate(componentClass, componentClass.getName(), componentFactory);
    }

    public void release(Class componentClass, String key) {
        Map<String, Object> componentMap = mComponentGroups.get(componentClass);
        if (componentMap != null) {
            componentMap.remove(key);
        }
    }

    public void release(Class componentClass) {
        release(componentClass, componentClass.getName());
    }

    public static InjectRequest with(Context context) {
        return new InjectRequest(sInstance, context.getApplicationContext());
    }

    @VisibleForTesting
    static void clearComponents() {
        sInstance.mComponentGroups.clear();
    }

    @VisibleForTesting
    static Map<Class, Map<String, Object>> getComponentGroups() {
        return sInstance.mComponentGroups;
    }
}
