package knight704.ufinjector;

import dagger.Component;
import dagger.Subcomponent;
import knight704.ufinjector.releasers.ComponentReleaser;

/**
 * This class represents client-code intention to create or reuse existing Dagger component with support of auto-release according to lifecycle.
 * TODO: At the moment this class supports auto-release only for Activity lifecycle. Binding to fragment lifecycle should be implemented as well.
 */
public class InjectRequest {
    private ComponentCache mComponentCache;
    private Class mComponentClass;
    private boolean mRetainOnConfigChange;
    private boolean mAllowComponentDuplicates;
    private String mDuplicateKey;

    public InjectRequest(ComponentCache componentCache, ComponentReleaser releaser) {
        mComponentCache = componentCache;
        bindToLifecycle(releaser);
    }

    /**
     * Keep track of component lifecycle according to releaser callback.
     * <p>
     * Note, when you retain component across config changes, keep in mind that component instance will be stored in singleton cache, so be careful
     * with items that you consider scope-singleton in that component, because they stay intact. It may produce undesired behavior
     * (i.e component that has module with activity link inside may lead to memory leak of this activity).
     */
    private void bindToLifecycle(final ComponentReleaser releaser) {
        releaser.onRegisterReleaser(new ComponentReleaser.OnReleaseListener() {
            @Override
            public void onRelease(boolean canRetain) {
                boolean shouldRelease = !(canRetain && mRetainOnConfigChange);
                if (shouldRelease) {
                    if (mAllowComponentDuplicates) {
                        mComponentCache.release(mComponentClass, mDuplicateKey);
                    } else {
                        mComponentCache.release(mComponentClass);
                    }
                }
                releaser.onUnregisterReleaser();
            }
        });
    }

    /**
     * Mark that component should be retained across config changes.
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
            return mComponentCache.getOrCreate(componentClass, mDuplicateKey, componentFactory);
        } else {
            return mComponentCache.getOrCreate(componentClass, componentFactory);
        }
    }

    private boolean isDaggerComponent(Class clazz) {
        return (clazz.getAnnotation(Component.class) != null)
                || (clazz.getAnnotation(Subcomponent.class) != null);
    }
}