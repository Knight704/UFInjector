package knight704.ufinjector;

public interface ComponentCache {
    <T> T getOrCreate(Class<T> componentClass, String key, ComponentFactory<T> componentFactory);

    <T> T getOrCreate(Class<T> componentClass, ComponentFactory<T> componentFactory);

    void release(Class componentClass, String key);

    void release(Class componentClass);
}
