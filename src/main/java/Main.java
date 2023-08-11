public class Main {
    public static void main(String[] args) {
        Context.autoRegister();

        Bar bar = (Bar) Context.getBean("bar");
        bar.showMessage();
    }
}
