package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q05ClassLoaderDelegationExample {
    private Q05ClassLoaderDelegationExample() {}

    public static void main(String[] args) {
        ClassLoader appLoader = Q05ClassLoaderDelegationExample.class.getClassLoader();
        String appLoaderName = appLoader.getName(); // "app" (Application ClassLoader)

        ClassLoader platformLoader = appLoader.getParent();
        String platformLoaderName = platformLoader.getName(); // "platform" (Platform ClassLoader)

        ClassLoader bootstrapLoader =
                String.class.getClassLoader(); // null (Bootstrap ClassLoader is native C++ runtime)
    }
}
