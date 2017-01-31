package knight704.ufinjector;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import dagger.Component;
import dagger.Subcomponent;

/**
 * Created by Dmitrii_Ivashkin.
 * This class is responsible for creating dagger components via convenient builder-style and keeping them in map-cache.
 */
public class Injector {
    private static Injector sInstance = new Injector();

    /**
     * This map contains groups of dagger components by its type.
     * Reason for holding multiple components is the fact that there could be many activity/fragments that should have their own (unique) object graph.
     */
    private Map<Class, Map<String, Object>> mComponentGroups = new HashMap<>();

    private Injector() {
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCreate(Class<T> componentClass, String key, ComponentFactory<T> componentFactory) {
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

    private <T> T getOrCreate(Class<T> componentClass, ComponentFactory<T> componentFactory) {
        return getOrCreate(componentClass, componentClass.getName(), componentFactory);
    }

    private void release(Class componentClass, String key) {
        Map<String, Object> componentMap = mComponentGroups.get(componentClass);
        if (componentMap != null) {
            componentMap.remove(key);
        }
    }

    private void release(Class componentClass) {
        release(componentClass, componentClass.getName());
    }

    public static InjectRequest with(Context context) {
        return new InjectRequest(context.getApplicationContext());
    }

    public static class InjectRequest {
        private Application mApp;
        private Class mComponentClass;
        private boolean mReleaseOnConfigChange;
        private boolean mAllowComponentDuplicates;
        private String mDuplicateKey;

        private InjectRequest(Context context) {
            mApp = (Application) context;
        }

        /**
         * Mark that component should not be retained across config change.
         * Use in conjunction with {@link #bindToLifecycle(Activity)} or {@link #bindToLifecycle(Fragment)}
         */
        public InjectRequest releaseOnConfigChange(boolean releaseOnConfigChange) {
            mReleaseOnConfigChange = releaseOnConfigChange;
            return this;
        }

        /**
         * Mark that while injecting we should find existing component by specific key. This allow to have different component graphs instances of the same type.
         *
         * @param key to identify component.
         */
        public InjectRequest allowComponentDuplicates(String key) {
            mAllowComponentDuplicates = true;
            mDuplicateKey = key;
            return this;
        }

        /**
         * Call to this method will add opportunity to automatically keep track of component lifecycle according to activity lifecycle.
         * If you call {@link #releaseOnConfigChange(boolean)} during construction it will ensure that component wouldn't be retained across activity
         * recreation (even when it is just configuration change).
         * <p>
         * Note, when you retain component across config changes, keep in mind that component will be retrieved from cache, so be careful with items that you
         * consider scope-singleton in that component, because they stay intact. It may produce undesired behavior.
         * For example: component that has module with activity link inside may lead to leak of this activity.
         */
        public InjectRequest bindToLifecycle(final Activity bindedActivity) {
            mApp.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksAdapter() {
                @Override
                public void onActivityStopped(Activity activity) {
                    if (activity == bindedActivity) {
                        if (activity.isFinishing() || mReleaseOnConfigChange) {
                            if (mAllowComponentDuplicates) {
                                sInstance.release(mComponentClass, mDuplicateKey);
                            } else {
                                sInstance.release(mComponentClass);
                            }
                        }
                        mApp.unregisterActivityLifecycleCallbacks(this);
                    }
                }
            });
            return this;
        }

        /**
         * Same as {@link #bindToLifecycle(Activity)} but for fragment. For now it just delegate call to the mentioned method with fragment activity.
         * Seems legit, but should be revisited in a greater depth in the future.
         */
        public InjectRequest bindToLifecycle(Fragment fragment) {
            return bindToLifecycle(fragment.getActivity());
        }

        /**
         * Provide component class and related factory. Class should be annotated with {@link Component} or {@link Subcomponent}, thus be compatible with dagger2.
         * Factory here is used for creating component from scratch if it wasn't stored in cache before.
         *
         * @param componentClass   Component class.
         * @param componentFactory factory instance that would be used to create new component in case it is not present in cache.
         * @param <T>              component type.
         * @return component (cached or created via factory).
         */
        public <T> T build(Class<T> componentClass, ComponentFactory<T> componentFactory) {
            if (componentClass == null || componentFactory == null) {
                throw new IllegalArgumentException("Component class or factory is not provided, call #component before #build");
            }
            if (!isDaggerComponent(componentClass)) {
                throw new IllegalArgumentException(String.format("Class %s isn't a Dagger component/subcomponent", componentClass.getName()));
            }
            mComponentClass = componentClass;
            if (mAllowComponentDuplicates) {
                return sInstance.getOrCreate(componentClass, mDuplicateKey, componentFactory);
            } else {
                return sInstance.getOrCreate(componentClass, componentFactory);
            }
        }

        private boolean isDaggerComponent(Class clazz) {
            return (clazz.getAnnotation(Component.class) != null)
                    || (clazz.getAnnotation(Subcomponent.class) != null);
        }
    }

    public interface ComponentFactory<T> {
        T create();
    }
}
