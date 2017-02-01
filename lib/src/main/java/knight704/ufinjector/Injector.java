package knight704.ufinjector;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

import dagger.Component;
import dagger.Subcomponent;

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
        private boolean mRetainOnConfigChange;
        private boolean mAllowComponentDuplicates;
        private String mDuplicateKey;

        private InjectRequest(Context context) {
            mApp = (Application) context;
        }

        /**
         * Mark that component should be retained across config changes.
         * Use in conjunction with {@link #bindToLifecycle(Activity)} or {@link #bindToLifecycle(Fragment)}
         */
        public InjectRequest retainOnConfigChange(boolean retainOnConfigChange) {
            mRetainOnConfigChange = retainOnConfigChange;
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
         * Call to this method will add listener to keep track of component lifecycle according to activity lifecycle.
         * <p>
         * Note, when you retain component across config changes, keep in mind that component instance will be stored in singleton cache, so be careful
         * with items that you consider scope-singleton in that component, because they stay intact. It may produce undesired behavior
         * (i.e component that has module with activity link inside may lead to memory leak of this activity).
         */
        public InjectRequest bindToLifecycle(final Activity bindedActivity) {
            mApp.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksAdapter() {
                @Override
                public void onActivityStopped(Activity activity) {
                    if (activity == bindedActivity) {
                        boolean shouldRelease = activity.isFinishing() || (activity.isChangingConfigurations() && !mRetainOnConfigChange);
                        if (shouldRelease) {
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
         * TODO: This should be revisited in a greater depth to cover complex fragment cases. Use with extreme caution if you know what you do.
         */
        public InjectRequest bindToLifecycle(Fragment fragment) {
            return bindToLifecycle(fragment.getActivity());
        }

        /**
         * Provide component class and related factory. Class should be annotated with {@link Component} or {@link Subcomponent}, thus be valid component
         * compatible with dagger2. Factory here is used for creating component from scratch if it wasn't stored in cache before.
         *
         * @param componentClass   component class.
         * @param componentFactory factory instance that would be used to create new component in case it is not present in cache.
         * @param <T>              component type.
         * @return component (existing one if cached otherwise will be created via provided factory).
         */
        public <T> T build(Class<T> componentClass, ComponentFactory<T> componentFactory) {
            if (componentClass == null || componentFactory == null) {
                throw new IllegalArgumentException("Component class or factory is not provided");
            }
            if (!isDaggerComponent(componentClass)) {
                throw new IllegalArgumentException(String.format("Class %s isn't a Dagger2 compatible component/subcomponent", componentClass.getName()));
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

    @VisibleForTesting
    static void clearComponents() {
        sInstance.mComponentGroups.clear();
    }

    @VisibleForTesting
    static Map<Class, Map<String, Object>> getComponentGroups() {
        return sInstance.mComponentGroups;
    }
}
