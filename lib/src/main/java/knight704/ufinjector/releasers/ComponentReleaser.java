package knight704.ufinjector.releasers;

public interface ComponentReleaser {
    void onRegisterReleaser(OnReleaseListener listener);

    void onUnregisterReleaser();

    public interface OnReleaseListener {
        void onRelease(boolean canRetain);
    }
}
