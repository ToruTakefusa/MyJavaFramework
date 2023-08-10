public class Bar {
    void showMessage() {
        Foo foo = (Foo) Context.getBean("foo");
        System.out.println(foo.getMessage());
    }
}
