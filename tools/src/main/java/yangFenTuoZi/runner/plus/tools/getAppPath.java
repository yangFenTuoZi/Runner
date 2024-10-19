package yangFenTuoZi.runner.plus.tools;

public class getAppPath {
    public static void main(String[] args) {
        try {
            String appPath = Tools.getAppPath(args[0]);
            if (appPath == null || appPath.isEmpty()) {
                System.exit(1);
            } else {
                System.out.println(appPath);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
