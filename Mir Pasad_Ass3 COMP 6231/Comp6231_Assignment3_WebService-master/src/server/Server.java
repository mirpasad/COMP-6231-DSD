package server;

public class Server {
    public static void main(String[] args) throws Exception {
        Runnable task1 = () -> {
            try {
                ServerInstance OutremontServer = new ServerInstance("OUT", args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Runnable task2 = () -> {
            try {
                ServerInstance VerdunServer = new ServerInstance("VER", args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Runnable task3 = () -> {
            try {
                ServerInstance AtwaterServer = new ServerInstance("ATW", args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(task1);
        thread1.start();
        Thread thread2 = new Thread(task2);
        thread2.start();
        Thread thread3 = new Thread(task3);
        thread3.start();
    }
}
